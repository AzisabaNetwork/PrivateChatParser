package net.azisaba.privateChatParser.filter

import net.azisaba.privateChatParser.LogData
import net.azisaba.privateChatParser.util.StringReader

object FilterParser {
    fun parse(s: StringReader): FilterPredicate {
        s.skipBlanks()
        if (s.isEOF()) return FilterPredicate.TRUE
        var currentOp: FilterPredicate.Op? = null
        val currentPredicates = mutableListOf<(LogData) -> Boolean>()
        while (!s.isEOF()) {
            s.skipBlanks()
            currentPredicates.add(parseSingle(s))
            s.skipBlanks()
            if (s.isEOF()) break
            when (val read = s.read(2)) {
                "&&" -> {
                    if (currentOp == null || currentOp == FilterPredicate.Op.AND) {
                        currentOp = FilterPredicate.Op.AND
                    } else {
                        throw IllegalArgumentException("Cannot change operator to $read (Use parentheses)")
                    }
                }
                "||" -> {
                    if (currentOp == null || currentOp == FilterPredicate.Op.OR) {
                        currentOp = FilterPredicate.Op.OR
                    } else {
                        throw IllegalArgumentException("Cannot change operator to $read (Use parentheses)")
                    }
                }
                else -> {
                    throw IllegalArgumentException("Unexpected token: $read")
                }
            }
            s.skipBlanks()
            if (s.isEOF()) throw IllegalArgumentException("Unexpected EOF")
        }
        if (currentOp == null) currentOp = FilterPredicate.Op.AND
        return FilterPredicate(currentOp, currentPredicates)
    }

    private fun parseSingle(s: StringReader): (LogData) -> Boolean {
        if (s.isEOF()) return ({ true })
        if (s.peek() == '(') {
            s.skip()
            val group = parse(s)
            if (s.peek() == ')') {
                s.skip()
                return ({ group.test(it) })
            }
            throw IllegalArgumentException("Expected ')' but found '${s.peek()}'")
        }
        s.skipBlanks()
        val left = s.readToken()
        if (left !in listOf("sender", "recipient", "is_reply")) {
            throw IllegalArgumentException("Expected 'sender', 'recipient', or 'is_reply' but found '$left'")
        }
        s.skipBlanks()
        val op = when (val found = s.peek()) {
            '=' -> {
                s.skip()
                if (s.peek() == '=') s.skip()
                EqOp.EQUALS
            }
            '!' -> {
                s.skip()
                if (s.peek() == '=') s.skip()
                EqOp.NOT_EQUALS
            }
            else -> {
                throw IllegalArgumentException("Expected '=', '==' or '!=' but found '$found'")
            }
        }
        s.skipBlanks()
        val predicate: (LogData) -> Boolean = if (s.peek() == '$') {
            // variable
            s.skip()
            when (val variable = s.readToken()) {
                "sender" -> {
                    when (left) {
                        "sender" -> ({ true }) // sender == sender
                        "recipient" -> ({ data -> data.sender == data.recipient }) // recipient == sender
                        else -> throw IllegalArgumentException("'is_reply' can't be used with 'sender'") // is_reply == sender
                    }
                }
                "recipient" -> {
                    when (left) {
                        "sender" -> ({ data -> data.recipient == data.sender }) // recipient == sender
                        "recipient" -> ({ true }) // recipient == recipient
                        else -> throw IllegalArgumentException("'is_reply' can't be used with 'recipient'") // is_reply == recipient
                    }
                }
                "is_reply" -> {
                    when (left) {
                        "sender" -> throw IllegalArgumentException("'is_reply' can't be used with 'sender'") // is_reply == sender
                        "recipient" -> throw IllegalArgumentException("'is_reply' can't be used with 'recipient'") // is_reply == recipient
                        else -> ({ true }) // is_reply == is_reply
                    }
                }
                else -> throw IllegalArgumentException("Expected 'sender', 'recipient', or 'is_reply' but found '$variable'")
            }
        } else {
            // exact match
            when (left) {
                "sender" -> {
                    val value = s.readToken();
                    { data -> data.sender == value.lowercase() } // lowercase
                }
                "recipient" -> {
                    val value = s.readToken();
                    { data -> data.recipient == value.lowercase() } // lowercase
                }
                "is_reply" -> {
                    val value = s.readToken();
                    { data -> data.isReply == value.toBoolean() }
                }
                else -> throw IllegalArgumentException("Expected 'sender', 'recipient', or 'is_reply' but found '$left'")
            }
        }
        if (op == EqOp.NOT_EQUALS) {
            return { !predicate(it) }
        } else {
            return predicate
        }
    }
}

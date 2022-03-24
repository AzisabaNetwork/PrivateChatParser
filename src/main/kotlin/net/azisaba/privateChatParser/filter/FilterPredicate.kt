package net.azisaba.privateChatParser.filter

import net.azisaba.privateChatParser.LogData

data class FilterPredicate(val op: Op, val predicates: List<(logData: LogData) -> Boolean>) {
    companion object {
        val TRUE = FilterPredicate(Op.AND, emptyList())
        val FALSE = FilterPredicate(Op.AND, listOf { false })
    }

    enum class Op {
        OR,
        AND,
    }

    fun test(logData: LogData): Boolean {
        if (predicates.isEmpty()) {
            return true
        }
        return when (op) {
            Op.OR -> predicates.any { it(logData) }
            Op.AND -> predicates.all { it(logData) }
        }
    }
}

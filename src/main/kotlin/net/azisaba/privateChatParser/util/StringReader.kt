package net.azisaba.privateChatParser.util

class StringReader(private val text: String) {
    private var index = 0

    fun peek(): Char = try {
        text[index]
    } catch (e: IndexOutOfBoundsException) {
        throw IllegalArgumentException("Reached end of file", e)
    }

    fun read(): Char = read(1).toCharArray()[0]

    fun read(amount: Int): String {
        if (isEOF()) {
            throw IllegalArgumentException("Reached end of file (idx: $index, len: ${text.length})")
        }
        val string = text.substring(index, index + amount)
        index += amount
        return string
    }

    fun startsWith(prefix: String): Boolean = text.substring(index).startsWith(prefix)

    fun skip(amount: Int = 1): StringReader {
        index += amount
        return this
    }

    fun skipBlanks(): StringReader {
        while (!isEOF() && peek().isWhitespace()) {
            skip()
        }
        return this
    }

    fun readToken(): String {
        val builder = StringBuilder()
        while (!isEOF() && (peek() in 'a'..'z' || peek() in 'A'..'Z' || peek() in '0'..'9' || peek() == '_')) {
            builder.append(read())
        }
        return builder.toString()
    }

    fun isEOF(): Boolean = index >= text.length
}

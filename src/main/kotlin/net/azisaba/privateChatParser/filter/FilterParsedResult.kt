package net.azisaba.privateChatParser.filter

import net.azisaba.privateChatParser.LogData

data class FilterParsedResult(private val predicate: FilterPredicate) {
    fun shouldLog(logData: LogData): Boolean = predicate.test(logData)
}

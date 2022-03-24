@file:JvmName("Main")
package net.azisaba.privateChatParser

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.vararg
import net.azisaba.privateChatParser.argType.FileArgType
import net.azisaba.privateChatParser.filter.FilterParsedResult
import net.azisaba.privateChatParser.filter.FilterParser
import net.azisaba.privateChatParser.util.RomajiTextReader
import net.azisaba.privateChatParser.util.StringReader
import org.json.JSONArray
import java.io.File
import java.util.zip.GZIPInputStream

fun main(args: Array<String>) {
    val parser = ArgParser("PrivateChatParser", prefixStyle = ArgParser.OptionPrefixStyle.GNU)
    val input by parser.argument(FileArgType, "input", "Input file").vararg()
    val outputText by parser.option(FileArgType, "output-text", "t", "Output text (easily readable by human) file").default(File("output.txt"))
    val outputJson by parser.option(FileArgType, "output-json", "j", "Output JSON file").default(File("output.json"))
    val unparsedFilter by parser.option(ArgType.String, "filter", "f", "Apply filter").default("")
    val append by parser.option(ArgType.Boolean, "append", "a", "Append to output file instead of overwriting").default(false)
    parser.parse(args)
    val filter = FilterParsedResult(FilterParser.parse(StringReader(unparsedFilter)))
    val recipients = mutableMapOf<String, String>()
    val jsonArray = if (append) JSONArray(outputJson.readText()) else JSONArray()
    if (!append) outputText.writeText("")
    input.forEach {
        if (!it.exists()) {
            println("File ${it.path} does not exist.")
            return@forEach
        }
        if (it.extension != "txt" && it.extension != "log" && !it.name.endsWith(".log.gz")) {
            println("${it.name} is not a text or gzipped file.")
            return@forEach
        }
        println("Parsing ${it.name}")
        if (it.extension == "gz") {
            GZIPInputStream(it.inputStream()).use { stream ->
                stream.reader().use { reader ->
                    reader.forEachLine { line -> processLine(filter, line, outputText, recipients, jsonArray) }
                }
            }
        } else {
            it.forEachLine { line -> processLine(filter, line, outputText, recipients, jsonArray) }
        }
    }
    outputJson.writeText(jsonArray.toString(2))
}

fun processLine(filter: FilterParsedResult, line: String, outText: File, recipients: MutableMap<String, String>, jsonArray: JSONArray) {
    if (!line.contains(" issued server command: ")) return
    val time = line.replace("\\[(.+?)] \\[Server thread/INFO]: .+? issued server command: /.*".toRegex(), "$1").trim().lowercase()
    val sender = line.replace(".*\\[Server thread/INFO]: (.+?) issued server command: /.*".toRegex(), "$1").trim().lowercase()
    val command = line.replace(".*\\[Server thread/INFO]: .+? issued server command: /(.*)".toRegex(), "$1").trim()
    val isTell = command.matches("(?i)^(lunachat:|minecraft:)?(tell|message|msg|m|t|w) .+?".toRegex())
    val isReply = command.matches("(?i)^(lunachat:)?(reply|r) .+?".toRegex())
    val commandArguments = command.split(' ').filterIndexed { index, _ -> index > 0 }
    var recipient: String? = null
    var message: String? = null
    if (isTell) {
        if (commandArguments.isNotEmpty()) {
            recipients[sender] = commandArguments[0].lowercase()
            recipients[commandArguments[0].lowercase()] = sender
            recipient = commandArguments[0].lowercase()
        }
        if (commandArguments.size > 1) {
            message = commandArguments.drop(1).joinToString(" ")
        }
    }
    if (isReply) {
        if (commandArguments.isNotEmpty()) {
            recipient = recipients[sender] ?: return
            recipients[recipient] = sender
            message = commandArguments.joinToString(" ")
        }
    }
    recipient ?: return
    message ?: return
    if (!filter.shouldLog(LogData(sender, recipient, isReply))) return
    println("[$time] [$sender -> $recipient]: ${RomajiTextReader.parse(message)} ($message)")
    outText.appendText("[$time] [$sender -> $recipient]: ${RomajiTextReader.parse(message)} ($message)\n")
    jsonArray.put(
        mapOf(
            "time" to time,
            "sender" to sender,
            "recipient" to recipient,
            "message" to message,
            "converted_message" to RomajiTextReader.parse(message),
            "is_reply" to isReply,
            "raw_command" to command,
        )
    )
}

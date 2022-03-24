@file:JvmName("Main")
package net.azisaba.privateChatParser

import net.azisaba.privateChatParser.util.RomajiTextReader
import org.json.JSONArray
import java.io.File
import java.util.zip.GZIPInputStream

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: java -jar PrivateChatParser.jar <input.(txt|log|log.gz)>")
        return
    }
    val outText = File("output.txt")
    val recipients = mutableMapOf<String, String>()
    val jsonArray = JSONArray()
    outText.writeText("")
    args.map { File(it) }.forEach {
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
                    reader.forEachLine { line -> processLine(line, outText, recipients, jsonArray) }
                }
            }
        } else {
            it.forEachLine { line -> processLine(line, outText, recipients, jsonArray) }
        }
    }
    File("output.json").writeText(jsonArray.toString(2))
}

fun processLine(line: String, outText: File, recipients: MutableMap<String, String>, jsonArray: JSONArray) {
    if (!line.contains(" issued server command: ")) return
    val time = line.replace("\\[(.+?)] \\[Server thread/INFO]: .+? issued server command: /.*".toRegex(), "$1").trim().lowercase()
    val sender = line.replace(".*\\[Server thread/INFO]: (.+?) issued server command: /.*".toRegex(), "$1").trim().lowercase()
    val command = line.replace(".*\\[Server thread/INFO]: .+? issued server command: /(.*)".toRegex(), "$1").trim()
    val isTell = command.matches("^(lunachat:|minecraft:)?(tell|message|msg|m|t|w) .+?".toRegex())
    val isReply = command.matches("^(lunachat:)?(reply|r) .+?".toRegex())
    val commandArguments = command.split(' ').filterIndexed { index, _ -> index > 0 }
    var recipient: String? = null
    var message: String? = null
    if (isTell) {
        if (commandArguments.isNotEmpty()) {
            recipients[sender] = commandArguments[0].lowercase()
            recipient = commandArguments[0].lowercase()
        }
        if (commandArguments.size > 1) {
            message = commandArguments.drop(1).joinToString(" ")
        }
    }
    if (isReply) {
        if (commandArguments.isNotEmpty()) {
            recipient = recipients[sender]
            message = commandArguments.joinToString(" ")
        }
    }
    recipient ?: return
    message ?: return
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

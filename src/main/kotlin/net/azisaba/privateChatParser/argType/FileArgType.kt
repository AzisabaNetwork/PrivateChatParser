package net.azisaba.privateChatParser.argType

import kotlinx.cli.ArgType
import java.io.File

object FileArgType: ArgType<File>(true) {
    override val description: kotlin.String
        get() = "{ File }"

    override fun convert(value: kotlin.String, name: kotlin.String): File =
        File(value).apply { if (!exists()) error("File $value does not exist for option $name") }
}

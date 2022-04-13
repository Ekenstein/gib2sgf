package com.github.ekenstein.gib2sgf

import com.github.ekenstein.gib2sgf.gib.Gib
import com.github.ekenstein.gib2sgf.gib.parser.from
import com.github.ekenstein.sgf.Sgf
import com.github.ekenstein.sgf.encode
import com.github.ekenstein.sgf.encodeToString
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import java.nio.file.Path

private val sgf: Sgf = Sgf { }

fun main(args: Array<String>) {
    val parser = ArgParser("gib2sgf")

    val input by parser.option(
        type = ArgType.String,
        shortName = "f",
        fullName = "file",
        description = "The GIB file to convert to SGF"
    ).required()

    val output = parser.option(
        type = ArgType.String,
        shortName = "o",
        fullName = "output",
        description = "The target SGF file"
    )

    parser.parse(args)

    val gib = Gib.from(Path.of(input))
    val sgfGameTree = gib.toSgf()

    val outputPath = output.value?.let { Path.of(it) }

    if (outputPath != null) {
        val outputFile = outputPath.toFile()
        outputFile.createNewFile()

        outputFile.outputStream().use {
            sgf.encode(it, sgfGameTree)
        }
    } else {
        println(sgf.encodeToString(sgfGameTree))
    }
}

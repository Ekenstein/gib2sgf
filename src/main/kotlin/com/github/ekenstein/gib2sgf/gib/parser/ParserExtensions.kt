package com.github.ekenstein.gib2sgf.gib.parser

import com.github.ekenstein.gib2sgf.gib.Gib
import com.github.ekenstein.gib2sgf.gib.GibColor
import com.github.ekenstein.gib2sgf.gib.GibGame
import com.github.ekenstein.gib2sgf.gib.GibGameProperty
import com.github.ekenstein.gib2sgf.gib.GibHeader
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import java.io.InputStream
import java.nio.file.Path

private val gibErrorListener = object : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        throw GibParseException(msg, Marker(line, charPositionInLine, line, charPositionInLine))
    }
}

fun Gib.Companion.from(string: String) = from(CharStreams.fromString(string))
fun Gib.Companion.from(path: Path) = from(CharStreams.fromPath(path))
fun Gib.Companion.from(inputStream: InputStream) = from(CharStreams.fromStream(inputStream))

private fun Gib.Companion.from(charStream: CharStream): Gib {
    val lexer = GibLexer(charStream)
    lexer.removeErrorListeners()
    lexer.addErrorListener(gibErrorListener)
    val tokenStream = CommonTokenStream(lexer)
    val parser = GibParser(tokenStream)
    parser.removeErrorListeners()
    parser.addErrorListener(gibErrorListener)

    return extractGib(parser)
}

private fun extractGib(parser: GibParser): Gib {
    val header = extractHeader(parser.header())
    val game = extractGame(parser.game())
    return Gib(header, game)
}

private fun extractGame(ctx: GibParser.GameContext): GibGame {
    val properties = ctx.game_property().mapNotNull { extractGameProperty(it) }
    return GibGame(properties)
}

private fun extractGameProperty(ctx: GibParser.Game_propertyContext): GibGameProperty? {
    return when (ctx) {
        is GibParser.MoveContext -> GibGameProperty.STO(
            ctx.moveNumber.toInt(),
            ctx.player.toColor(),
            ctx.x.toInt() to ctx.y.toInt()
        )
        is GibParser.IniContext -> GibGameProperty.INI(
            ctx.handicap.toInt()
        )
        is GibParser.PassContext -> GibGameProperty.SKI(
            ctx.moveNumber.toInt()
        )
        else -> null
    }
}

private fun extractHeader(ctx: GibParser.HeaderContext): GibHeader {
    val properties = ctx.header_property().associate { extractHeaderProperty(it) }
    return GibHeader(properties)
}

private fun extractHeaderProperty(ctx: GibParser.Header_propertyContext): Pair<String, String> {
    val identifier = ctx.property_identifier().text
    val value = ctx.VALUE()?.text

    val strippedValue = value?.substring(1, value.length - 2) ?: ""

    return identifier to strippedValue
}

private fun Token.toInt(): Int = text.toIntOrNull()
    ?: throw GibParseException("Expected an integer, but got $text", toMarker())

private fun Token.toColor(): GibColor {
    return when (toInt()) {
        1 -> GibColor.Black
        2 -> GibColor.White
        else -> throw GibParseException("Expected either '1' or '2' but got $text", toMarker())
    }
}

private fun Token.toMarker(): Marker {
    val startColumn = charPositionInLine + 1

    return Marker(
        startLineNumber = line,
        startColumn = startColumn,
        endLineNumber = line,
        endColumn = startColumn + text.length
    )
}

package com.github.ekenstein.gib2sgf.gib

private const val HEADER_GAME_INFO = "GAMEINFOMAIN"
private const val HEADER_GAME_PLACE = "GAMEPLACE"
private const val HEADER_KOMI = "GAMEGONGJE"
private const val HEADER_GAME_SCORE = "GAMEZIPSU"
private const val HEADER_BLACK_PLAYER = "GAMEBLACKNAME"
private const val HEADER_WHITE_PLAYER = "GAMEWHITENAME"
private const val GAME_INFO_GAME_RESULT = "GRLT"
private const val GAME_INFO_TIME_INFO = "GTIME"

enum class GibColor { Black, White }

data class Gib(val header: GibHeader, val game: GibGame) {
    private val gameInfo by lazy {
        val gameInfoProperties = header.properties[HEADER_GAME_INFO]?.split(",").orEmpty()

        gameInfoProperties.associate {
            val (name, value) = it.split(":")
            name to value
        }
    }

    val handicap by lazy {
        game.properties.filterIsInstance<GibGameProperty.INI>().map { it.handicap }.singleOrNull() ?: 0
    }
    val komi by lazy { header.properties[HEADER_KOMI]?.toIntOrNull()?.let { it / 10.0 } }
    val gamePlace by lazy { header.properties[HEADER_GAME_PLACE] }
    val playerBlack by lazy { header.properties[HEADER_BLACK_PLAYER] }
    val playerWhite by lazy { header.properties[HEADER_WHITE_PLAYER] }

    private val gameScore by lazy { header.properties[HEADER_GAME_SCORE]?.toIntOrNull()?.let { it / 10.0 } }
    val gameResult by lazy {
        when (gameInfo[GAME_INFO_GAME_RESULT]?.toIntOrNull()) {
            0 -> "B+$gameScore"
            1 -> "W+$gameScore"
            3 -> "B+R"
            4 -> "W+R"
            7 -> "B+T"
            8 -> "W+T"
            else -> null
        }
    }

    val startColor by lazy { if (handicap >= 2) GibColor.White else GibColor.Black }

    private val timeInfo by lazy { gameInfo[GAME_INFO_TIME_INFO]?.split("-") }
    val timeLimit by lazy { timeInfo?.let { (timeLimit, _, _) -> timeLimit.toIntOrNull()?.toDouble() } }

    val overtime by lazy { timeInfo?.let { (_, seconds, stones) -> "${stones}x$seconds byo-yomi" } }

    companion object
}

sealed class GibGameProperty {
    data class STO(val moveNumber: Int, val color: GibColor, val point: Pair<Int, Int>) : GibGameProperty()
    data class INI(val handicap: Int) : GibGameProperty()
    data class SKI(val moveNumber: Int) : GibGameProperty()
}

@JvmInline
value class GibHeader(val properties: Map<String, String>)

@JvmInline
value class GibGame(val properties: List<GibGameProperty>)

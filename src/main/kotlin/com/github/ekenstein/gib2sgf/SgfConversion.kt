package com.github.ekenstein.gib2sgf

import com.github.ekenstein.gibson.GameResult
import com.github.ekenstein.gibson.Gib
import com.github.ekenstein.gibson.GibColor
import com.github.ekenstein.gibson.Move
import com.github.ekenstein.gibson.TimeSettings
import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.addProperty

private fun GibColor.toSgf() = when (this) {
    GibColor.Black -> SgfColor.Black
    GibColor.White -> SgfColor.White
}

private fun handicapPoints(stones: Int): Set<SgfPoint> = when (stones) {
    2 -> setOf(SgfPoint(4, 16), SgfPoint(16, 4))
    3 -> handicapPoints(2) + setOf(SgfPoint(16, 16))
    4 -> handicapPoints(3) + setOf(SgfPoint(4, 4))
    5 -> handicapPoints(4) + setOf(SgfPoint(10, 10))
    6 -> handicapPoints(4) + setOf(SgfPoint(4, 10), SgfPoint(16, 10))
    7 -> handicapPoints(6) + setOf(SgfPoint(10, 10))
    8 -> handicapPoints(6) + setOf(SgfPoint(10, 4), SgfPoint(10, 16))
    9 -> handicapPoints(8) + setOf(SgfPoint(10, 10))
    else -> emptySet()
}

private fun GameResult.toSgf() = when (this) {
    is GameResult.Score -> com.github.ekenstein.sgf.GameResult.Score(winner.toSgf(), score)
    is GameResult.Resignation -> com.github.ekenstein.sgf.GameResult.Resignation(winner.toSgf())
    is GameResult.Time -> com.github.ekenstein.sgf.GameResult.Time(winner.toSgf())
}

private fun Move.toSgf() = when (this) {
    is Move.Point -> com.github.ekenstein.sgf.Move.Stone(SgfPoint(x, y))
    is Move.Pass -> com.github.ekenstein.sgf.Move.Pass
}

private fun TimeSettings.toSgf() = listOf(
    SgfProperty.GameInfo.OT("${overtimePeriods}x$overtimeSeconds"),
    SgfProperty.GameInfo.TM(timeLimit.toDouble())
)

private val rootProperties
    get() = listOf(
        SgfProperty.Root.SZ(19),
        SgfProperty.Root.GM(GameType.Go),
        SgfProperty.Root.AP("gib2sgf", "0.1.0"),
        SgfProperty.Root.CA(Charsets.UTF_8),
        SgfProperty.Root.FF(4)
    )

private val Gib.handicapProperties: List<SgfProperty>
    get() = handicapPoints(handicap).takeIf { it.isNotEmpty() }?.let {
        listOf(
            SgfProperty.GameInfo.HA(handicap),
            SgfProperty.Setup.AB(it)
        )
    }.orEmpty()

private val Gib.timeSettingsProperties: List<SgfProperty>
    get() = timeSettings?.toSgf() ?: emptyList()

private val Gib.gameInfoProperties
    get() = listOfNotNull(
        gamePlace?.let { SgfProperty.GameInfo.PC(it) },
        playerBlack?.let { SgfProperty.GameInfo.PB(it) },
        playerWhite?.let { SgfProperty.GameInfo.PW(it) },
        komi?.let { SgfProperty.GameInfo.KM(it) },
        gameResult?.let { SgfProperty.GameInfo.RE(it.toSgf()) }
    )

private val Gib.moveProperties
    get() = moves.flatMap {
        val moveProperty = when (it.color) {
            GibColor.Black -> SgfProperty.Move.B(it.toSgf())
            GibColor.White -> SgfProperty.Move.W(it.toSgf())
        }

        listOf(moveProperty, SgfProperty.Move.MN(it.moveNumber))
    }

private val Gib.sgfProperties
    get() = rootProperties + gameInfoProperties + handicapProperties + timeSettingsProperties + moveProperties

fun Gib.toSgf() = sgfProperties.fold(SgfGameTree.empty) { tree, property -> tree.addProperty(property) }

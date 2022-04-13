package com.github.ekenstein.gib2sgf

import com.github.ekenstein.gib2sgf.gib.Gib
import com.github.ekenstein.gib2sgf.gib.GibColor
import com.github.ekenstein.gib2sgf.gib.GibGameProperty
import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.addProperty

private val rootProperties = listOf(
    SgfProperty.Root.FF(4),
    SgfProperty.Root.GM(GameType.Go),
    SgfProperty.Root.SZ(19),
    SgfProperty.Root.CA("UTF-8"),
    SgfProperty.Root.AP("gib2sgf", "0.1.0")
)

private fun Gib.gameInfoProperties() = listOfNotNull(
    gamePlace?.let { SgfProperty.GameInfo.PC(it) },
    playerBlack?.let { SgfProperty.GameInfo.PB(it) },
    playerWhite?.let { SgfProperty.GameInfo.PW(it) },
    komi?.let { SgfProperty.GameInfo.KM(it) },
    gameResult?.let { SgfProperty.GameInfo.RE(it) },
    timeLimit?.let { SgfProperty.GameInfo.TM(it) },
    overtime?.let { SgfProperty.GameInfo.OT(it) },
    handicap.takeIf { it > 0 }?.let { SgfProperty.GameInfo.HA(it) }
)

private fun GibColor.flip() = when (this) {
    GibColor.Black -> GibColor.White
    GibColor.White -> GibColor.Black
}

private fun Gib.colorByMoveNumber(moveNumber: Int) = when (moveNumber % 2) {
    0 -> startColor
    else -> startColor.flip()
}

private fun Gib.gameMoveProperties() = game.properties.flatMap { property ->
    when (property) {
        is GibGameProperty.INI -> emptyList()
        is GibGameProperty.SKI -> {
            val move = when (colorByMoveNumber(property.moveNumber)) {
                GibColor.Black -> SgfProperty.Move.B.pass()
                GibColor.White -> SgfProperty.Move.W.pass()
            }

            listOf(SgfProperty.Move.MN(property.moveNumber - 1), move)
        }
        is GibGameProperty.STO -> {
            val (x, y) = property.point
            val move = when (property.color) {
                GibColor.Black -> SgfProperty.Move.B(x, y)
                GibColor.White -> SgfProperty.Move.W(x, y)
            }

            listOf(SgfProperty.Move.MN(property.moveNumber - 1), move)
        }
    }
}

fun Gib.toSgf(): SgfGameTree {
    val properties = rootProperties + gameInfoProperties() + gameMoveProperties()
    return properties.fold(SgfGameTree.empty) { tree, property -> tree.addProperty(property) }
}

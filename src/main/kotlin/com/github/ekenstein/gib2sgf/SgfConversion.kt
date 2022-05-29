package com.github.ekenstein.gib2sgf

import com.github.ekenstein.gibson.GameResult
import com.github.ekenstein.gibson.Gib
import com.github.ekenstein.gibson.GibColor
import com.github.ekenstein.gibson.Move
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.editor.SgfEditor
import com.github.ekenstein.sgf.editor.commit
import com.github.ekenstein.sgf.editor.pass
import com.github.ekenstein.sgf.editor.placeStone

private fun GibColor.toSgf() = when (this) {
    GibColor.Black -> SgfColor.Black
    GibColor.White -> SgfColor.White
}

private fun GameResult.toSgf() = when (this) {
    is GameResult.Score -> com.github.ekenstein.sgf.GameResult.Score(winner.toSgf(), score)
    is GameResult.Resignation -> com.github.ekenstein.sgf.GameResult.Resignation(winner.toSgf())
    is GameResult.Time -> com.github.ekenstein.sgf.GameResult.Time(winner.toSgf())
}

fun Gib.toSgf(): SgfGameTree {
    val editor = SgfEditor {
        rules {
            boardSize = 19
            komi = this@toSgf.komi ?: 0.0
            handicap = this@toSgf.handicap
        }

        whitePlayer {
            name = playerBlack
        }

        blackPlayer {
            name = playerBlack
        }

        result = gameResult?.toSgf()
        gamePlace = this@toSgf.gamePlace
    }

    return moves.fold(editor, ::placeStone).commit()
}

private fun placeStone(editor: SgfEditor, move: Move): SgfEditor {
    val color = move.color.toSgf()
    return when (move) {
        is Move.Point -> editor.placeStone(color, move.x + 1, move.y + 1)
        is Move.Pass -> editor.pass(color)
    }
}

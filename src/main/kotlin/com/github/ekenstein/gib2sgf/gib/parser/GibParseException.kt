package com.github.ekenstein.gib2sgf.gib.parser

data class Marker(
    val startLineNumber: Int,
    val startColumn: Int,
    val endLineNumber: Int,
    val endColumn: Int
)

class GibParseException(override val message: String?, val marker: Marker) : Exception()

package io.rstlne.aether

/**
 * Created by pdv on 12/15/17.
 */

val C2 = 24

val NOTE_NAMES = listOf("C", "C# / Db", "D", "D# / Eb", "E", "F", "F# / Gb", "G", "G# / Ab", "A", "A# / Bb", "B")

val SCALES_BY_NAME = listOf(
    "Major" to Scale.MAJOR,
    "Minor" to Scale.MINOR,
    "Blues" to Scale.BLUES,
    "Chromatic" to Scale.CHROMATIC
)

object Scale {
    val MAJOR = listOf(2, 2, 1, 2, 2, 2, 1)
    val MINOR = listOf(2, 1, 2, 2, 1, 2, 2)
    val BLUES = listOf(3, 2, 1, 1, 3, 2)
    val CHROMATIC = (0..11).map { 1 }
}
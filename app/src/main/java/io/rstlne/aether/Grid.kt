package io.rstlne.aether

/**
 * Created by pdv on 10/27/17.
 */

// 8x8 grid

// 56..63
// ...
// 8..15
// 0..7



sealed class GridLayout {
    object Drums
    sealed class Notes()

}

val MAJOR = listOf(2, 2, 1, 2, 2, 2, 1)
val MINOR = listOf(2, 1, 2, 2, 1, 2, 2)

val c = 60

fun <T> List<T>.reductions(): Unit = Unit

fun scale(root: Int, intervals: List<Int>): List<Int> {
    return listOf()
}


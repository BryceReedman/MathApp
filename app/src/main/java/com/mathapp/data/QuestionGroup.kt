package com.mathapp.data

import kotlin.math.sqrt

data class QuestionGroup(
    val id: Int,        // 1, 2, 3, ...
    val label: String,  // "1–10", "11–20", ...
    val minVal: Int,
    val maxVal: Int,
    val operation: MathOperation
)

object QuestionGroups {
    /** 10 groups covering 1–100 for all operations */
    fun forOperation(op: MathOperation): List<QuestionGroup> =
        (1..10).map { id ->
            val min = (id - 1) * 10 + 1
            val max = id * 10
            QuestionGroup(id, "$min\u2013$max", min, max, op)
        }

    fun defaultSelected(op: MathOperation): List<Int> = listOf(1)

    fun groupRange(groupId: Int): IntRange = ((groupId - 1) * 10 + 1)..(groupId * 10)

    /** Combined, sorted, deduplicated factor list from the given groups, minus any excluded numbers. */
    fun buildFactorList(groupIds: List<Int>, excludedNumbers: Set<Int> = emptySet()): List<Int> =
        groupIds.flatMap { groupRange(it) }.distinct().sorted().filter { it !in excludedNumbers }

    val STANDARD_PERCENTAGES = listOf(10, 20, 25, 50, 75)

    fun hasProperFactorPair(n: Int): Boolean =
        (2..sqrt(n.toDouble()).toInt()).any { n % it == 0 }

    fun totalQuestionCount(op: MathOperation, factors: List<Int>): Int = when (op) {
        MathOperation.MULTIPLICATION -> factors.size * factors.size
        MathOperation.DIVISION       -> factors.size * factors.size
        MathOperation.ADDITION       -> factors.size * 12
        MathOperation.SUBTRACTION    -> factors.size * 12
        MathOperation.POWERS         -> factors.size * 3
        MathOperation.SQUARE_ROOTS   -> factors.size
        MathOperation.CUBE_ROOTS     -> factors.size
        MathOperation.FRACTIONS      -> factors.filter { it > 1 }.sumOf { it - 1 }
        MathOperation.PERCENTAGES    -> factors.size * STANDARD_PERCENTAGES.size
        MathOperation.FACTORIZATION  -> factors.count { n -> n > 1 && hasProperFactorPair(n) }
        MathOperation.MIXED          -> factors.size * 10
    }
}

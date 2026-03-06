package com.mathapp.data

data class QuestionGroup(
    val id: Int,
    val label: String,
    val operation: MathOperation
)

object QuestionGroups {
    fun forOperation(op: MathOperation): List<QuestionGroup> = when (op) {
        MathOperation.MULTIPLICATION -> (1..12).map { QuestionGroup(it, "×$it", op) }
        MathOperation.DIVISION       -> (1..12).map { QuestionGroup(it, "÷$it", op) }
        MathOperation.POWERS         -> (2..12).map { QuestionGroup(it, "$it^", op) }
        MathOperation.FRACTIONS      -> listOf(2, 3, 4, 5, 6, 8, 10).map { QuestionGroup(it, "/$it", op) }
    }

    fun defaultSelected(op: MathOperation): List<Int> = when (op) {
        MathOperation.MULTIPLICATION -> listOf(2, 3, 4, 5)
        MathOperation.DIVISION       -> listOf(2, 3, 4, 5)
        MathOperation.POWERS         -> listOf(2, 3, 4)
        MathOperation.FRACTIONS      -> listOf(2, 4, 5)
    }

    /** Total unique questions possible across the given group IDs for this operation. */
    fun totalQuestionCount(op: MathOperation, groupIds: List<Int>): Int = groupIds.sumOf { id ->
        when (op) {
            MathOperation.MULTIPLICATION -> 12        // id × 1..12
            MathOperation.DIVISION       -> 12        // (id×1..id×12) ÷ id
            MathOperation.POWERS         -> 4         // id^2..id^5
            MathOperation.FRACTIONS      -> id - 1    // 1/id...(id-1)/id
        }
    }
}

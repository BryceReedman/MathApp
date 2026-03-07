package com.mathapp.data

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

    fun totalQuestionCount(op: MathOperation, groupIds: List<Int>): Int =
        groupIds.sumOf { id -> questionCountForGroup(op, id) }

    private fun questionCountForGroup(op: MathOperation, groupId: Int): Int {
        val range = groupRange(groupId)
        return when (op) {
            MathOperation.MULTIPLICATION -> range.count() * 12
            MathOperation.DIVISION       -> range.count() * 12
            MathOperation.POWERS         -> range.count() * 3   // exp 2..4
            MathOperation.FRACTIONS      -> range.filter { it > 1 }.sumOf { it - 1 }
        }
    }
}

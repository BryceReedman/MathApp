package com.mathapp.data

data class SessionRecord(
    val timestampMs: Long,
    val operation: MathOperation,
    val selectedGroups: List<Int>,
    val strugglingOnly: Boolean,
    val correct: Int,
    val total: Int,
    val avgTimeMs: Long
) {
    val accuracy: Float get() = if (total == 0) 0f else correct.toFloat() / total.toFloat() * 100f
}

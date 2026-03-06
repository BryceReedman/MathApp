package com.mathapp.data

data class QuestionStats(
    val questionKey: String,
    val operation: MathOperation,
    val recentAttempts: List<Attempt>  // capped at last 5
) {
    val hasEnoughData: Boolean get() = recentAttempts.size >= 3

    val recentErrorRate: Float get() {
        if (recentAttempts.isEmpty()) return 0f
        return recentAttempts.count { !it.isCorrect }.toFloat() / recentAttempts.size
    }

    val recentAvgTimeMs: Long get() {
        if (recentAttempts.isEmpty()) return 0L
        return recentAttempts.sumOf { it.timeMs } / recentAttempts.size
    }

    val isStruggling: Boolean get() =
        hasEnoughData && (recentErrorRate > 0.4f || recentAvgTimeMs > 8_000L)

    val displayQuestionText: String get() = questionKey.substringAfter("_")
}

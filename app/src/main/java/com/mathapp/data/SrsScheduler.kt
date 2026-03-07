package com.mathapp.data

object SrsScheduler {

    /** Compute the next review interval in days. */
    fun nextIntervalDays(wasCorrect: Boolean, isFast: Boolean, prevIntervalDays: Int): Int =
        when {
            !wasCorrect                         -> 1
            prevIntervalDays == 0 && isFast     -> 2   // first correct + fast: 2 days
            prevIntervalDays == 0               -> 1   // first correct + slow: tomorrow
            isFast                              -> (prevIntervalDays * 2).coerceAtMost(30)
            else -> (prevIntervalDays * 1.5f).toInt().coerceAtLeast(prevIntervalDays + 1)
        }

    /** Fast = answered in under 2 seconds. */
    fun isFast(timeMs: Long): Boolean = timeMs < 2_000L
}

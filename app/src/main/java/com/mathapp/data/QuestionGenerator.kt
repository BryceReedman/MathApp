package com.mathapp.data

import kotlin.math.pow

object QuestionGenerator {

    /**
     * Builds a shuffled list of [count] questions from the given factor list.
     * If [strugglingKeys] is provided, only questions whose key is in that set are included.
     * Cycles through the pool with re-shuffles when count > pool size.
     */
    fun buildShuffledPool(
        operation: MathOperation,
        factors: List<Int>,
        strugglingKeys: Set<String>?,
        count: Int
    ): List<Question> {
        val pool = allQuestions(operation, factors)
            .filter { q -> strugglingKeys == null || q.questionKey in strugglingKeys }

        if (pool.isEmpty()) return emptyList()

        val result = mutableListOf<Question>()
        val shuffled = pool.shuffled().toMutableList()
        while (result.size < count) {
            result.addAll(shuffled.take(count - result.size))
            if (result.size < count) shuffled.shuffle()
        }
        return result.take(count)
    }

    /** All unique questions producible from the given factor list for the operation. */
    fun allQuestions(operation: MathOperation, factors: List<Int>): List<Question> =
        when (operation) {
            MathOperation.MULTIPLICATION -> factors.flatMap { a ->
                factors.map { b ->
                    Question("$a \u00d7 $b", (a * b).toDouble(), operation, 0)
                }
            }
            MathOperation.DIVISION -> factors.flatMap { divisor ->
                factors.map { quotient ->
                    Question(
                        "${divisor * quotient} \u00f7 $divisor",
                        quotient.toDouble(),
                        operation,
                        0
                    )
                }
            }
            MathOperation.POWERS -> factors.flatMap { base ->
                (2..4).map { exp ->
                    Question("$base^$exp", base.toDouble().pow(exp.toDouble()), operation, 0)
                }
            }
            MathOperation.FRACTIONS -> factors.filter { it > 1 }.flatMap { denom ->
                (1 until denom).map { num ->
                    Question("$num/$denom", num.toDouble() / denom.toDouble(), operation, 0)
                }
            }
        }
}

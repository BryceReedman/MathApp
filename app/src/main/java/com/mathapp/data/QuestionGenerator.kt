package com.mathapp.data

import kotlin.math.pow

object QuestionGenerator {

    /**
     * Builds a shuffled list of [count] questions from the given groups,
     * filtering out excluded numbers and (if provided) limiting to struggling keys.
     * Cycles through the pool with re-shuffles if count > pool size.
     */
    fun buildShuffledPool(
        operation: MathOperation,
        groupIds: List<Int>,
        excludedNumbers: Set<Int>,
        strugglingKeys: Set<String>?,
        count: Int
    ): List<Question> {
        val pool = groupIds
            .flatMap { allQuestionsForGroup(operation, it) }
            .distinctBy { it.questionKey }
            .filter { q -> !isExcludedQuestion(q, excludedNumbers) }
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

    fun allQuestionsForGroup(operation: MathOperation, groupId: Int): List<Question> {
        val range = QuestionGroups.groupRange(groupId)
        return when (operation) {
            MathOperation.MULTIPLICATION -> range.flatMap { a ->
                (1..12).map { b ->
                    Question("$a \u00d7 $b", (a * b).toDouble(), operation, groupId)
                }
            }
            MathOperation.DIVISION -> range.flatMap { divisor ->
                (1..12).map { quotient ->
                    Question(
                        "${divisor * quotient} \u00f7 $divisor",
                        quotient.toDouble(),
                        operation,
                        groupId
                    )
                }
            }
            MathOperation.POWERS -> range.flatMap { base ->
                (2..4).map { exp ->
                    Question("$base^$exp", base.toDouble().pow(exp.toDouble()), operation, groupId)
                }
            }
            MathOperation.FRACTIONS -> range.filter { it > 1 }.flatMap { denom ->
                (1 until denom).map { num ->
                    Question("$num/$denom", num.toDouble() / denom.toDouble(), operation, groupId)
                }
            }
        }
    }

    private fun isExcludedQuestion(question: Question, excluded: Set<Int>): Boolean {
        if (excluded.isEmpty()) return false
        val text = question.questionText
        val num = when (question.operationType) {
            MathOperation.MULTIPLICATION -> text.substringBefore(" \u00d7").toIntOrNull()
            MathOperation.DIVISION       -> text.substringAfterLast("\u00f7 ").trim().toIntOrNull()
            MathOperation.POWERS         -> text.substringBefore("^").toIntOrNull()
            MathOperation.FRACTIONS      -> text.substringAfterLast("/").toIntOrNull()
        }
        return num != null && num in excluded
    }
}

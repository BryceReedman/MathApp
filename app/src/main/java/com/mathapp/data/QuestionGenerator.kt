package com.mathapp.data

import kotlin.math.pow
import kotlin.random.Random

object QuestionGenerator {

    fun generate(operation: MathOperation, groupId: Int): Question = when (operation) {
        MathOperation.MULTIPLICATION -> multiplication(groupId)
        MathOperation.DIVISION       -> division(groupId)
        MathOperation.POWERS         -> powers(groupId)
        MathOperation.FRACTIONS      -> fractions(groupId)
    }

    fun generateFromGroups(operation: MathOperation, groupIds: List<Int>): Question {
        val groupId = groupIds.random()
        return generate(operation, groupId)
    }

    fun generateStruggling(
        operation: MathOperation,
        groupIds: List<Int>,
        strugglingKeys: Set<String>
    ): Question {
        // Collect all possible question texts for the struggling keys that match selected groups
        val candidates = buildCandidatePool(operation, groupIds, strugglingKeys)
        return if (candidates.isNotEmpty() && Random.nextFloat() < 0.85f) {
            candidates.random()
        } else {
            generateFromGroups(operation, groupIds)
        }
    }

    private fun buildCandidatePool(
        operation: MathOperation,
        groupIds: List<Int>,
        strugglingKeys: Set<String>
    ): List<Question> {
        val candidates = mutableListOf<Question>()
        for (groupId in groupIds) {
            val questions = allQuestionsForGroup(operation, groupId)
            for (q in questions) {
                if (q.questionKey in strugglingKeys) candidates.add(q)
            }
        }
        return candidates
    }

    private fun allQuestionsForGroup(operation: MathOperation, groupId: Int): List<Question> =
        when (operation) {
            MathOperation.MULTIPLICATION -> (1..12).map { other ->
                val a = groupId; val b = other
                Question("$a × $b", (a * b).toDouble(), operation, groupId)
            }
            MathOperation.DIVISION -> (1..12).map { quotient ->
                val dividend = groupId * quotient
                Question("$dividend ÷ $groupId", quotient.toDouble(), operation, groupId)
            }
            MathOperation.POWERS -> (2..5).map { exp ->
                val result = groupId.toDouble().pow(exp.toDouble())
                Question("$groupId^$exp", result, operation, groupId)
            }
            MathOperation.FRACTIONS -> (1 until groupId).map { numerator ->
                val result = numerator.toDouble() / groupId.toDouble()
                Question("$numerator/$groupId", result, operation, groupId)
            }
        }

    private fun multiplication(groupId: Int): Question {
        val other = Random.nextInt(1, 13)
        val a = groupId; val b = other
        return Question("$a × $b", (a * b).toDouble(), MathOperation.MULTIPLICATION, groupId)
    }

    private fun division(groupId: Int): Question {
        val quotient = Random.nextInt(1, 13)
        val dividend = groupId * quotient
        return Question(
            "$dividend ÷ $groupId",
            quotient.toDouble(),
            MathOperation.DIVISION,
            groupId
        )
    }

    private fun powers(groupId: Int): Question {
        val exp = Random.nextInt(2, 6)
        val result = groupId.toDouble().pow(exp.toDouble())
        return Question("$groupId^$exp", result, MathOperation.POWERS, groupId)
    }

    private fun fractions(groupId: Int): Question {
        val numerator = Random.nextInt(1, groupId)
        val result = numerator.toDouble() / groupId.toDouble()
        return Question("$numerator/$groupId", result, MathOperation.FRACTIONS, groupId)
    }
}

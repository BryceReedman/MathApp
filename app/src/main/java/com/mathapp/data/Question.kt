package com.mathapp.data

import kotlin.math.floor

data class Question(
    val questionText: String,
    val correctAnswer: Double,
    val operationType: MathOperation,
    val groupId: Int,
    val questionKey: String = "${operationType.name}_$questionText"
) {
    val displayAnswer: String get() = if (correctAnswer == floor(correctAnswer)) {
        correctAnswer.toInt().toString()
    } else {
        "%.3f".format(correctAnswer).trimEnd('0').trimEnd('.')
    }
}

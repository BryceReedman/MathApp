package com.mathapp.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository
import com.mathapp.data.Question
import com.mathapp.data.QuestionGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

data class QuizState(
    val currentQuestion: Question? = null,
    val userInput: String = "",
    val feedbackState: Boolean? = null,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val correctCount: Int = 0,
    val totalTimeMs: Long = 0L,
    val isComplete: Boolean = false
)

class QuizViewModel : ViewModel() {

    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state.asStateFlow()

    private lateinit var operation: MathOperation
    private var groupIds: List<Int> = emptyList()
    private var strugglingOnly: Boolean = false
    private var strugglingKeys: Set<String> = emptySet()
    private lateinit var repository: ProgressRepository
    private var questionStartTime = 0L
    private var initialized = false

    fun initialize(
        operation: MathOperation,
        groupIds: List<Int>,
        totalCount: Int,
        strugglingOnly: Boolean,
        repository: ProgressRepository
    ) {
        if (initialized) return
        initialized = true

        this.operation      = operation
        this.groupIds       = groupIds
        this.strugglingOnly = strugglingOnly
        this.repository     = repository

        if (strugglingOnly) {
            strugglingKeys = repository.getStrugglingKeys(operation, groupIds)
        }

        val firstQuestion = nextQuestion()
        questionStartTime = System.currentTimeMillis()
        _state.value = QuizState(
            currentQuestion = firstQuestion,
            totalCount      = totalCount
        )
    }

    fun onKeyPress(key: String) {
        val current = _state.value
        if (current.feedbackState != null) return
        when (key) {
            "⌫" -> _state.value = current.copy(
                userInput = current.userInput.dropLast(1)
            )
            "." -> {
                if ("." !in current.userInput) {
                    _state.value = current.copy(userInput = current.userInput + ".")
                }
            }
            else -> {
                if (current.userInput.length < 10) {
                    _state.value = current.copy(userInput = current.userInput + key)
                }
            }
        }
    }

    fun onSubmit() {
        val current = _state.value
        if (current.feedbackState != null || current.userInput.isBlank()) return

        val question    = current.currentQuestion ?: return
        val userAnswer  = current.userInput.toDoubleOrNull() ?: return
        val timeMs      = System.currentTimeMillis() - questionStartTime
        val isCorrect   = isAnswerCorrect(userAnswer, question)

        repository.recordAnswer(question.questionKey, operation, isCorrect, timeMs)

        _state.value = current.copy(
            feedbackState = isCorrect,
            correctCount  = if (isCorrect) current.correctCount + 1 else current.correctCount,
            totalTimeMs   = current.totalTimeMs + timeMs
        )

        viewModelScope.launch {
            delay(if (isCorrect) 800L else 1500L)
            advance()
        }
    }

    private fun advance() {
        val current   = _state.value
        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.totalCount) {
            _state.value = current.copy(isComplete = true)
        } else {
            val q = nextQuestion()
            questionStartTime = System.currentTimeMillis()
            _state.value = current.copy(
                currentQuestion = q,
                currentIndex    = nextIndex,
                userInput       = "",
                feedbackState   = null
            )
        }
    }

    private fun nextQuestion(): Question = if (strugglingOnly && strugglingKeys.isNotEmpty()) {
        QuestionGenerator.generateStruggling(operation, groupIds, strugglingKeys)
    } else {
        QuestionGenerator.generateFromGroups(operation, groupIds)
    }

    private fun isAnswerCorrect(userAnswer: Double, question: Question): Boolean =
        when (question.operationType) {
            MathOperation.FRACTIONS -> abs(userAnswer - question.correctAnswer) < 0.001
            else                    -> userAnswer == question.correctAnswer
        }
}

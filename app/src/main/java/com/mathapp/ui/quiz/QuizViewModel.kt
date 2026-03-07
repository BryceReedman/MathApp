package com.mathapp.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository
import com.mathapp.data.Question
import com.mathapp.data.QuestionGenerator
import com.mathapp.data.QuestionGroups
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

    private lateinit var sessionOperation: MathOperation
    private var groupIds: List<Int> = emptyList()
    private var strugglingOnly: Boolean = false
    private lateinit var repository: ProgressRepository
    private var questionStartTime = 0L
    private var initialized = false

    private lateinit var questionQueue: ArrayDeque<Question>

    fun initialize(
        operation: MathOperation,
        groupIds: List<Int>,
        totalCount: Int,
        strugglingOnly: Boolean,
        repository: ProgressRepository
    ) {
        if (initialized) return
        initialized = true

        this.sessionOperation = operation
        this.groupIds         = groupIds
        this.strugglingOnly   = strugglingOnly
        this.repository       = repository

        val excludedNumbers = repository.getExcludedNumbers(operation)
        val factors = QuestionGroups.buildFactorList(groupIds, excludedNumbers)
        val strugglingKeys = if (strugglingOnly) {
            repository.getStrugglingKeys(operation, groupIds)
        } else null

        val pool = QuestionGenerator.buildShuffledPool(
            operation, factors, strugglingKeys, totalCount
        )
        questionQueue = ArrayDeque(pool)

        val firstQuestion = nextQuestion()
        questionStartTime = System.currentTimeMillis()
        _state.value = QuizState(
            currentQuestion = firstQuestion,
            totalCount      = totalCount
        )
    }

    /** Used for Mixed Mode: merge question pools from multiple operations. */
    fun initializeMixed(
        operations: List<MathOperation>,
        groupIds: List<Int>,
        totalCount: Int,
        repository: ProgressRepository
    ) {
        if (initialized) return
        initialized = true

        this.sessionOperation = MathOperation.MIXED
        this.groupIds         = groupIds
        this.strugglingOnly   = false
        this.repository       = repository

        val defaultGroups = if (groupIds.isEmpty()) listOf(1) else groupIds
        val pool = operations.flatMap { op ->
            val factors = QuestionGroups.buildFactorList(defaultGroups, repository.getExcludedNumbers(op))
            QuestionGenerator.allQuestions(op, factors)
        }.shuffled().take(totalCount)

        questionQueue = ArrayDeque(pool)

        val firstQuestion = nextQuestion()
        questionStartTime = System.currentTimeMillis()
        _state.value = QuizState(
            currentQuestion = firstQuestion,
            totalCount      = pool.size.coerceAtLeast(1)
        )
    }

    fun onKeyPress(key: String) {
        val current = _state.value
        if (current.feedbackState != null) return
        val isFactorization = current.currentQuestion?.operationType == MathOperation.FACTORIZATION
        when (key) {
            "\u232b" -> _state.value = current.copy(
                userInput = current.userInput.dropLast(1)
            )
            "." -> {
                if (!isFactorization && "." !in current.userInput) {
                    _state.value = current.copy(userInput = current.userInput + ".")
                }
            }
            "\u00d7" -> {  // × separator for factorization
                if (isFactorization && "\u00d7" !in current.userInput && current.userInput.isNotEmpty()) {
                    _state.value = current.copy(userInput = current.userInput + "\u00d7")
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

        val question = current.currentQuestion ?: return
        val timeMs   = System.currentTimeMillis() - questionStartTime

        val isCorrect = if (question.operationType == MathOperation.FACTORIZATION) {
            checkFactorization(current.userInput, question.correctAnswer.toInt())
        } else {
            val userAnswer = current.userInput.toDoubleOrNull() ?: return
            isAnswerCorrect(userAnswer, question)
        }

        // Record under the question's actual operation (important for MIXED sessions)
        repository.recordAnswer(question.questionKey, question.operationType, isCorrect, timeMs)
        repository.updateSrs(question.questionKey, isCorrect, timeMs)
        repository.incrementDailyProgress()

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

    private fun checkFactorization(input: String, target: Int): Boolean {
        val parts = input.split("\u00d7")
        if (parts.size != 2) return false
        val a = parts[0].trim().toIntOrNull() ?: return false
        val b = parts[1].trim().toIntOrNull() ?: return false
        return a >= 2 && b >= 2 && a * b == target
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

    private fun nextQuestion(): Question = questionQueue.removeFirst()

    private fun isAnswerCorrect(userAnswer: Double, question: Question): Boolean =
        when (question.operationType) {
            MathOperation.FRACTIONS,
            MathOperation.PERCENTAGES -> abs(userAnswer - question.correctAnswer) < 0.01
            else                      -> userAnswer == question.correctAnswer
        }
}

package com.mathapp.ui.journey

import androidx.lifecycle.ViewModel
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository
import com.mathapp.data.QuestionStats
import com.mathapp.data.SessionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class JourneyState(
    val selectedOperation: MathOperation = MathOperation.MULTIPLICATION,
    val sessionHistory: List<SessionRecord> = emptyList(),
    val strugglingQuestions: List<QuestionStats> = emptyList(),
    val totalAnswered: Int = 0,
    val overallAccuracy: Float = 0f,
    val overallAvgTimeMs: Long = 0L
)

class JourneyViewModel(private val repository: ProgressRepository) : ViewModel() {

    private val _state = MutableStateFlow(JourneyState())
    val state: StateFlow<JourneyState> = _state.asStateFlow()

    fun selectOperation(operation: MathOperation) {
        val history      = repository.getSessionHistory(operation)
        val allStats     = repository.getAllQuestionStats(operation)
        val struggling   = allStats.filter { it.isStruggling }
            .sortedByDescending { it.recentErrorRate * 0.7f + (it.recentAvgTimeMs / 10_000f).coerceAtMost(1f) * 0.3f }
        val totalAnswered = history.sumOf { it.total }
        val overallCorrect = history.sumOf { it.correct }
        val overallAccuracy = if (totalAnswered == 0) 0f
            else overallCorrect.toFloat() / totalAnswered.toFloat() * 100f
        val overallAvgTime = if (history.isEmpty()) 0L
            else history.sumOf { it.avgTimeMs } / history.size

        _state.value = JourneyState(
            selectedOperation  = operation,
            sessionHistory     = history,
            strugglingQuestions = struggling,
            totalAnswered      = totalAnswered,
            overallAccuracy    = overallAccuracy,
            overallAvgTimeMs   = overallAvgTime
        )
    }
}

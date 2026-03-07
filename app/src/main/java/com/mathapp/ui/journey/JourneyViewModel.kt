package com.mathapp.ui.journey

import androidx.lifecycle.ViewModel
import com.mathapp.data.GroupStatus
import com.mathapp.data.MathOperation
import com.mathapp.data.Milestone
import com.mathapp.data.ProgressRepository
import com.mathapp.data.QuestionGroup
import com.mathapp.data.QuestionGroups
import com.mathapp.data.QuestionStats
import com.mathapp.data.SessionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GroupStat(
    val group: QuestionGroup,
    val accuracy: Float,   // 0-100; -1 = no data
    val attempted: Int
) {
    val status: GroupStatus get() = when {
        attempted == 0  -> GroupStatus.NONE
        accuracy >= 80f -> GroupStatus.GREEN
        accuracy >= 50f -> GroupStatus.YELLOW
        else            -> GroupStatus.RED
    }
}

data class JourneyState(
    val selectedOperation: MathOperation = MathOperation.MULTIPLICATION,
    val sessionHistory: List<SessionRecord> = emptyList(),
    val strugglingQuestions: List<QuestionStats> = emptyList(),
    val totalAnswered: Int = 0,
    val overallAccuracy: Float = 0f,
    val overallAvgTimeMs: Long = 0L,
    val groupStats: List<GroupStat> = emptyList(),
    val milestones: List<Milestone> = emptyList()
)

class JourneyViewModel(private val repository: ProgressRepository) : ViewModel() {

    private val _state = MutableStateFlow(JourneyState())
    val state: StateFlow<JourneyState> = _state.asStateFlow()

    fun selectOperation(operation: MathOperation) {
        val history         = repository.getSessionHistory(operation)
        val allStats        = repository.getAllQuestionStats(operation)
        val struggling      = allStats.filter { it.isStruggling }
            .sortedByDescending { it.recentErrorRate * 0.7f + (it.recentAvgTimeMs / 10_000f).coerceAtMost(1f) * 0.3f }
        val totalAnswered    = history.sumOf { it.total }
        val overallCorrect   = history.sumOf { it.correct }
        val overallAccuracy  = if (totalAnswered == 0) 0f
            else overallCorrect.toFloat() / totalAnswered.toFloat() * 100f
        val overallAvgTime   = if (history.isEmpty()) 0L
            else history.sumOf { it.avgTimeMs } / history.size

        val groupStats = QuestionGroups.forOperation(operation).map { group ->
            val (accuracy, attempted) = repository.getGroupAccuracy(operation, group.id)
            GroupStat(group, accuracy, attempted)
        }.filter { it.attempted > 0 }

        val milestones = repository.getMilestones(operation)

        _state.value = JourneyState(
            selectedOperation   = operation,
            sessionHistory      = history,
            strugglingQuestions = struggling,
            totalAnswered       = totalAnswered,
            overallAccuracy     = overallAccuracy,
            overallAvgTimeMs    = overallAvgTime,
            groupStats          = groupStats,
            milestones          = milestones
        )
    }
}

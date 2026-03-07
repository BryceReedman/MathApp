package com.mathapp.ui.daily

import androidx.lifecycle.ViewModel
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DailyState(
    val goalTotal: Int = 20,
    val progressToday: Int = 0,
    val streak: Int = 0,
    val dueByOperation: Map<MathOperation, Int> = emptyMap(),
    val totalDue: Int = 0
)

class DailyViewModel(private val repository: ProgressRepository) : ViewModel() {

    private val _state = MutableStateFlow(DailyState())
    val state: StateFlow<DailyState> = _state.asStateFlow()

    private val trackableOps = MathOperation.entries.filter {
        it != MathOperation.MIXED
    }

    fun load() {
        val goal = repository.getDailyGoal()
        val progress = repository.getDailyProgressToday()
        val streak = repository.getStreak()
        val due = repository.getDueQuestionCounts(trackableOps)
        val totalDue = due.values.sum()
        _state.value = DailyState(
            goalTotal = goal,
            progressToday = progress,
            streak = streak,
            dueByOperation = due.filter { it.value > 0 },
            totalDue = totalDue
        )
    }

    fun setGoal(goal: Int) {
        repository.setDailyGoal(goal)
        load()
    }

    /** Returns the list of operations to quiz on for today's daily session. */
    fun getDailyOperations(): List<MathOperation> {
        val due = _state.value.dueByOperation
        return if (due.isNotEmpty()) due.keys.toList()
        else listOf(MathOperation.MULTIPLICATION, MathOperation.ADDITION)
    }

    fun getDailyCount(): Int =
        (_state.value.goalTotal - _state.value.progressToday).coerceAtLeast(10)
}

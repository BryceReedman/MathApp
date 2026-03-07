package com.mathapp.ui.home

import androidx.lifecycle.ViewModel
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(private val repository: ProgressRepository) : ViewModel() {

    private val _accuracyMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val accuracyMap: StateFlow<Map<String, Float>> = _accuracyMap.asStateFlow()

    fun loadProgress() {
        fun combinedAccuracy(ops: List<MathOperation>): Float {
            val values = ops.map { repository.getOverallAccuracy(it) }.filter { it > 0f }
            return if (values.isEmpty()) 0f else values.average().toFloat()
        }
        _accuracyMap.value = mapOf(
            "MULT_DIV"    to combinedAccuracy(listOf(MathOperation.MULTIPLICATION, MathOperation.DIVISION, MathOperation.FACTORIZATION)),
            "ADD_SUB"     to combinedAccuracy(listOf(MathOperation.ADDITION, MathOperation.SUBTRACTION)),
            "POWERS_ROOTS" to combinedAccuracy(listOf(MathOperation.POWERS, MathOperation.SQUARE_ROOTS, MathOperation.CUBE_ROOTS)),
            "FRACTIONS"   to repository.getOverallAccuracy(MathOperation.FRACTIONS),
            "PERCENTAGES" to repository.getOverallAccuracy(MathOperation.PERCENTAGES),
            "MIXED"       to repository.getOverallAccuracy(MathOperation.MIXED),
        )
    }
}

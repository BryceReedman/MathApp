package com.mathapp.ui.home

import androidx.lifecycle.ViewModel
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(private val repository: ProgressRepository) : ViewModel() {

    private val _accuracyMap = MutableStateFlow<Map<MathOperation, Float>>(emptyMap())
    val accuracyMap: StateFlow<Map<MathOperation, Float>> = _accuracyMap.asStateFlow()

    fun loadProgress() {
        _accuracyMap.value = MathOperation.entries.associateWith { op ->
            repository.getOverallAccuracy(op)
        }
    }
}

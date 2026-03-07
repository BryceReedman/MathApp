package com.mathapp.ui.quiz

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository
import com.mathapp.data.SessionRecord
import com.mathapp.ui.theme.Green
import com.mathapp.ui.theme.Red

@Composable
fun QuizScreen(
    operation: MathOperation,
    groupIds: List<Int>,
    questionCount: Int,
    strugglingOnly: Boolean,
    repository: ProgressRepository,
    mixedOperations: List<MathOperation>? = null,
    onQuizComplete: (correct: Int, total: Int, avgTimeMs: Long) -> Unit
) {
    val vm: QuizViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(operation, groupIds, questionCount, strugglingOnly) {
        if (operation == MathOperation.MIXED && mixedOperations != null) {
            vm.initializeMixed(mixedOperations, groupIds, questionCount, repository)
        } else {
            vm.initialize(operation, groupIds, questionCount, strugglingOnly, repository)
        }
    }

    // Sound + vibration when feedback arrives
    LaunchedEffect(state.feedbackState) {
        when (state.feedbackState) {
            true  -> { playTone(ToneGenerator.TONE_PROP_BEEP, 120); vibrate(context, longArrayOf(0, 60)) }
            false -> { playTone(ToneGenerator.TONE_PROP_BEEP2, 250); vibrate(context, longArrayOf(0, 80, 60, 80)) }
            null  -> {}
        }
    }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            val avgTime = if (state.totalCount > 0) state.totalTimeMs / state.totalCount else 0L
            repository.recordSession(
                SessionRecord(
                    timestampMs    = System.currentTimeMillis(),
                    operation      = operation,
                    selectedGroups = groupIds,
                    strugglingOnly = strugglingOnly,
                    correct        = state.correctCount,
                    total          = state.totalCount,
                    avgTimeMs      = avgTime
                )
            )
            if (operation != MathOperation.MIXED) {
                repository.checkAndRecordMilestones(operation, groupIds)
            }
            onQuizComplete(state.correctCount, state.totalCount, avgTime)
        }
    }

    val question = state.currentQuestion ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text  = "Question ${state.currentIndex + 1} of ${state.totalCount}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = (state.currentIndex + 1).toFloat() / state.totalCount.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${state.correctCount} correct",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text      = question.questionText,
                style     = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            AnswerDisplay(
                userInput     = state.userInput,
                feedbackState = state.feedbackState,
                correctAnswer = question.displayAnswer
            )
        }

        val currentOp = question.operationType
        NumericKeypad(
            onKey         = { vm.onKeyPress(it) },
            showDecimal   = currentOp == MathOperation.FRACTIONS || currentOp == MathOperation.PERCENTAGES,
            showFactorSep = currentOp == MathOperation.FACTORIZATION,
            enabled       = state.feedbackState == null
        )
    }
}

@Composable
fun AnswerDisplay(
    userInput: String,
    feedbackState: Boolean?,
    correctAnswer: String
) {
    val bgColor by animateColorAsState(
        targetValue = when (feedbackState) {
            true  -> Green.copy(alpha = 0.2f)
            false -> Red.copy(alpha = 0.2f)
            null  -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "answerBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = if (userInput.isEmpty()) "?" else userInput,
            style = MaterialTheme.typography.displayLarge,
            color = if (userInput.isEmpty())
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.onSurface
        )
    }

    if (feedbackState == false) {
        Spacer(Modifier.height(8.dp))
        Text(
            text       = "Correct answer: $correctAnswer",
            style      = MaterialTheme.typography.bodyLarge,
            color      = Red,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun NumericKeypad(
    onKey: (String) -> Unit,
    showDecimal: Boolean,
    showFactorSep: Boolean = false,
    enabled: Boolean
) {
    val view = LocalView.current

    // Bottom-row special key: "×" for factorization, "." for fractions/percentages, empty otherwise
    val specialKey = when {
        showFactorSep -> "\u00d7"
        showDecimal   -> "."
        else          -> null
    }

    fun pressKey(key: String) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        onKey(key)
    }

    val rows = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick  = { pressKey(key) },
                        enabled  = enabled,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text(key, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
        // Bottom row: [special/blank] [0] [⌫]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (specialKey != null) {
                OutlinedButton(
                    onClick  = { pressKey(specialKey) },
                    enabled  = enabled,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text(specialKey, style = MaterialTheme.typography.headlineMedium)
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            OutlinedButton(
                onClick  = { pressKey("0") },
                enabled  = enabled,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("0", style = MaterialTheme.typography.headlineMedium)
            }
            OutlinedButton(
                onClick  = { pressKey("\u232b") },
                enabled  = enabled,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("⌫", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

private fun playTone(tone: Int, durationMs: Int) {
    try {
        val tg = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME * 2 / 3)
        tg.startTone(tone, durationMs)
    } catch (_: Exception) {}
}

@Suppress("DEPRECATION")
private fun vibrate(context: Context, pattern: LongArray) {
    try {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings  = pattern.drop(1).toLongArray()
            val amps     = IntArray(timings.size) { VibrationEffect.DEFAULT_AMPLITUDE }
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    } catch (_: Exception) {}
}

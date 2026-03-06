package com.mathapp.ui.results

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mathapp.data.MathOperation

@Composable
fun ResultsScreen(
    operation: MathOperation,
    correct: Int,
    total: Int,
    avgTimeMs: Long,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    val accuracy = if (total == 0) 0f else correct.toFloat() / total.toFloat() * 100f
    val avgTimeSec = avgTimeMs / 1000.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when {
                accuracy >= 90f -> "Excellent!"
                accuracy >= 70f -> "Good job!"
                accuracy >= 50f -> "Keep practising!"
                else            -> "Try again!"
            },
            style     = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        Text(
            text       = "$correct / $total",
            style      = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = "correct",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(8.dp))

        Text(
            text  = "%.0f%% accuracy".format(accuracy),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))

        Text(
            text  = "Avg time: %.1fs per question".format(avgTimeSec),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(48.dp))

        Button(
            onClick  = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Try Again")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick  = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Home")
        }
    }
}

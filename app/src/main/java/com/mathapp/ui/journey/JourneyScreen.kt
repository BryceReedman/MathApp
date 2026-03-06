package com.mathapp.ui.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository
import com.mathapp.data.QuestionStats
import com.mathapp.data.SessionRecord
import com.mathapp.ui.theme.Blue40
import com.mathapp.ui.theme.Green
import com.mathapp.ui.theme.Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    repository: ProgressRepository,
    onBack: () -> Unit
) {
    val vm: JourneyViewModel = viewModel(factory = androidx.lifecycle.viewmodel.initializer {
        JourneyViewModel(repository)
    })
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.selectOperation(MathOperation.MULTIPLICATION) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journey") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Operation selector ───────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MathOperation.entries.forEach { op ->
                    FilterChip(
                        selected = state.selectedOperation == op,
                        onClick  = { vm.selectOperation(op) },
                        label    = { Text(op.symbol) }
                    )
                }
            }

            // ── Stats summary ────────────────────────────────────────────────
            if (state.totalAnswered > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "Answered",
                        value = state.totalAnswered.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Accuracy",
                        value = "%.0f%%".format(state.overallAccuracy),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Avg time",
                        value = "%.1fs".format(state.overallAvgTimeMs / 1000.0),
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Accuracy line graph ──────────────────────────────────────
                if (state.sessionHistory.size >= 2) {
                    Text("Accuracy over time", style = MaterialTheme.typography.headlineMedium)
                    AccuracyGraph(
                        sessions = state.sessionHistory.takeLast(20),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }

                // ── Struggling questions ─────────────────────────────────────
                if (state.strugglingQuestions.isNotEmpty()) {
                    Text(
                        "Struggling questions (${state.strugglingQuestions.size})",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    state.strugglingQuestions.take(15).forEach { stats ->
                        StrugglingRow(stats)
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Green.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("No struggling questions", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Green)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Complete a quiz to see your journey",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun AccuracyGraph(sessions: List<SessionRecord>, modifier: Modifier = Modifier) {
    val primaryColor = Blue40
    Card(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (sessions.size < 2) return@Canvas

            val w = size.width
            val h = size.height
            val padBottom = 20f
            val plotH = h - padBottom

            // Axis
            drawLine(Color.LightGray, Offset(0f, 0f), Offset(0f, plotH), strokeWidth = 1f)
            drawLine(Color.LightGray, Offset(0f, plotH), Offset(w, plotH), strokeWidth = 1f)

            // Data points
            val points = sessions.mapIndexed { i, s ->
                val x = if (sessions.size == 1) w / 2f else i.toFloat() / (sessions.size - 1) * w
                val y = plotH - (s.accuracy / 100f * plotH)
                Offset(x, y)
            }

            // Polyline
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, primaryColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Dots
            points.forEach { pt ->
                drawCircle(primaryColor, radius = 5f, center = pt)
                drawCircle(Color.White, radius = 3f, center = pt)
            }
        }
    }
}

@Composable
fun StrugglingRow(stats: QuestionStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Red.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stats.displayQuestionText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Error rate: %.0f%%  ·  Avg: %.1fs".format(
                        stats.recentErrorRate * 100f,
                        stats.recentAvgTimeMs / 1000.0
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            // Small error bar
            LinearProgressIndicator(
                progress = { stats.recentErrorRate },
                modifier = Modifier.width(60.dp),
                color = Red
            )
        }
    }
}

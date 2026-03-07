package com.mathapp.ui.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathapp.data.GroupStatus
import com.mathapp.data.MathOperation
import com.mathapp.data.Milestone
import com.mathapp.data.ProgressRepository
import com.mathapp.data.QuestionStats
import com.mathapp.data.SessionRecord
import com.mathapp.ui.theme.Blue40
import com.mathapp.ui.theme.Green
import com.mathapp.ui.theme.Red

private val Yellow = Color(0xFFFFC107)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    repository: ProgressRepository
) {
    val vm: JourneyViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = JourneyViewModel(repository) as T
    })
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.selectOperation(MathOperation.MULTIPLICATION) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Operation selector ───────────────────────────────────────────
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JOURNEY_OPERATIONS.forEach { op ->
                FilterChip(
                    selected = state.selectedOperation == op,
                    onClick  = { vm.selectOperation(op) },
                    label    = { Text(op.symbol) }
                )
            }
        }

        // ── Group performance ────────────────────────────────────────────
        if (state.groupStats.isNotEmpty()) {
            Text("Group performance", style = MaterialTheme.typography.titleLarge)
            GroupPerformanceRow(state.groupStats)
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
                Text("Accuracy over time", style = MaterialTheme.typography.titleLarge)
                AccuracyGraph(
                    sessions   = state.sessionHistory.takeLast(20),
                    milestones = state.milestones,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            // ── Struggling questions ─────────────────────────────────────
            if (state.strugglingQuestions.isNotEmpty()) {
                Text(
                    "Struggling questions (${state.strugglingQuestions.size})",
                    style = MaterialTheme.typography.titleLarge
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "No struggling questions",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Green
                        )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupPerformanceRow(groupStats: List<GroupStat>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupStats.forEach { stat ->
            val (bgColor, labelColor) = when (stat.status) {
                GroupStatus.GREEN  -> Green.copy(alpha = 0.2f) to Green
                GroupStatus.YELLOW -> Yellow.copy(alpha = 0.2f) to Yellow
                GroupStatus.RED    -> Red.copy(alpha = 0.2f) to Red
                GroupStatus.NONE   -> Color.Gray.copy(alpha = 0.1f) to Color.Gray
            }
            Surface(
                color = bgColor,
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stat.group.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = labelColor
                    )
                    if (stat.accuracy >= 0f) {
                        Text(
                            "%.0f%%".format(stat.accuracy),
                            style = MaterialTheme.typography.bodyMedium,
                            color = labelColor.copy(alpha = 0.8f)
                        )
                    }
                    stat.speedRecord?.let { ms ->
                        Text(
                            "\uD83C\uDFC6 %.1fs".format(ms / 1000.0),
                            style = MaterialTheme.typography.bodySmall,
                            color = labelColor.copy(alpha = 0.7f)
                        )
                    }
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
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun AccuracyGraph(
    sessions: List<SessionRecord>,
    milestones: List<Milestone>,
    modifier: Modifier = Modifier
) {
    val primaryColor = Blue40
    val yellowColor  = Yellow
    val greenColor   = Green

    Card(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (sessions.size < 2) return@Canvas

            val w = size.width
            val h = size.height
            val padBottom = 20f
            val padTop    = 20f
            val plotH = h - padBottom - padTop

            drawLine(Color.LightGray, Offset(0f, padTop), Offset(0f, padTop + plotH), strokeWidth = 1f)
            drawLine(Color.LightGray, Offset(0f, padTop + plotH), Offset(w, padTop + plotH), strokeWidth = 1f)

            val points = sessions.mapIndexed { i, s ->
                val x = if (sessions.size == 1) w / 2f else i.toFloat() / (sessions.size - 1) * w
                val y = padTop + plotH - (s.accuracy / 100f * plotH)
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, primaryColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

            points.forEach { pt ->
                drawCircle(primaryColor, radius = 5f, center = pt)
                drawCircle(Color.White, radius = 3f, center = pt)
            }

            milestones.forEach { milestone ->
                val idx = sessions.indexOfFirst { it.timestampMs >= milestone.timestampMs }
                    .takeIf { it >= 0 } ?: return@forEach
                val x = if (sessions.size == 1) w / 2f else idx.toFloat() / (sessions.size - 1) * w
                val tipY = padTop - 4f
                val triangleSize = 10f
                val color = if (milestone.newStatus == GroupStatus.GREEN) greenColor else yellowColor

                val trianglePath = Path().apply {
                    moveTo(x, tipY)
                    lineTo(x - triangleSize / 2f, tipY + triangleSize)
                    lineTo(x + triangleSize / 2f, tipY + triangleSize)
                    close()
                }
                drawPath(trianglePath, color)
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
                    "Error rate: %.0f%%  \u00b7  Avg: %.1fs".format(
                        stats.recentErrorRate * 100f,
                        stats.recentAvgTimeMs / 1000.0
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            LinearProgressIndicator(
                progress = stats.recentErrorRate,
                modifier = Modifier.width(60.dp),
                color = Red
            )
        }
    }
}

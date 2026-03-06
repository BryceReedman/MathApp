package com.mathapp.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository
import com.mathapp.data.QuestionGroups

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizConfigScreen(
    operation: MathOperation,
    repository: ProgressRepository,
    onStartQuiz: (groupIds: List<Int>, count: Int, strugglingOnly: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val allGroups = QuestionGroups.forOperation(operation)
    var selectedGroupIds by remember { mutableStateOf(QuestionGroups.defaultSelected(operation).toSet()) }
    var selectedCount by remember { mutableStateOf(10) }
    var strugglingOnly by remember { mutableStateOf(false) }

    // Recompute struggling count whenever selected groups change
    val strugglingCount by remember(selectedGroupIds) {
        derivedStateOf {
            repository.getStrugglingCount(operation, selectedGroupIds.toList())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(operation.displayName) },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Group selector ───────────────────────────────────────────────
            Text("Select groups", style = MaterialTheme.typography.headlineMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allGroups.forEach { group ->
                    val selected = group.id in selectedGroupIds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedGroupIds = if (selected && selectedGroupIds.size > 1) {
                                selectedGroupIds - group.id
                            } else if (!selected) {
                                selectedGroupIds + group.id
                            } else {
                                selectedGroupIds // prevent deselecting last
                            }
                            // Reset struggling mode if it becomes invalid
                            if (strugglingOnly && repository.getStrugglingCount(operation, selectedGroupIds.toList()) == 0) {
                                strugglingOnly = false
                            }
                        },
                        label = { Text(group.label) }
                    )
                }
            }

            // ── Question count ───────────────────────────────────────────────
            Text("Number of questions", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 40).forEach { count ->
                    FilterChip(
                        selected = selectedCount == count,
                        onClick  = { selectedCount = count },
                        label    = { Text(count.toString()) }
                    )
                }
            }

            // ── Struggling toggle ────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Struggling problems only",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (strugglingCount > 0) {
                                Badge { Text(strugglingCount.toString()) }
                            }
                        }
                        Text(
                            text = if (strugglingCount == 0)
                                "No struggling questions yet — keep practising!"
                            else
                                "Focus on ${strugglingCount} question${if (strugglingCount == 1) "" else "s"} you find hard",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked  = strugglingOnly,
                        onCheckedChange = { if (strugglingCount > 0) strugglingOnly = it },
                        enabled  = strugglingCount > 0
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onStartQuiz(selectedGroupIds.sorted(), selectedCount, strugglingOnly) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Start Quiz", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

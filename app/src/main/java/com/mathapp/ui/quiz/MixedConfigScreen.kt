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
import androidx.compose.ui.unit.dp
import com.mathapp.data.MathOperation

/** Operations available to include in a mixed quiz (excludes MIXED itself). */
private val MIXABLE_OPERATIONS = listOf(
    MathOperation.MULTIPLICATION,
    MathOperation.DIVISION,
    MathOperation.ADDITION,
    MathOperation.SUBTRACTION,
    MathOperation.POWERS,
    MathOperation.SQUARE_ROOTS,
    MathOperation.CUBE_ROOTS,
    MathOperation.FRACTIONS,
    MathOperation.PERCENTAGES,
    MathOperation.FACTORIZATION
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixedConfigScreen(
    onStartMixed: (operations: List<MathOperation>, count: Int) -> Unit,
    onBack: () -> Unit
) {
    var selectedOps by remember {
        mutableStateOf(
            setOf(MathOperation.MULTIPLICATION, MathOperation.DIVISION, MathOperation.ADDITION)
        )
    }
    var selectedCount by remember { mutableStateOf<Int?>(20) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mixed Mode") },
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
            Text("Select operations", style = MaterialTheme.typography.headlineMedium)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MIXABLE_OPERATIONS.forEach { op ->
                    val checked = op in selectedOps
                    Card(
                        onClick = {
                            selectedOps = if (checked && selectedOps.size > 1) {
                                selectedOps - op
                            } else if (!checked) {
                                selectedOps + op
                            } else {
                                selectedOps
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (checked)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(op.symbol, style = MaterialTheme.typography.headlineSmall)
                                Column {
                                    Text(op.displayName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        op.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Checkbox(checked = checked, onCheckedChange = null)
                        }
                    }
                }
            }

            Text("Number of questions", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 40).forEach { count ->
                    FilterChip(
                        selected = selectedCount == count,
                        onClick = { selectedCount = count },
                        label = { Text(count.toString()) }
                    )
                }
                FilterChip(
                    selected = selectedCount == null,
                    onClick = { selectedCount = null },
                    label = { Text("50") }
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val ops = selectedOps.toList()
                    val count = selectedCount ?: 50
                    onStartMixed(ops, count)
                },
                enabled = selectedOps.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Start Mixed Quiz", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

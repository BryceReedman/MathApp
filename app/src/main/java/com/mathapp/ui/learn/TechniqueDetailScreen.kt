package com.mathapp.ui.learn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniqueDetailScreen(
    techniqueId: String,
    onBack: () -> Unit,
    onPractice: ((operationName: String) -> Unit)?
) {
    val technique = techniqueById(techniqueId)

    if (technique == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Not found") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { Text("Technique not found", modifier = Modifier.padding(16.dp)) }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(technique.title) },
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
            // ── Why it works ─────────────────────────────────────────────────
            Section(title = "Why it works") {
                Text(
                    text = technique.why,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // ── How to use it ─────────────────────────────────────────────────
            Section(title = "How to use it") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    technique.howTo.forEachIndexed { i, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (i > 0 || technique.howTo.size > 1) {
                                Text(
                                    "${i + 1}.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(step, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // ── Examples ──────────────────────────────────────────────────────
            Section(title = "Examples") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    technique.examples.forEach { (problem, monologue) ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    problem,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    monologue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Try it button ─────────────────────────────────────────────────
            technique.relatedOperation?.let { op ->
                if (onPractice != null) {
                    Button(
                        onClick = { onPractice(op.name) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            "Practice ${op.displayName} →",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

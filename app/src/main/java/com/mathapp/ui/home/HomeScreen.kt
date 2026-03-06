package com.mathapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: ProgressRepository,
    onOperationSelected: (MathOperation) -> Unit,
    onJourney: () -> Unit
) {
    val vm: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository) as T
    })
    val accuracyMap by vm.accuracyMap.collectAsState()

    LaunchedEffect(Unit) { vm.loadProgress() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MathApp", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onJourney) {
                        Icon(Icons.Default.Timeline, contentDescription = "Journey")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Choose an operation", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            MathOperation.entries.forEach { op ->
                OperationCard(
                    operation = op,
                    accuracy  = accuracyMap[op] ?: 0f,
                    onClick   = { onOperationSelected(op) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationCard(
    operation: MathOperation,
    accuracy: Float,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${operation.symbol}  ${operation.displayName}",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = operation.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val accuracyText = if (accuracy == 0f) "—" else "%.0f%%".format(accuracy)
                Text(
                    text = accuracyText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "accuracy",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

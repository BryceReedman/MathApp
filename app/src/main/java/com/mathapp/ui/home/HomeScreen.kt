package com.mathapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathapp.data.ProgressRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: ProgressRepository,
    onMultDiv: () -> Unit,
    onAddSub: () -> Unit,
    onPowersRoots: () -> Unit,
    onFractions: () -> Unit,
    onPercentages: () -> Unit,
    onMixed: () -> Unit
) {
    val vm: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository) as T
    })
    val accuracyMap by vm.accuracyMap.collectAsState()

    LaunchedEffect(Unit) { vm.loadProgress() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader("Core Skills")
        OperationCard(
            symbol = "×÷",
            name = "Multiplication & Division",
            description = "Times tables, division & factor pairs",
            accuracy = accuracyMap["MULT_DIV"],
            onClick = onMultDiv
        )
        OperationCard(
            symbol = "+−",
            name = "Addition & Subtraction",
            description = "Add and subtract numbers",
            accuracy = accuracyMap["ADD_SUB"],
            onClick = onAddSub
        )

        Spacer(Modifier.height(4.dp))
        SectionHeader("Numbers")
        OperationCard(
            symbol = "^√",
            name = "Powers & Roots",
            description = "Exponents, square & cube roots",
            accuracy = accuracyMap["POWERS_ROOTS"],
            onClick = onPowersRoots
        )
        OperationCard(
            symbol = "/",
            name = "Fractions",
            description = "Convert fractions to decimals",
            accuracy = accuracyMap["FRACTIONS"],
            onClick = onFractions
        )
        OperationCard(
            symbol = "%",
            name = "Percentages",
            description = "Percentage calculations",
            accuracy = accuracyMap["PERCENTAGES"],
            onClick = onPercentages
        )

        Spacer(Modifier.height(4.dp))
        SectionHeader("Techniques")
        OperationCard(
            symbol = "∞",
            name = "Mixed Mode",
            description = "Combine multiple operations",
            accuracy = accuracyMap["MIXED"],
            onClick = onMixed
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationCard(
    symbol: String,
    name: String,
    description: String,
    accuracy: Float?,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$symbol  $name",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            val accuracyText = if (accuracy == null || accuracy == 0f) "—" else "%.0f%%".format(accuracy)
            Text(
                text = accuracyText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

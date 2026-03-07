package com.mathapp.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mathapp.data.ProgressRepository
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val body: String
)

private val PAGES = listOf(
    OnboardingPage(
        emoji = "🧮",
        title = "Build your number sense",
        body = "MathApp helps you practise core arithmetic daily until the answers come instantly — freeing your mind to focus on the bigger picture."
    ),
    OnboardingPage(
        emoji = "📋",
        title = "How it works",
        body = "Practice — pick an operation and drill a custom quiz.\n\nDaily — your personalised review queue, powered by spaced repetition.\n\nJourney — track accuracy, speed, and milestones over time.\n\nLearn — techniques that make hard sums feel easy."
    ),
    OnboardingPage(
        emoji = "💡",
        title = "Learn the techniques",
        body = "Mental math isn't about memorising — it's about seeing patterns and shortcuts.\n\nThe Learn tab teaches you the tricks top mental mathematicians use."
    ),
    OnboardingPage(
        emoji = "🚀",
        title = "Where to start",
        body = "Begin with the basics and expand as your accuracy reaches 80%%.\n\nSuggested first steps:\n★  Multiplication × — Group 1–10\n★  Addition + — Group 1–10"
    )
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    repository: ProgressRepository,
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGES.size - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val page = PAGES[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = page.emoji,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        }

        // Page indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            repeat(PAGES.size) { i ->
                val isSelected = pagerState.currentPage == i
                Surface(
                    modifier = Modifier.size(if (isSelected) 10.dp else 8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                ) {}
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (isLastPage) {
                    repository.setOnboardingComplete()
                    onComplete()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (isLastPage) "Get Started" else "Next →",
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (!isLastPage) {
            TextButton(
                onClick = {
                    repository.setOnboardingComplete()
                    onComplete()
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Skip")
            }
        }
    }
}

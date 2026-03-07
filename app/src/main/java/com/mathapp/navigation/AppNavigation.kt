package com.mathapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository
import com.mathapp.ui.addsub.AddSubModeScreen
import com.mathapp.ui.daily.DailyScreen
import com.mathapp.ui.home.HomeScreen
import com.mathapp.ui.journey.JourneyScreen
import com.mathapp.ui.learn.LearnScreen
import com.mathapp.ui.learn.TechniqueDetailScreen
import com.mathapp.ui.multdiv.MultDivModeScreen
import com.mathapp.ui.onboarding.OnboardingScreen
import com.mathapp.ui.powers.PowersModeScreen
import com.mathapp.ui.quiz.MixedConfigScreen
import com.mathapp.ui.quiz.QuizConfigScreen
import com.mathapp.ui.quiz.QuizScreen
import com.mathapp.ui.results.ResultsScreen

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val TAB_ITEMS = listOf(
    TabItem("home",    "Practice", Icons.Default.Home),
    TabItem("daily",   "Daily",    Icons.Default.Star),
    TabItem("journey", "Journey",  Icons.Default.Timeline),
    TabItem("learn",   "Learn",    Icons.Default.Info)
)

private val TAB_ROOTS = TAB_ITEMS.map { it.route }.toSet()

/** Routes where the bottom nav bar should be hidden (full-screen flows). */
private fun shouldHideBottomNav(route: String?): Boolean {
    if (route == null) return true
    return route.startsWith("quiz/") ||
           route.startsWith("mixed_quiz/") ||
           route.startsWith("results/") ||
           route == "onboarding"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { ProgressRepository(context) }

    val startDestination = if (repository.isOnboardingComplete()) "home" else "onboarding"

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = !shouldHideBottomNav(currentRoute)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TAB_ITEMS.forEach { tab ->
                        val selected = navBackStackEntry?.destination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding)
        ) {

            // ── Onboarding ────────────────────────────────────────────────────
            composable("onboarding") {
                OnboardingScreen(
                    repository = repository,
                    onComplete = {
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            // ══════════════ PRACTICE TAB ══════════════════════════════════════

            composable("home") {
                HomeScreen(
                    repository    = repository,
                    onMultDiv     = { navController.navigate("mult_div_mode") },
                    onAddSub      = { navController.navigate("add_sub_mode") },
                    onPowersRoots = { navController.navigate("powers_mode") },
                    onFractions   = { navController.navigate("quiz_config/${MathOperation.FRACTIONS.name}") },
                    onPercentages = { navController.navigate("quiz_config/${MathOperation.PERCENTAGES.name}") },
                    onMixed       = { navController.navigate("mixed_config") }
                )
            }

            composable("mult_div_mode") {
                MultDivModeScreen(
                    onMultiplication  = { navController.navigate("quiz_config/${MathOperation.MULTIPLICATION.name}") },
                    onDivision        = { navController.navigate("quiz_config/${MathOperation.DIVISION.name}") },
                    onFactorBreakdown = { navController.navigate("quiz_config/${MathOperation.FACTORIZATION.name}") },
                    onBack            = { navController.popBackStack() }
                )
            }

            composable("add_sub_mode") {
                AddSubModeScreen(
                    onAddition    = { navController.navigate("quiz_config/${MathOperation.ADDITION.name}") },
                    onSubtraction = { navController.navigate("quiz_config/${MathOperation.SUBTRACTION.name}") },
                    onBack        = { navController.popBackStack() }
                )
            }

            composable("powers_mode") {
                PowersModeScreen(
                    onPowers      = { navController.navigate("quiz_config/${MathOperation.POWERS.name}") },
                    onSquareRoots = { navController.navigate("quiz_config/${MathOperation.SQUARE_ROOTS.name}") },
                    onCubeRoots   = { navController.navigate("quiz_config/${MathOperation.CUBE_ROOTS.name}") },
                    onBack        = { navController.popBackStack() }
                )
            }

            composable(
                route = "quiz_config/{operationName}",
                arguments = listOf(navArgument("operationName") { type = NavType.StringType })
            ) { back ->
                val op = MathOperation.valueOf(back.arguments!!.getString("operationName")!!)
                QuizConfigScreen(
                    operation   = op,
                    repository  = repository,
                    onStartQuiz = { groupIds, count, strugglingOnly ->
                        val groups = groupIds.joinToString(",")
                        navController.navigate("quiz/${op.name}/$groups/$count/$strugglingOnly")
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("mixed_config") {
                MixedConfigScreen(
                    onStartMixed = { ops, count ->
                        val opsStr = ops.joinToString(",") { it.name }
                        navController.navigate("mixed_quiz/$opsStr/$count")
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Single-operation quiz ─────────────────────────────────────────
            composable(
                route = "quiz/{operationName}/{groupIds}/{questionCount}/{strugglingOnly}",
                arguments = listOf(
                    navArgument("operationName")  { type = NavType.StringType },
                    navArgument("groupIds")       { type = NavType.StringType },
                    navArgument("questionCount")  { type = NavType.IntType },
                    navArgument("strugglingOnly") { type = NavType.BoolType }
                )
            ) { back ->
                val args          = back.arguments!!
                val op            = MathOperation.valueOf(args.getString("operationName")!!)
                val groupIds      = args.getString("groupIds")!!.split(",").map { it.toInt() }
                val count         = args.getInt("questionCount")
                val strugglingOnly = args.getBoolean("strugglingOnly")
                QuizScreen(
                    operation      = op,
                    groupIds       = groupIds,
                    questionCount  = count,
                    strugglingOnly = strugglingOnly,
                    repository     = repository,
                    onQuizComplete = { correct, total, avgTimeMs ->
                        // Update speed records per group
                        groupIds.forEach { gid ->
                            repository.updateSpeedRecord(op, gid, avgTimeMs)
                        }
                        navController.popBackStack()
                        navController.navigate("results/${op.name}/$correct/$total/$avgTimeMs")
                    }
                )
            }

            // ── Mixed quiz ───────────────────────────────────────────────────
            composable(
                route = "mixed_quiz/{ops}/{count}",
                arguments = listOf(
                    navArgument("ops")   { type = NavType.StringType },
                    navArgument("count") { type = NavType.IntType }
                )
            ) { back ->
                val args  = back.arguments!!
                val ops   = args.getString("ops")!!.split(",").map { MathOperation.valueOf(it) }
                val count = args.getInt("count")
                QuizScreen(
                    operation      = MathOperation.MIXED,
                    groupIds       = listOf(1),
                    questionCount  = count,
                    strugglingOnly = false,
                    repository     = repository,
                    mixedOperations = ops,
                    onQuizComplete = { correct, total, avgTimeMs ->
                        navController.popBackStack()
                        navController.navigate("results/${MathOperation.MIXED.name}/$correct/$total/$avgTimeMs")
                    }
                )
            }

            // ── Results ───────────────────────────────────────────────────────
            composable(
                route = "results/{operationName}/{correct}/{total}/{avgTimeMs}",
                arguments = listOf(
                    navArgument("operationName") { type = NavType.StringType },
                    navArgument("correct")       { type = NavType.IntType },
                    navArgument("total")         { type = NavType.IntType },
                    navArgument("avgTimeMs")     { type = NavType.LongType }
                )
            ) { back ->
                val args      = back.arguments!!
                val op        = MathOperation.valueOf(args.getString("operationName")!!)
                val correct   = args.getInt("correct")
                val total     = args.getInt("total")
                val avgTimeMs = args.getLong("avgTimeMs")
                ResultsScreen(
                    operation  = op,
                    correct    = correct,
                    total      = total,
                    avgTimeMs  = avgTimeMs,
                    onRetry    = { navController.popBackStack() },
                    onHome     = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                )
            }

            // ══════════════ DAILY TAB ══════════════════════════════════════════

            composable("daily") {
                DailyScreen(
                    repository = repository,
                    onStartDailyQuiz = { ops, count ->
                        val opsStr = ops.joinToString(",") { it.name }
                        navController.navigate("mixed_quiz/$opsStr/$count")
                    }
                )
            }

            // ══════════════ JOURNEY TAB ══════════════════════════════════════

            composable("journey") {
                JourneyScreen(repository = repository)
            }

            // ══════════════ LEARN TAB ══════════════════════════════════════════

            composable("learn") {
                LearnScreen(
                    onTechnique    = { id -> navController.navigate("technique/$id") },
                    onRunOnboarding = {
                        navController.navigate("onboarding") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = "technique/{techniqueId}",
                arguments = listOf(navArgument("techniqueId") { type = NavType.StringType })
            ) { back ->
                val id = back.arguments!!.getString("techniqueId")!!
                TechniqueDetailScreen(
                    techniqueId = id,
                    onBack      = { navController.popBackStack() },
                    onPractice  = { opName ->
                        navController.navigate("quiz_config/$opName")
                    }
                )
            }
        }
    }
}

package com.mathapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mathapp.data.MathOperation
import com.mathapp.data.ProgressRepository
import com.mathapp.ui.home.HomeScreen
import com.mathapp.ui.journey.JourneyScreen
import com.mathapp.ui.quiz.QuizConfigScreen
import com.mathapp.ui.quiz.QuizScreen
import com.mathapp.ui.results.ResultsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = ProgressRepository(context)

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                repository = repository,
                onOperationSelected = { op ->
                    navController.navigate("quiz_config/${op.name}")
                },
                onJourney = {
                    navController.navigate("journey")
                }
            )
        }

        composable("journey") {
            JourneyScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "quiz_config/{operationName}",
            arguments = listOf(navArgument("operationName") { type = NavType.StringType })
        ) { back ->
            val op = MathOperation.valueOf(back.arguments!!.getString("operationName")!!)
            QuizConfigScreen(
                operation  = op,
                repository = repository,
                onStartQuiz = { groupIds, count, strugglingOnly ->
                    val groups = groupIds.joinToString(",")
                    navController.navigate("quiz/${op.name}/$groups/$count/$strugglingOnly")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "quiz/{operationName}/{groupIds}/{questionCount}/{strugglingOnly}",
            arguments = listOf(
                navArgument("operationName")  { type = NavType.StringType },
                navArgument("groupIds")       { type = NavType.StringType },
                navArgument("questionCount")  { type = NavType.IntType },
                navArgument("strugglingOnly") { type = NavType.BoolType }
            )
        ) { back ->
            val args = back.arguments!!
            val op             = MathOperation.valueOf(args.getString("operationName")!!)
            val groupIds       = args.getString("groupIds")!!.split(",").map { it.toInt() }
            val count          = args.getInt("questionCount")
            val strugglingOnly = args.getBoolean("strugglingOnly")
            QuizScreen(
                operation      = op,
                groupIds       = groupIds,
                questionCount  = count,
                strugglingOnly = strugglingOnly,
                repository     = repository,
                onQuizComplete = { correct, total, avgTimeMs ->
                    navController.popBackStack()
                    navController.navigate("results/${op.name}/$correct/$total/$avgTimeMs")
                }
            )
        }

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
                onHome     = { navController.popBackStack("home", inclusive = false) }
            )
        }
    }
}

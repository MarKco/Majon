package com.ilsecondodasinistra.majon.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ilsecondodasinistra.majon.ui.counter.CounterScreen
import com.ilsecondodasinistra.majon.ui.editproject.EditProjectScreen
import com.ilsecondodasinistra.majon.ui.home.HomeScreen
import com.ilsecondodasinistra.majon.ui.notes.NotesScreen
import com.ilsecondodasinistra.majon.ui.projectdetail.ProjectDetailScreen
import com.ilsecondodasinistra.majon.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val EDIT_PROJECT = "edit_project?projectId={projectId}"
    const val PROJECT_DETAIL = "project/{projectId}"
    const val COUNTER = "counter/{partId}"
    const val NOTES = "notes/{partId}"
    const val SETTINGS = "settings"

    fun editProject(projectId: Long? = null) =
        if (projectId == null) "edit_project?projectId=0" else "edit_project?projectId=$projectId"

    fun projectDetail(projectId: Long) = "project/$projectId"
    fun counter(partId: Long) = "counter/$partId"
    fun notes(partId: Long) = "notes/$partId"
}

@Composable
fun MajonNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) + fadeIn()
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) + fadeOut()
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) + fadeIn()
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) + fadeOut()
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onProjectClick = { navController.navigate(Routes.projectDetail(it)) },
                onAddProject = { navController.navigate(Routes.editProject()) },
                onEditProject = { navController.navigate(Routes.editProject(it)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            Routes.EDIT_PROJECT,
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
        ) {
            EditProjectScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            Routes.PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) {
            ProjectDetailScreen(
                onBack = { navController.popBackStack() },
                onPartClick = { navController.navigate(Routes.counter(it)) },
                onPartNotes = { navController.navigate(Routes.notes(it)) },
                onEditProject = { navController.navigate(Routes.editProject(it)) },
            )
        }

        composable(
            Routes.COUNTER,
            arguments = listOf(navArgument("partId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val partId = backStackEntry.arguments?.getLong("partId") ?: return@composable
            CounterScreen(
                onBack = { navController.popBackStack() },
                onNotes = { navController.navigate(Routes.notes(partId)) },
            )
        }

        composable(
            Routes.NOTES,
            arguments = listOf(navArgument("partId") { type = NavType.LongType }),
        ) {
            NotesScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

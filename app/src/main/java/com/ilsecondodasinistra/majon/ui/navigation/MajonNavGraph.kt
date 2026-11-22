package com.ilsecondodasinistra.majon.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.ui.components.LocalBottomBarHeight
import com.ilsecondodasinistra.majon.ui.components.LocalHazeState
import com.ilsecondodasinistra.majon.ui.components.glass
import com.ilsecondodasinistra.majon.ui.partedit.PartEditScreen
import com.ilsecondodasinistra.majon.ui.projectlinks.ProjectLinksScreen
import com.ilsecondodasinistra.majon.ui.stash.NeedleEditScreen
import com.ilsecondodasinistra.majon.ui.stash.StashScreen
import com.ilsecondodasinistra.majon.ui.stash.SwatchEditScreen
import com.ilsecondodasinistra.majon.ui.stash.YarnDetailScreen
import com.ilsecondodasinistra.majon.ui.stash.YarnEditScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
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
    const val STASH = "stash"
    const val YARN_DETAIL = "yarn/{yarnId}"
    const val YARN_EDIT = "yarn_edit?yarnId={yarnId}"
    const val PROJECT_LINKS = "project_links/{projectId}"
    const val PART_EDIT = "part_edit/{projectId}?partId={partId}"
    const val NEEDLE_EDIT = "needle_edit?needleId={needleId}"
    const val SWATCH_EDIT = "swatch_edit/{yarnId}?swatchId={swatchId}"

    fun editProject(projectId: Long? = null) =
        if (projectId == null) "edit_project?projectId=0" else "edit_project?projectId=$projectId"

    fun projectDetail(projectId: Long) = "project/$projectId"
    fun counter(partId: Long) = "counter/$partId"
    fun notes(partId: Long) = "notes/$partId"
    fun yarnDetail(yarnId: Long) = "yarn/$yarnId"
    fun projectLinks(projectId: Long) = "project_links/$projectId"
    fun partEdit(projectId: Long, partId: Long? = null) = "part_edit/$projectId?partId=${partId ?: 0}"
    fun needleEdit(needleId: Long? = null) = "needle_edit?needleId=${needleId ?: 0}"
    fun swatchEdit(yarnId: Long, swatchId: Long? = null) = "swatch_edit/$yarnId?swatchId=${swatchId ?: 0}"
    fun yarnEdit(yarnId: Long? = null) =
        if (yarnId == null) "yarn_edit?yarnId=0" else "yarn_edit?yarnId=$yarnId"
}

/** The two destinations of the bottom bar. */
private val TAB_ROUTES = listOf(Routes.HOME, Routes.STASH)

private fun NavBackStackEntry.isTab() = destination.route in TAB_ROUTES

/** Switching between bottom-bar tabs fades instead of sliding. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs() =
    initialState.isTab() && targetState.isTab()

@Composable
fun MajonNavGraph(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in TAB_ROUTES
    val haze = rememberHazeState()
    val density = LocalDensity.current
    var barHeight by remember { mutableStateOf(0.dp) }

    CompositionLocalProvider(
        LocalHazeState provides haze,
        LocalBottomBarHeight provides (if (showBar) barHeight else 0.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            MajonNavHost(navController, Modifier.fillMaxSize().hazeSource(haze))
            AnimatedVisibility(
                visible = showBar,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
            ) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
                        .glass(haze),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        selected = route == Routes.HOME,
                        onClick = {
                            if (route != Routes.HOME) navController.popBackStack(Routes.HOME, inclusive = false)
                        },
                        icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_projects)) },
                    )
                    NavigationBarItem(
                        selected = route == Routes.STASH,
                        onClick = {
                            if (route != Routes.STASH) {
                                navController.navigate(Routes.STASH) { launchSingleTop = true }
                            }
                        },
                        icon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_stash)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MajonNavHost(navController: NavHostController, modifier: Modifier) {
    NavHost(
        navController = navController,
        modifier = modifier,
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
        composable(
            Routes.HOME,
            enterTransition = {
                if (betweenTabs()) fadeIn(tween(300)) else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) + fadeIn()
            },
            exitTransition = {
                if (betweenTabs()) fadeOut(tween(200)) else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) + fadeOut()
            },
            popEnterTransition = {
                if (betweenTabs()) fadeIn(tween(300)) else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) + fadeIn()
            },
            popExitTransition = {
                if (betweenTabs()) fadeOut(tween(200)) else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) + fadeOut()
            },
        ) {
            HomeScreen(
                onProjectClick = { navController.navigate(Routes.projectDetail(it)) },
                onAddProject = { navController.navigate(Routes.editProject()) },
                onEditProject = { navController.navigate(Routes.editProject(it)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            Routes.STASH,
            enterTransition = { if (betweenTabs()) fadeIn(tween(300)) else fadeIn() },
            exitTransition = { if (betweenTabs()) fadeOut(tween(200)) else fadeOut() },
            popEnterTransition = { if (betweenTabs()) fadeIn(tween(300)) else fadeIn() },
            popExitTransition = { if (betweenTabs()) fadeOut(tween(200)) else fadeOut() },
        ) {
            StashScreen(
                onYarnClick = { navController.navigate(Routes.yarnDetail(it)) },
                onAddYarn = { navController.navigate(Routes.yarnEdit()) },
                onEditNeedle = { navController.navigate(Routes.needleEdit(it)) },
            )
        }

        composable(
            Routes.YARN_DETAIL,
            arguments = listOf(navArgument("yarnId") { type = NavType.LongType }),
        ) {
            YarnDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.yarnEdit(it)) },
                onEditSwatch = { yarnId, swatchId -> navController.navigate(Routes.swatchEdit(yarnId, swatchId)) },
            )
        }

        composable(
            Routes.NEEDLE_EDIT,
            arguments = listOf(navArgument("needleId") { type = NavType.LongType; defaultValue = 0L }),
        ) {
            NeedleEditScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.SWATCH_EDIT,
            arguments = listOf(
                navArgument("yarnId") { type = NavType.LongType },
                navArgument("swatchId") { type = NavType.LongType; defaultValue = 0L },
            ),
        ) {
            SwatchEditScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.PROJECT_LINKS,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) {
            ProjectLinksScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.PART_EDIT,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("partId") { type = NavType.LongType; defaultValue = 0L },
            ),
        ) {
            PartEditScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.YARN_EDIT,
            arguments = listOf(
                navArgument("yarnId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
        ) {
            YarnEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
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
                onSaved = { id, isNew ->
                    if (isNew) {
                        // A new project opens on its detail, where parts, yarn and needles are added next
                        navController.navigate(Routes.projectDetail(id)) {
                            popUpTo(Routes.EDIT_PROJECT) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }

        composable(
            Routes.PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val detailProjectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ProjectDetailScreen(
                onBack = { navController.popBackStack() },
                onEditPart = { navController.navigate(Routes.partEdit(detailProjectId, it)) },
                onEditLinks = { navController.navigate(Routes.projectLinks(detailProjectId)) },
                onPartClick = { navController.navigate(Routes.counter(it)) },
                onPartNotes = { navController.navigate(Routes.notes(it)) },
                onEditProject = { navController.navigate(Routes.editProject(it)) },
                onDuplicated = { navController.navigate(Routes.editProject(it)) },
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
                onNextPart = { nextId ->
                    // Replace this counter with the next part's, so Back still returns to the project
                    navController.navigate(Routes.counter(nextId)) {
                        popUpTo(Routes.COUNTER) { inclusive = true }
                    }
                },
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

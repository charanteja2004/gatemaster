package com.gatemaster.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gatemaster.app.core.model.ContentType
import com.gatemaster.app.core.model.PracticeSpec
import com.gatemaster.app.navigation.BranchPickerRoute
import com.gatemaster.app.navigation.HomeRoute
import com.gatemaster.app.navigation.PapersRoute
import com.gatemaster.app.navigation.ReaderRoute
import com.gatemaster.app.navigation.SearchRoute
import com.gatemaster.app.navigation.SettingsRoute
import com.gatemaster.app.navigation.SubjectRoute
import com.gatemaster.app.navigation.SubjectsRoute
import com.gatemaster.app.navigation.TestListRoute
import com.gatemaster.app.navigation.TestPlayerRoute
import com.gatemaster.app.ui.branch.BranchPickerScreen
import com.gatemaster.app.ui.home.HomeScreen
import com.gatemaster.app.ui.papers.PapersScreen
import com.gatemaster.app.ui.reader.ReaderScreen
import com.gatemaster.app.ui.search.SearchScreen
import com.gatemaster.app.ui.settings.SettingsScreen
import com.gatemaster.app.ui.subject.OpenRequest
import com.gatemaster.app.ui.subject.SubjectScreen
import com.gatemaster.app.ui.subject.SubjectsScreen
import com.gatemaster.app.ui.test.TestListScreen
import com.gatemaster.app.ui.test.TestPlayerScreen

/** The four places the bottom bar can take you. */
private enum class TopLevel(
    val label: String,
    val icon: ImageVector,
    val route: Any,
) {
    HOME("Home", Icons.Filled.Home, HomeRoute),
    SUBJECTS("Study", Icons.AutoMirrored.Filled.MenuBook, SubjectsRoute),
    TESTS("Tests", Icons.Filled.EditNote, TestListRoute),
    SETTINGS("Settings", Icons.Filled.Settings, SettingsRoute),
}

private fun NavDestination.matches(level: TopLevel): Boolean = when (level) {
    TopLevel.HOME -> hasRoute(HomeRoute::class)
    TopLevel.SUBJECTS -> hasRoute(SubjectsRoute::class)
    TopLevel.TESTS -> hasRoute(TestListRoute::class)
    TopLevel.SETTINGS -> hasRoute(SettingsRoute::class)
}

@Composable
fun GateMasterApp(
    startOnBranchPicker: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val current = TopLevel.entries.firstOrNull { destination?.matches(it) == true }

    fun openDocument(request: OpenRequest) {
        navController.navigate(
            ReaderRoute(
                title = request.title,
                subtitle = request.subtitle,
                path = request.ref.path,
                isPdf = request.ref.type == ContentType.PDF,
                subjectId = request.subjectId,
                topicId = request.topicId,
            ),
        )
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            // The bar shows only on the four top-level destinations. Reading an
            // article or sitting a test stays full-screen.
            if (current != null) {
                NavigationBar {
                    TopLevel.entries.forEach { level ->
                        NavigationBarItem(
                            selected = level == current,
                            onClick = {
                                if (level != current) {
                                    navController.navigate(level.route) {
                                        popUpTo(HomeRoute) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(level.icon, contentDescription = null) },
                            label = { Text(level.label) },
                        )
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startOnBranchPicker) {
                BranchPickerRoute(firstRun = true)
            } else {
                HomeRoute
            },
            modifier = Modifier.padding(scaffoldPadding),
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 5 }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 8 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 8 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 5 }) + fadeOut() },
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onSubjectClick = { navController.navigate(SubjectRoute(it)) },
                    onPapersClick = { navController.navigate(PapersRoute) },
                    onTestsClick = { navController.navigate(TestListRoute) },
                    onSearchClick = { navController.navigate(SearchRoute) },
                    onChangeBranch = { navController.navigate(BranchPickerRoute()) },
                    onSeeAllSubjects = { navController.navigate(SubjectsRoute) },
                    onResume = { entry ->
                        navController.navigate(
                            ReaderRoute(
                                title = entry.title,
                                subtitle = entry.subjectName,
                                path = entry.path,
                                isPdf = entry.isPdf,
                                subjectId = entry.subjectId,
                                topicId = entry.topicId,
                            ),
                        )
                    },
                )
            }

            composable<SubjectsRoute> {
                SubjectsScreen(
                    onSubjectClick = { navController.navigate(SubjectRoute(it)) },
                    onSearchClick = { navController.navigate(SearchRoute) },
                )
            }

            composable<SettingsRoute> {
                SettingsScreen(
                    onChangeBranch = { navController.navigate(BranchPickerRoute()) },
                )
            }

            composable<BranchPickerRoute> { entry ->
                val route = entry.toRoute<BranchPickerRoute>()
                BranchPickerScreen(
                    showBack = !route.firstRun,
                    onDone = {
                        if (route.firstRun) {
                            navController.navigate(HomeRoute) {
                                popUpTo<BranchPickerRoute> { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }

            composable<SubjectRoute> {
                SubjectScreen(
                    onBack = navController::popBackStack,
                    onOpen = ::openDocument,
                    onPractise = { subjectId, topicId ->
                        // The id encodes the spec, so a generated paper reaches
                        // the player through exactly the same route as a
                        // bundled one -- no second player, no second ViewModel.
                        navController.navigate(
                            TestPlayerRoute(
                                testId = if (topicId == null) {
                                    PracticeSpec.subject(subjectId).id
                                } else {
                                    PracticeSpec.topic(subjectId, topicId).id
                                },
                            ),
                        )
                    },
                )
            }

            composable<PapersRoute> {
                PapersScreen(
                    onBack = navController::popBackStack,
                    onOpen = ::openDocument,
                )
            }

            composable<TestListRoute> {
                TestListScreen(
                    onBack = navController::popBackStack,
                    onStartTest = { testId, restart ->
                        navController.navigate(TestPlayerRoute(testId, restart))
                    },
                    onPractise = { subjectId ->
                        navController.navigate(
                            TestPlayerRoute(testId = PracticeSpec.subject(subjectId).id),
                        )
                    },
                )
            }

            composable<TestPlayerRoute> {
                TestPlayerScreen(onExit = navController::popBackStack)
            }

            composable<SearchRoute> {
                SearchScreen(
                    onBack = navController::popBackStack,
                    onOpen = ::openDocument,
                )
            }

            composable<ReaderRoute> { entry ->
                val route = entry.toRoute<ReaderRoute>()
                ReaderScreen(
                    title = route.title,
                    subtitle = route.subtitle,
                    assetPath = route.path,
                    isPdf = route.isPdf,
                    isDarkTheme = isDarkTheme,
                    onBack = navController::popBackStack,
                    onOpenTopic = { topic ->
                        // Replace rather than stack, so back returns to the
                        // topic list instead of walking every article read.
                        navController.navigate(
                            ReaderRoute(
                                title = topic.title,
                                subtitle = route.subtitle,
                                path = topic.content.path,
                                isPdf = topic.content.type == ContentType.PDF,
                                subjectId = route.subjectId,
                                topicId = topic.id,
                            ),
                        ) {
                            popUpTo<ReaderRoute> { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}

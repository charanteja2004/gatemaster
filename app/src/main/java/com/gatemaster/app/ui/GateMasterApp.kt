package com.gatemaster.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gatemaster.app.core.model.ContentType
import com.gatemaster.app.navigation.BranchPickerRoute
import com.gatemaster.app.navigation.HomeRoute
import com.gatemaster.app.navigation.PapersRoute
import com.gatemaster.app.navigation.ReaderRoute
import com.gatemaster.app.navigation.SearchRoute
import com.gatemaster.app.navigation.SubjectRoute
import com.gatemaster.app.navigation.TestListRoute
import com.gatemaster.app.navigation.TestPlayerRoute
import com.gatemaster.app.ui.branch.BranchPickerScreen
import com.gatemaster.app.ui.home.HomeScreen
import com.gatemaster.app.ui.papers.PapersScreen
import com.gatemaster.app.ui.reader.ReaderScreen
import com.gatemaster.app.ui.search.SearchScreen
import com.gatemaster.app.ui.subject.OpenRequest
import com.gatemaster.app.ui.subject.SubjectScreen
import com.gatemaster.app.ui.test.TestListScreen
import com.gatemaster.app.ui.test.TestPlayerScreen

@Composable
fun GateMasterApp(
    startOnBranchPicker: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
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

    NavHost(
        navController = navController,
        startDestination = if (startOnBranchPicker) {
            BranchPickerRoute(firstRun = true)
        } else {
            HomeRoute
        },
        modifier = modifier,
        enterTransition = { slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 6 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 6 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut() },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onSubjectClick = { navController.navigate(SubjectRoute(it)) },
                onPapersClick = { navController.navigate(PapersRoute) },
                onTestsClick = { navController.navigate(TestListRoute) },
                onSearchClick = { navController.navigate(SearchRoute) },
                onChangeBranch = { navController.navigate(BranchPickerRoute()) },
            )
        }

        composable<BranchPickerRoute> { entry ->
            val route = entry.toRoute<BranchPickerRoute>()
            BranchPickerScreen(
                showBack = !route.firstRun,
                onDone = {
                    if (route.firstRun) {
                        // Replace the picker so back does not return to it.
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
                onBack = navController::popBackStack,
                onOpenTopic = { topic ->
                    // Replace rather than stack, so back returns to the topic
                    // list instead of walking every article already read.
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

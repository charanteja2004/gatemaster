package com.gatemaster.app.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gatemaster.app.core.model.ContentType
import com.gatemaster.app.navigation.HomeRoute
import com.gatemaster.app.navigation.PapersRoute
import com.gatemaster.app.navigation.ReaderRoute
import com.gatemaster.app.navigation.SearchRoute
import com.gatemaster.app.navigation.SubjectRoute
import com.gatemaster.app.navigation.TestListRoute
import com.gatemaster.app.navigation.TestPlayerRoute
import com.gatemaster.app.ui.home.HomeScreen
import com.gatemaster.app.ui.papers.PapersScreen
import com.gatemaster.app.ui.reader.ReaderScreen
import com.gatemaster.app.ui.search.SearchScreen
import com.gatemaster.app.ui.subject.OpenRequest
import com.gatemaster.app.ui.subject.SubjectScreen
import com.gatemaster.app.ui.test.TestListScreen
import com.gatemaster.app.ui.test.TestPlayerScreen

private const val TRANSITION_MS = 220

@Composable
fun GateMasterApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    fun openDocument(request: OpenRequest) {
        navController.navigate(
            ReaderRoute(
                title = request.title,
                subtitle = request.subtitle,
                path = request.ref.path,
                isPdf = request.ref.type == ContentType.PDF,
            ),
        )
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it / 4 }) + androidx.compose.animation.fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 6 }) + androidx.compose.animation.fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 6 }) + androidx.compose.animation.fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it / 4 }) + androidx.compose.animation.fadeOut()
        },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onSubjectClick = { navController.navigate(SubjectRoute(it)) },
                onPapersClick = { navController.navigate(PapersRoute) },
                onTestsClick = { navController.navigate(TestListRoute) },
                onSearchClick = { navController.navigate(SearchRoute) },
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
            )
        }
    }
}

package net.focustation.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import net.focustation.myapplication.session.SelectedSessionPlace
import net.focustation.myapplication.session.SessionPlaceSelectionStore
import net.focustation.myapplication.ui.screen.dashboard.DashboardScreen
import net.focustation.myapplication.ui.screen.login.LoginScreen
import net.focustation.myapplication.ui.screen.onboarding.OnboardingScreen
import net.focustation.myapplication.ui.screen.onboarding.OnboardingSetupScreen
import net.focustation.myapplication.ui.screen.onboarding.OnboardingStore
import net.focustation.myapplication.ui.screen.report.SessionReportDetailScreen
import net.focustation.myapplication.ui.screen.report.SessionReportScreen
import net.focustation.myapplication.ui.screen.session.EnvironmentSessionScreen
import net.focustation.myapplication.ui.screen.session.FeedbackSessionScreen
import net.focustation.myapplication.ui.screen.session.FocusSessionScreen
import net.focustation.myapplication.ui.screen.session.PlaceSelectionScreen
import net.focustation.myapplication.ui.screen.session.StartSessionPlaceSheet
import net.focustation.myapplication.ui.screen.settings.SettingsScreen
import net.focustation.myapplication.ui.screen.space.SpaceHistoryScreen
import net.focustation.myapplication.ui.screen.survey.SurveyScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    val reportTabRoute = NavRoute.SessionReport.route
    val context = LocalContext.current
    val onboardingStore = remember(context) { OnboardingStore(context) }

    NavHost(
        navController = navController,
        startDestination =
            if (onboardingStore.isCompleted()) {
                NavRoute.Dashboard.route
            } else {
                NavRoute.Onboarding.route
            },
    ) {
        composable(NavRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoute.OnboardingSetup.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoute.Onboarding.route) {
            OnboardingScreen(
                onLoginClick = {
                    navController.navigate(NavRoute.Login.route)
                },
            )
        }

        composable(NavRoute.OnboardingSetup.route) {
            OnboardingSetupScreen(
                onStartSurvey = {
                    onboardingStore.markCompleted()
                    navController.navigate(NavRoute.Survey.route) {
                        popUpTo(NavRoute.Onboarding.route) { inclusive = true }
                    }
                },
                onExploreFirst = {
                    onboardingStore.markCompleted()
                    navController.navigate(NavRoute.Dashboard.route) {
                        popUpTo(NavRoute.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoute.Survey.route) {
            SurveyScreen(
                onComplete = {
                    navController.navigate(NavRoute.Dashboard.route) {
                        popUpTo(NavRoute.Survey.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(NavRoute.Dashboard.route) {
            DashboardScreen(
                onStartSession = { navController.navigate(NavRoute.EnvironmentSession.route) },
                onNavigateToReport = { navController.navigateToMainTab(reportTabRoute) },
                onNavigateToSpaceHistory = { navController.navigateToMainTab(NavRoute.SpaceHistory.route) },
                onNavigateToSettings = { navController.navigateToMainTab(NavRoute.Settings.route) },
                onRetakeSurvey = { navController.navigate(NavRoute.Survey.route) },
            )
        }

        composable(NavRoute.EnvironmentSession.route) {
            EnvironmentSessionScreen(
                onSessionComplete = { navController.navigate(NavRoute.StartSessionPlace.route) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(NavRoute.StartSessionPlace.route) {
            StartSessionPlaceSheet(
                onSelectRecent = { place ->
                    SessionPlaceSelectionStore.save(
                        SelectedSessionPlace(
                            name = place.name,
                            latitude = place.latitude,
                            longitude = place.longitude,
                        ),
                    )
                    navController.navigate(NavRoute.FocusSession.route) {
                        popUpTo(NavRoute.StartSessionPlace.route) { inclusive = true }
                    }
                },
                onPickManually = { navController.navigate(NavRoute.PlaceSelection.route) },
                onSkip = {
                    SessionPlaceSelectionStore.clear()
                    navController.navigate(NavRoute.FocusSession.route) {
                        popUpTo(NavRoute.StartSessionPlace.route) { inclusive = true }
                    }
                },
                onDismiss = { navController.popBackStack() },
            )
        }

        composable(NavRoute.PlaceSelection.route) {
            PlaceSelectionScreen(
                onPlaceSelected = {
                    navController.navigate(NavRoute.FocusSession.route) {
                        popUpTo(NavRoute.StartSessionPlace.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(NavRoute.FocusSession.route) {
            FocusSessionScreen(
                onSessionEnd = { navController.navigate(NavRoute.FeedbackSession.route) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(NavRoute.FeedbackSession.route) {
            FeedbackSessionScreen(
                onSubmit = {
                    navController.navigateHomeAfterSession()
                },
            )
        }

        composable(NavRoute.SessionReport.route) {
            SessionReportScreen(
                onHistoryItemClick = { sessionId ->
                    navController.navigate(NavRoute.SessionReportDetail.createRoute(sessionId))
                },
                onNavigateToHome = {
                    navController.navigateToMainTab(NavRoute.Dashboard.route)
                },
                onNavigateToSpaceHistory = {
                    navController.navigateToMainTab(NavRoute.SpaceHistory.route)
                },
                onNavigateToSettings = {
                    navController.navigateToMainTab(NavRoute.Settings.route)
                },
            )
        }

        composable(
            route = NavRoute.SessionReportDetail.route,
            arguments =
                listOf(
                    navArgument(NavRoute.SessionReportDetail.ARG_SESSION_ID) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val sessionId =
                backStackEntry.arguments?.getString(NavRoute.SessionReportDetail.ARG_SESSION_ID).orEmpty()
            SessionReportDetailScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(NavRoute.SpaceHistory.route) {
            SpaceHistoryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigateToMainTab(NavRoute.Dashboard.route)
                },
                onNavigateToReport = {
                    navController.navigateToMainTab(reportTabRoute)
                },
                onNavigateToSettings = {
                    navController.navigateToMainTab(NavRoute.Settings.route)
                },
            )
        }

        composable(NavRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigateToMainTab(NavRoute.Dashboard.route)
                },
                onNavigateToReport = {
                    navController.navigateToMainTab(reportTabRoute)
                },
                onNavigateToSpaceHistory = {
                    navController.navigateToMainTab(NavRoute.SpaceHistory.route)
                },
            )
        }
    }
}

private fun NavHostController.navigateToMainTab(route: String) {
    navigate(route) {
        // 하단 탭은 동일 목적지 중복을 막고 상태를 최대한 복원한다.
        launchSingleTop = true
        restoreState = true
        popUpTo(NavRoute.Dashboard.route) {
            saveState = true
        }
    }
}

private fun NavHostController.navigateHomeAfterSession() {
    navigate(NavRoute.Dashboard.route) {
        popUpTo(NavRoute.Dashboard.route)
        launchSingleTop = true
    }
}

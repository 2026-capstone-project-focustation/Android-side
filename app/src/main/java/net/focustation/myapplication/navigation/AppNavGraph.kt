package net.focustation.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import net.focustation.myapplication.ui.screen.dashboard.DashboardScreen
import net.focustation.myapplication.ui.screen.login.LoginScreen
import net.focustation.myapplication.ui.screen.onboarding.OnboardingScreen
import net.focustation.myapplication.ui.screen.report.SessionReportDetailScreen
import net.focustation.myapplication.ui.screen.report.SessionReportScreen
import net.focustation.myapplication.ui.screen.session.EnvironmentSessionScreen
import net.focustation.myapplication.ui.screen.session.FeedbackSessionScreen
import net.focustation.myapplication.ui.screen.session.FocusSessionScreen
import net.focustation.myapplication.ui.screen.settings.SettingsScreen
import net.focustation.myapplication.ui.screen.space.SpaceHistoryScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.Login.route,
    ) {
        composable(NavRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoute.Onboarding.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoute.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(NavRoute.Dashboard.route) {
                        popUpTo(NavRoute.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoute.Dashboard.route) {
            DashboardScreen(
                onStartSession = { navController.navigate(NavRoute.EnvironmentSession.route) },
                onNavigateToReport = { navController.navigate(NavRoute.SessionReport.createRoute(false)) },
                onNavigateToSpaceHistory = { navController.navigate(NavRoute.SpaceHistory.route) },
                onNavigateToSettings = { navController.navigate(NavRoute.Settings.route) },
            )
        }

        composable(NavRoute.EnvironmentSession.route) {
            EnvironmentSessionScreen(
                onSessionComplete = { navController.navigate(NavRoute.FocusSession.route) },
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
                    navController.navigate(NavRoute.SessionReport.createRoute(true)) {
                        popUpTo(NavRoute.Dashboard.route)
                    }
                },
                onSkip = {
                    navController.navigate(NavRoute.SessionReport.createRoute(true)) {
                        popUpTo(NavRoute.Dashboard.route)
                    }
                },
            )
        }

        composable(
            route = NavRoute.SessionReport.route,
            arguments =
                listOf(
                    navArgument(NavRoute.SessionReport.ARG_FROM_SESSION) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
        ) { backStackEntry ->
            val isFromActiveSession =
                backStackEntry.arguments?.getBoolean(NavRoute.SessionReport.ARG_FROM_SESSION) ?: false
            SessionReportScreen(
                isFromActiveSession = isFromActiveSession,
                onHistoryItemClick = { sessionId ->
                    navController.navigate(NavRoute.SessionReportDetail.createRoute(sessionId))
                },
                onBack = {
                    navController.navigate(NavRoute.Dashboard.route) {
                        popUpTo(NavRoute.Dashboard.route) { inclusive = true }
                    }
                },
                onRetry = { navController.navigate(NavRoute.EnvironmentSession.route) },
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
            )
        }

        composable(NavRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

package net.focustation.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import net.focustation.myapplication.ui.screen.dashboard.DashboardScreen
import net.focustation.myapplication.ui.screen.login.LoginScreen
import net.focustation.myapplication.ui.screen.onboarding.OnboardingScreen
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
                onNavigateToReport = { navController.navigate(NavRoute.SessionReport.route) },
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
                    navController.navigate(NavRoute.SessionReport.route) {
                        popUpTo(NavRoute.Dashboard.route)
                    }
                },
                onSkip = {
                    navController.navigate(NavRoute.SessionReport.route) {
                        popUpTo(NavRoute.Dashboard.route)
                    }
                },
            )
        }

        composable(NavRoute.SessionReport.route) {
            SessionReportScreen(
                onBack = {
                    navController.navigate(NavRoute.Dashboard.route) {
                        popUpTo(NavRoute.Dashboard.route) { inclusive = true }
                    }
                },
                onRetry = { navController.navigate(NavRoute.EnvironmentSession.route) },
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

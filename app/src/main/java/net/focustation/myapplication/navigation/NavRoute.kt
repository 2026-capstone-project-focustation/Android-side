package net.focustation.myapplication.navigation

sealed class NavRoute(
    val route: String,
) {
    // Auth
    data object Login : NavRoute("login")

    // Onboarding
    data object Onboarding : NavRoute("onboarding")

    // Main
    data object Dashboard : NavRoute("dashboard")

    // Session
    data object EnvironmentSession : NavRoute("environment_session")

    data object FocusSession : NavRoute("focus_session")

    data object FeedbackSession : NavRoute("feedback_session")

    // Report
    data object SessionReport : NavRoute("session_report?fromSession={fromSession}") {
        const val ARG_FROM_SESSION = "fromSession"

        fun createRoute(fromSession: Boolean): String =
            "session_report?fromSession=$fromSession"
    }

    data object SessionReportDetail : NavRoute("session_report_detail/{sessionId}") {
        const val ARG_SESSION_ID = "sessionId"

        fun createRoute(sessionId: String): String =
            "session_report_detail/$sessionId"
    }

    // Map / History
    data object SpaceHistory : NavRoute("space_history")

    // Settings
    data object Settings : NavRoute("settings")
}

package com.gorman.ourmemoryapp.ui.navigation.ui

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.gorman.ourmemoryapp.domain.models.Screen
import com.gorman.ourmemoryapp.ui.common.models.VeteranLink
import com.gorman.ourmemoryapp.ui.details.ui.DetailsScreen
import com.gorman.ourmemoryapp.ui.details.viewmodels.DetailsViewModel
import com.gorman.ourmemoryapp.ui.feedback.ui.FeedbackScreen
import com.gorman.ourmemoryapp.ui.scan.ui.ScannerScreen
import com.gorman.ourmemoryapp.ui.submission.ui.SubmissionScreen

fun NavGraphBuilder.veteranGraph(navController: NavHostController) {
    composable(
        route = "${Screen.DetailScreen.route}/{veteranId}",
        deepLinks = listOf(navDeepLink { uriPattern = "${VeteranLink.BASE_URL}/{veteranId}" })
    ) {
        val veteranId = it.arguments?.getString("veteranId")
        val detailsViewModel = hiltViewModel<DetailsViewModel, DetailsViewModel.Factory>(
            creationCallback = { factory -> factory.create(veteranId.orEmpty()) }
        )
        DetailsScreen(
            detailsViewModel = detailsViewModel,
            onBackClick = { navController.popBackStack() },
            onShowOnMapClick = { burialId ->
                navController.navigate(Screen.BurialMapScreen.withBurial(burialId))
            },
            onAddToHistoryClick = {
                navController.navigate(Screen.SubmissionScreen.forVeteran(veteranId.orEmpty()))
            },
            onReportErrorClick = {
                navController.navigate(Screen.FeedbackScreen.forVeteran(veteranId.orEmpty()))
            }
        )
    }
    composable(Screen.ScannerScreen.route) {
        ScannerScreen(
            onBackClick = { navController.popBackStack() },
            onVeteranOpen = { veteranId ->
                navController.popBackStack()
                navController.navigate("${Screen.DetailScreen.route}/$veteranId")
            }
        )
    }
    composable(
        route = Screen.SubmissionScreen.pattern,
        arguments = listOf(navArgument(Screen.SubmissionScreen.VETERAN_ID_ARG) { type = NavType.StringType })
    ) {
        SubmissionScreen(onBackClick = { navController.popBackStack() })
    }
    composable(
        route = Screen.FeedbackScreen.pattern,
        arguments = listOf(
            navArgument(Screen.FeedbackScreen.VETERAN_ID_ARG) {
                type = NavType.StringType
                defaultValue = ""
            }
        )
    ) {
        FeedbackScreen(onBackClick = { navController.popBackStack() })
    }
}

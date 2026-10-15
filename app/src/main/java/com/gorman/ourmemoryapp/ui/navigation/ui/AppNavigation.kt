package com.gorman.ourmemoryapp.ui.navigation.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.util.Consumer
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gorman.ourmemoryapp.domain.models.Screen
import com.gorman.ourmemoryapp.ui.favorites.ui.FavoritesScreen
import com.gorman.ourmemoryapp.ui.home.ui.MainScreen
import com.gorman.ourmemoryapp.ui.info.ui.InfoScreen
import com.gorman.ourmemoryapp.ui.intro.ui.IntroScreen
import com.gorman.ourmemoryapp.ui.map.ui.MapScreen
import com.gorman.ourmemoryapp.ui.more.ui.MoreScreen
import com.gorman.ourmemoryapp.ui.myrequests.ui.MyRequestsScreen
import com.gorman.ourmemoryapp.ui.navigation.models.TopLevelTab
import com.gorman.ourmemoryapp.ui.navigation.viewmodels.SessionViewModel
import com.gorman.ourmemoryapp.ui.tours.ui.TourScreen
import kotlinx.collections.immutable.toPersistentList

@Composable
fun AppNavigation(
    showIntro: Boolean,
    onIntroStartClick: () -> Unit,
    onChangeLangClick: (String) -> Unit
) {
    val navController = rememberNavController()
    val startDestination = remember { if (showIntro) Screen.IntroScreen.route else Screen.HomeScreen.route }
    val activity = LocalActivity.current as? ComponentActivity

    DisposableEffect(navController, activity) {
        val listener = Consumer<Intent> { navController.handleDeepLink(it) }
        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }

    val sessionViewModel: SessionViewModel = hiltViewModel()
    val isAdmin by sessionViewModel.isAdmin.collectAsStateWithLifecycle()
    val tabs = remember(isAdmin) {
        TopLevelTab.entries.filter { it != TopLevelTab.ADMIN || isAdmin }.toPersistentList()
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = TopLevelTab.entries.firstOrNull { it.screen.route == backStackEntry?.destination?.route }
    Box(modifier = Modifier.fillMaxSize()) {
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            onIntroStartClick = onIntroStartClick,
            isAdmin = isAdmin,
            onChangeLangClick = onChangeLangClick,
            modifier = Modifier.fillMaxSize()
        )
        if (currentTab != null) {
            AppBottomBar(
                tabs = tabs,
                selectedTab = currentTab,
                onTabClick = { navController.navigateToTab(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    onIntroStartClick: () -> Unit,
    isAdmin: Boolean,
    onChangeLangClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val openVeteran: (String) -> Unit = { veteranId ->
        navController.navigate("${Screen.DetailScreen.route}/$veteranId")
    }
    val openTour: (String) -> Unit = { tourId ->
        navController.navigate(Screen.TourScreen.withTour(tourId))
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.IntroScreen.route) {
            IntroScreen {
                onIntroStartClick()
                navController.navigate(Screen.HomeScreen.route) {
                    popUpTo(Screen.IntroScreen.route) { inclusive = true }
                }
            }
        }
        tabComposable(Screen.HomeScreen.route, hasStatusBarScrim = true) {
            MainScreen(
                onItemClick = openVeteran,
                onScanClick = { navController.navigate(Screen.ScannerScreen.route) }
            )
        }
        tabComposable(Screen.MapScreen.route) {
            MapScreen(onBackClick = null, onVeteranClick = openVeteran, onTourClick = openTour)
        }
        tabComposable(Screen.InfoScreen.route) {
            InfoScreen(
                onOpenMapClick = { navController.navigateToTab(TopLevelTab.MAP) },
                onChangeLangClick = onChangeLangClick
            )
        }
        tabComposable(Screen.MoreScreen.route, hasStatusBarScrim = true) {
            MoreScreen(
                onLanguageChange = onChangeLangClick,
                onWriteToUsClick = { navController.navigate(Screen.FeedbackScreen.route) },
                onMyRequestsClick = { navController.navigate(Screen.MyRequestsScreen.route) },
                onFavoritesClick = { navController.navigate(Screen.FavoritesScreen.route) },
                onScanClick = { navController.navigate(Screen.ScannerScreen.route) },
                onAdminClick = {
                    if (isAdmin) {
                        navController.navigateToTab(TopLevelTab.ADMIN)
                    } else {
                        navController.navigate(Screen.AdminLoginScreen.route)
                    }
                }
            )
        }
        composable(
            route = Screen.BurialMapScreen.pattern,
            arguments = listOf(navArgument(Screen.BurialMapScreen.BURIAL_ID_ARG) { type = NavType.StringType })
        ) {
            MapScreen(
                onBackClick = { navController.popBackStack() },
                onVeteranClick = openVeteran,
                onTourClick = openTour
            )
        }
        composable(
            route = Screen.TourScreen.pattern,
            arguments = listOf(navArgument(Screen.TourScreen.TOUR_ID_ARG) { type = NavType.StringType })
        ) {
            TourScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.MyRequestsScreen.route) {
            MyRequestsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.FavoritesScreen.route) {
            FavoritesScreen(onBackClick = { navController.popBackStack() }, onVeteranClick = openVeteran)
        }
        veteranGraph(navController)
        adminGraph(navController)
    }
}

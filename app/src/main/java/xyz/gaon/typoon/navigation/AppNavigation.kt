@file:Suppress("LongMethod", "MaxLineLength")

package xyz.gaon.typoon.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import xyz.gaon.typoon.R
import xyz.gaon.typoon.feature.dictionary.DictionaryScreen
import xyz.gaon.typoon.feature.history.HistoryScreen
import xyz.gaon.typoon.feature.home.HomeScreen
import xyz.gaon.typoon.feature.onboarding.OnboardingRoute
import xyz.gaon.typoon.feature.result.ResultScreen
import xyz.gaon.typoon.feature.settings.SettingsScreen
import xyz.gaon.typoon.feature.settings.SettingsViewModel
import xyz.gaon.typoon.feature.settings.sub.AboutSettingsScreen
import xyz.gaon.typoon.feature.settings.sub.AutoConvertClipboardSettingsScreen
import xyz.gaon.typoon.feature.settings.sub.AutoReadClipboardSettingsScreen
import xyz.gaon.typoon.feature.settings.sub.ContributorsScreen
import xyz.gaon.typoon.feature.settings.sub.DonorsScreen
import xyz.gaon.typoon.feature.settings.sub.EasterEggScreen
import xyz.gaon.typoon.feature.settings.sub.GeneralSettingsScreen
import xyz.gaon.typoon.feature.settings.sub.HapticSettingsScreen
import xyz.gaon.typoon.feature.settings.sub.LanguageSettingsScreen
import xyz.gaon.typoon.feature.settings.sub.OpenSourceLicensesScreen
import xyz.gaon.typoon.feature.settings.sub.PrivacySettingsScreen
import xyz.gaon.typoon.feature.settings.sub.SaveHistorySettingsScreen
import xyz.gaon.typoon.feature.settings.sub.ThemeSettingsScreen
import xyz.gaon.typoon.feature.splash.SplashRoute
import xyz.gaon.typoon.ui.components.AdBannerView

@Composable
fun AppNavigation(shortcutClipboardToken: Int = 0) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val activity = context as? Activity
    val exitToastMessage = stringResource(R.string.nav_exit_toast)
    var lastBackPressedAt by remember { mutableLongStateOf(0L) }

    val bottomNavItems =
        listOf(
            BottomNavItem.Home,
            BottomNavItem.History,
            BottomNavItem.Dictionary,
            BottomNavItem.Settings,
        )
    val bottomNavRoutes = bottomNavItems.map { it.route }

    val showBottomBar = currentDestination?.route in bottomNavRoutes
    val rootContentInsets =
        if (showBottomBar) {
            WindowInsets(0, 0, 0, 0)
        } else {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
        }

    var homeScrollTrigger by remember { mutableIntStateOf(0) }
    var historyScrollTrigger by remember { mutableIntStateOf(0) }
    var dictionaryScrollTrigger by remember { mutableIntStateOf(0) }
    var settingsScrollTrigger by remember { mutableIntStateOf(0) }

    fun navigateToTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(AppRoute.Home.route) {
                inclusive = false
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BackHandler(enabled = currentDestination?.route in bottomNavRoutes) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressedAt < 2_000L) {
            activity?.finish()
        } else {
            lastBackPressedAt = now
            Toast.makeText(context, exitToastMessage, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        contentWindowInsets = rootContentInsets,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    AdBannerView()
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                                label = { Text(stringResource(item.labelRes)) },
                                selected = selected,
                                onClick = {
                                    if (selected) {
                                        when (item.route) {
                                            AppRoute.Home.route -> homeScrollTrigger++
                                            AppRoute.History.route -> historyScrollTrigger++
                                            AppRoute.Dictionary.route -> dictionaryScrollTrigger++
                                            AppRoute.Settings.route -> settingsScrollTrigger++
                                        }
                                    } else {
                                        navigateToTopLevel(item.route)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        LaunchedEffect(shortcutClipboardToken) {
            if (shortcutClipboardToken > 0) {
                navController.navigate(AppRoute.Home.route) {
                    launchSingleTop = true
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = AppRoute.Splash.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
        ) {
            composable(
                AppRoute.Splash.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                SplashRoute(
                    onFinished = { onboardingCompleted ->
                        val destination =
                            if (onboardingCompleted) {
                                AppRoute.Home.route
                            } else {
                                AppRoute.Onboarding.route
                            }
                        navController.navigate(destination) {
                            popUpTo(AppRoute.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(
                AppRoute.Onboarding.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                OnboardingRoute(
                    onFinish = {
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.Onboarding.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(
                AppRoute.Home.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { fadeOut() },
            ) {
                HomeScreen(
                    shortcutClipboardToken = shortcutClipboardToken,
                    scrollToTopTrigger = homeScrollTrigger,
                    onNavigateToResult = {
                        navController.navigate(AppRoute.Result.route)
                    },
                    onNavigateToSettings = {
                        navigateToTopLevel(AppRoute.Settings.route)
                    },
                    onNavigateToHistory = {
                        navigateToTopLevel(AppRoute.History.route)
                    },
                )
            }

            composable(
                AppRoute.History.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { fadeOut() },
            ) {
                HistoryScreen(
                    onNavigateBack = null,
                    scrollToTopTrigger = historyScrollTrigger,
                    onNavigateToResult = {
                        navController.navigate(AppRoute.Result.route)
                    },
                )
            }

            composable(AppRoute.SettingsHistory.route) {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToResult = {
                        navController.navigate(AppRoute.Result.route)
                    },
                )
            }

            composable(AppRoute.Result.route) {
                ResultScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable(
                AppRoute.Settings.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { fadeOut() },
            ) {
                SettingsScreen(
                    scrollToTopTrigger = settingsScrollTrigger,
                    onNavigateBack = {
                        navigateToTopLevel(AppRoute.Home.route)
                    },
                    onResetApp = {
                        navController.navigate(AppRoute.Onboarding.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToTheme = {
                        navController.navigate(AppRoute.SettingsTheme.route)
                    },
                    onNavigateToLanguage = {
                        navController.navigate(AppRoute.SettingsLanguage.route)
                    },
                    onNavigateToGeneral = {
                        navController.navigate(AppRoute.SettingsGeneral.route)
                    },
                    onNavigateToSaveHistory = {
                        navController.navigate(AppRoute.SettingsSaveHistory.route)
                    },
                    onNavigateToAutoReadClipboard = {
                        navController.navigate(AppRoute.SettingsAutoReadClipboard.route)
                    },
                    onNavigateToAutoConvertClipboard = {
                        navController.navigate(AppRoute.SettingsAutoConvertClipboard.route)
                    },
                    onNavigateToHaptic = {
                        navController.navigate(AppRoute.SettingsHaptic.route)
                    },
                    onNavigateToDictionary = {
                        navigateToTopLevel(AppRoute.Dictionary.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(AppRoute.SettingsHistory.route)
                    },
                    onNavigateToPrivacy = {
                        navController.navigate(AppRoute.SettingsPrivacy.route)
                    },
                    onNavigateToAbout = {
                        navController.navigate(AppRoute.SettingsAbout.route)
                    },
                )
            }

            composable(AppRoute.SettingsTheme.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                ThemeSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel,
                )
            }

            composable(AppRoute.SettingsLanguage.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                LanguageSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel,
                )
            }

            composable(AppRoute.SettingsGeneral.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                GeneralSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel,
                )
            }

            composable(AppRoute.SettingsSaveHistory.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SaveHistorySettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel,
                )
            }

            composable(AppRoute.SettingsAutoReadClipboard.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                AutoReadClipboardSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel,
                )
            }

            composable(AppRoute.SettingsAutoConvertClipboard.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                AutoConvertClipboardSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel,
                )
            }

            composable(AppRoute.SettingsHaptic.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                HapticSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel,
                )
            }

            composable(AppRoute.SettingsPrivacy.route) {
                PrivacySettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(AppRoute.SettingsAbout.route) {
                AboutSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToOpenSource = {
                        navController.navigate(AppRoute.SettingsOpenSource.route)
                    },
                    onNavigateToContributors = {
                        navController.navigate(AppRoute.SettingsContributors.route)
                    },
                    onNavigateToDonors = {
                        navController.navigate(AppRoute.SettingsDonors.route)
                    },
                    onNavigateToEasterEgg = {
                        navController.navigate(AppRoute.SettingsEasterEgg.route)
                    },
                )
            }

            composable(AppRoute.SettingsOpenSource.route) {
                OpenSourceLicensesScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(AppRoute.SettingsContributors.route) {
                ContributorsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(AppRoute.SettingsDonors.route) {
                DonorsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(AppRoute.SettingsEasterEgg.route) {
                EasterEggScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(
                AppRoute.Dictionary.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { fadeOut() },
            ) {
                DictionaryScreen(
                    onNavigateBack = null,
                    scrollToTopTrigger = dictionaryScrollTrigger,
                )
            }
        }
    }
}

private sealed class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    data object Home : BottomNavItem(AppRoute.Home.route, R.string.nav_home, Icons.Default.Home)

    data object History : BottomNavItem(AppRoute.History.route, R.string.nav_history, Icons.Default.History)

    data object Dictionary : BottomNavItem(AppRoute.Dictionary.route, R.string.nav_dictionary, Icons.Default.Book)

    data object Settings : BottomNavItem(AppRoute.Settings.route, R.string.nav_settings, Icons.Default.Settings)
}

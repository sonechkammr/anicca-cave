package com.lifeclock.widget.ui

import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifeclock.widget.R
import com.lifeclock.widget.ui.components.Confetti
import com.lifeclock.widget.ui.settings.SettingsScreen
import com.lifeclock.widget.ui.settings.SetupScreen
import com.lifeclock.widget.ui.theme.AniccaTheme
import com.lifeclock.widget.ui.today.TodayScreen
import com.lifeclock.widget.ui.widgets.WidgetsScreen

/** The three top-level screens, in navigation bar order. */
private enum class Dest(val route: String, @param:StringRes val label: Int, val icon: ImageVector) {
    Today("today", R.string.tab_age, Icons.Filled.DateRange),
    Widgets("widgets", R.string.tab_widgets, Icons.Filled.Star),
    Settings("settings", R.string.tab_settings, Icons.Filled.Settings),
}

@Composable
fun AniccaApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val confetti by vm.confetti.collectAsStateWithLifecycle()
    // Kept as State, not unpacked here, so only the screens that show digits recompose on each tick.
    val nowMs = vm.nowMs.collectAsStateWithLifecycle()

    AniccaTheme(state.themeMode) {
        val nav = rememberNavController()
        // Without a birth date the app opens on Setup, with no navigation bar, until one is saved.
        val start = remember { if (state.birth == null) SETUP_ROUTE else Dest.Today.route }
        val backStack by nav.currentBackStackEntryAsState()
        val current = backStack?.destination

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.imePadding(),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    if (current?.route != SETUP_ROUTE) {
                        // The bar has a fixed height and three labels to fit, so its text stops growing at 130 %
                        // while the rest of the app keeps following the system font size.
                        val density = LocalDensity.current
                        CompositionLocalProvider(
                            LocalDensity provides Density(density.density, fontScale = minOf(density.fontScale, 1.3f)),
                        ) {
                            NavigationBar {
                                Dest.entries.forEach { d ->
                                    NavigationBarItem(
                                        selected = current?.hierarchy?.any { it.route == d.route } == true,
                                        onClick = { nav.goTab(d) },
                                        icon = { Icon(d.icon, contentDescription = null) },
                                        label = { Text(stringResource(d.label), maxLines = 1) },
                                    )
                                }
                            }
                        }
                    }
                },
            ) { inner ->
                NavHost(
                    navController = nav,
                    startDestination = start,
                    modifier = Modifier.padding(inner),
                    // The old tab switch was instant; keep it that way.
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                ) {
                    composable(Dest.Today.route) {
                        TodayScreen(state.birth, nowMs) { nav.goTab(Dest.Settings) }
                    }
                    composable(Dest.Widgets.route) {
                        WidgetsScreen(state.birth) { nav.goTab(Dest.Settings) }
                    }
                    composable(SETUP_ROUTE) {
                        SetupScreen { b ->
                            vm.saveBirth(b)
                            // Setup must not stay in the back stack under Today.
                            nav.navigate(Dest.Today.route) { popUpTo(SETUP_ROUTE) { inclusive = true } }
                        }
                    }
                    composable(Dest.Settings.route) {
                        state.birth?.let { birth ->
                            SettingsScreen(
                                birth = birth,
                                themeMode = state.themeMode,
                                onBirth = vm::saveBirth,
                                onTheme = vm::setTheme,
                            )
                        }
                    }
                }
            }
            Confetti(confetti)
        }
    }
}

private const val SETUP_ROUTE = "setup"

private fun NavController.goTab(d: Dest) = navigate(d.route) {
    // Tabs hang off Today, which is always in the back stack once a birth date exists.
    popUpTo(Dest.Today.route) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

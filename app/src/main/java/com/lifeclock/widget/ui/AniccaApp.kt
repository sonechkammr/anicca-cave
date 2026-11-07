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
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.lifeclock.widget.R
import com.lifeclock.widget.ui.components.Confetti
import com.lifeclock.widget.ui.settings.SettingsScreen
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
        // Without a birth date the app opens on Settings; decided once, so saving does not rebuild the graph.
        val start = remember { if (state.birth == null) Dest.Settings else Dest.Today }
        val backStack by nav.currentBackStackEntryAsState()
        val current = backStack?.destination

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.imePadding(),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    NavigationBar {
                        Dest.entries.forEach { d ->
                            NavigationBarItem(
                                selected = current?.hierarchy?.any { it.route == d.route } == true,
                                onClick = { nav.goTab(d) },
                                icon = { Icon(d.icon, contentDescription = null) },
                                label = { Text(stringResource(d.label)) },
                            )
                        }
                    }
                },
            ) { inner ->
                NavHost(
                    navController = nav,
                    startDestination = start.route,
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
                    composable(Dest.Settings.route) {
                        SettingsScreen(
                            birth = state.birth,
                            themeMode = state.themeMode,
                            onBirth = { b ->
                                vm.saveBirth(b)
                                nav.goTab(Dest.Today)
                            },
                            onTheme = vm::setTheme,
                        )
                    }
                }
            }
            Confetti(confetti)
        }
    }
}

private fun NavController.goTab(d: Dest) = navigate(d.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

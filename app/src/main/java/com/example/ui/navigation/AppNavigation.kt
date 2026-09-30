package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.di.AppContainer
import com.example.ui.screens.browser.BrowserScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.scenes.ScenesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.studio.StudioScreen
import com.example.ui.theme.DarkSlateBorder
import com.example.ui.theme.DarkSlateSurface
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.*

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Scenes : Screen("scenes", "Scenes", Icons.Filled.GridView, Icons.Outlined.GridView)
    object Studio : Screen("studio/{sceneId}", "Studio", Icons.Filled.VideoCameraFront, Icons.Outlined.VideoCameraFront) {
        fun createRoute(sceneId: String) = "studio/$sceneId"
    }
    object Library : Screen("library", "Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    object Browser : Screen("browser", "Browser", Icons.Filled.Language, Icons.Outlined.Language)
}

@Composable
fun AppNavigation(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val factory = remember {
        ViewModelFactory(
            sceneRepository = container.sceneRepository,
            preferencesRepository = container.preferencesRepository,
            mediaRepository = container.mediaRepository,
            streamingEngine = container.streamingEngine
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isStudioScreen = currentRoute?.startsWith("studio/") == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isStudioScreen) {
                NavigationBar(
                    containerColor = DarkSlateSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    val items = listOf(
                        Screen.Home,
                        Screen.Scenes,
                        Screen.Library,
                        Screen.Settings
                    )
                    items.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = ElectricIndigo,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                val homeVm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    viewModel = homeVm,
                    onNavigateToStudio = { sceneId ->
                        navController.navigate(Screen.Studio.createRoute(sceneId))
                    },
                    onNavigateToBrowser = {
                        navController.navigate(Screen.Browser.route)
                    }
                )
            }
            composable(Screen.Scenes.route) {
                val scenesVm: ScenesViewModel = viewModel(factory = factory)
                ScenesScreen(
                    viewModel = scenesVm,
                    onNavigateToStudio = { sceneId ->
                        navController.navigate(Screen.Studio.createRoute(sceneId))
                    }
                )
            }
            composable(
                route = Screen.Studio.route,
                arguments = listOf(navArgument("sceneId") { type = NavType.StringType })
            ) { entry ->
                val sceneId = entry.arguments?.getString("sceneId") ?: "default_scene"
                val studioVm: StudioViewModel = viewModel(factory = factory)
                StudioScreen(
                    sceneId = sceneId,
                    viewModel = studioVm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Library.route) {
                val libraryVm: LibraryViewModel = viewModel(factory = factory)
                LibraryScreen(viewModel = libraryVm)
            }
            composable(Screen.Settings.route) {
                val settingsVm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(viewModel = settingsVm)
            }
            composable(Screen.Browser.route) {
                BrowserScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

package ru.hoster.inprogress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import ru.hoster.inprogress.navigation.MainScreen
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import ru.hoster.inprogress.navigation.StatsScreen
import ru.hoster.inprogress.navigation.addeditactivity.AddEditActivityScreen
import ru.hoster.inprogress.navigation.AddEditGoalScreen
import ru.hoster.inprogress.navigation.bottomNavItems
import ru.hoster.inprogress.navigation.groups.GroupsScreen
import ru.hoster.inprogress.navigation.HelpFaqScreen
import ru.hoster.inprogress.navigation.HomeViewModel
import ru.hoster.inprogress.navigation.ProfileScreen
import ru.hoster.inprogress.navigation.Route
import ru.hoster.inprogress.navigation.Screen
import ru.hoster.inprogress.navigation.UserSettingsScreen
import ru.hoster.inprogress.navigation.achievements.AchievementsScreen
import ru.hoster.inprogress.navigation.groups.AddEditGroupScreen
import ru.hoster.inprogress.navigation.groups.GroupDetailsScreen
import ru.hoster.inprogress.navigation.LoginScreen
import ru.hoster.inprogress.navigation.MainScreenUiState
import ru.hoster.inprogress.navigation.SignUpScreen
import ru.hoster.inprogress.navigation.StatsViewModel


import ru.hoster.inprogress.ui.theme.InProgressTheme

@AndroidEntryPoint // Важно для Hilt
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()

        setContent {
            InProgressTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route


                val showBottomBar = bottomNavItems.any { it.route == currentRoute } &&
                        currentRoute != Route.LOGIN && currentRoute != Route.SIGN_UP

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                val currentDestination = navBackStackEntry?.destination
                                bottomNavItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                                        label = { Text(screen.title) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Route.LOGIN, // Начинаем с экрана входа
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // --- Экраны Аутентификации ---
                        composable(Route.LOGIN) {
                            LoginScreen(navController = navController)
                        }
                        composable(Route.SIGN_UP) {
                            SignUpScreen(navController = navController)
                        }

                        // --- Основные экраны (с Bottom Navigation) ---
                        composable(Screen.Main.route) {
                            val homeViewModel: HomeViewModel = hiltViewModel()
                            // Создаем временную заглушку для uiState
                            val placeholderUiState = MainScreenUiState()
                            val uiState by homeViewModel.uiState.collectAsState()
                            MainScreen(
                                onDailyTimerClick = { navController.navigate(Route.STATISTICS) },
                                onAddActivityClick = { navController.navigate(Route.addEditActivity()) },
                                onEditGoalClick = { goalId ->
                                    navController.navigate(Route.addEditGoal(goalId = goalId))
                                },

                                onDeleteActivityClick = { activityId -> homeViewModel.onDeleteActivityClick(activityId)},
                                onActivityTimerToggle = {  activityId-> homeViewModel.onActivityTimerToggle(activityId)  /* Пока ничего не делаем */ },
                                uiState = uiState,
                                onAddNewGoalClick = { /* Пока ничего не делаем */ },
                                onViewAllGoalsClick = { /* Пока ничего не делаем */ }
                            )
                        }
                        composable(Screen.Groups.route) {
                            GroupsScreen(
                                onNavigateToGroupDetails = { groupId ->
                                    navController.navigate(Route.groupDetails(groupId))
                                }

                            )
                        }
                        composable(Screen.Profile.route) {
                            ProfileScreen(
                                onNavigateToAchievements = { navController.navigate(Route.ACHIEVEMENTS) },
                                onNavigateToHelp = { navController.navigate(Route.HELP_FAQ) },
                                onNavigateToUserSettings = { navController.navigate(Route.USER_SETTINGS) }
                            )
                        }

                        // --- Вторичные экраны ---
                        composable(Route.STATISTICS) {
                            val viewModel: StatsViewModel = hiltViewModel()
                            StatsScreen(
                                dailyViewModel = viewModel
                            )
                        }
                        composable(
                            route = Route.addEditActivity(),
                            arguments = listOf(navArgument("activityId") {
                                type = NavType.StringType
                                nullable = true
                            })
                        ) {
                            AddEditActivityScreen(navController = navController)
                        }
                        composable(
                            route = Route.addEditGoal(),
                            arguments = listOf(navArgument("goalId") {
                                type = NavType.StringType
                                nullable = true
                            })
                        ) {
                            AddEditGoalScreen(
                                navController = navController,
                                goalId = it.arguments?.getString("goalId")
                            )
                        }
                        composable(Route.ACHIEVEMENTS) {
                            AchievementsScreen(navController)
                        }
                        composable(Route.HELP_FAQ) {
                            HelpFaqScreen(navController)
                        }
                        composable(Route.USER_SETTINGS) {
                            UserSettingsScreen(navController)
                        }

                        // --- Экраны, связанные с группами (вторичные) ---
                        composable(
                            route = Route.addEditGroup(), // Используем helper функцию
                            arguments = listOf(navArgument("groupId") {
                                type = NavType.StringType
                                nullable = true
                            })
                        ) { backStackEntry ->
                            AddEditGroupScreen(
                                navController = navController,
                                groupId = backStackEntry.arguments?.getString("groupId")
                            )
                        }
                        composable(

                            route = Route.GROUP_DETAILS + "/{groupId}",
                            arguments = listOf(navArgument("groupId") {
                                type = NavType.StringType
                            })
                        ) {
                            GroupDetailsScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    InProgressTheme {
        Text("App Preview Holder - Запустите на эмуляторе для проверки навигации")
    }
}
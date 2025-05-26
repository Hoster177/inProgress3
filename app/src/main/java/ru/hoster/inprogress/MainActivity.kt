package ru.hoster.inprogress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import dagger.hilt.android.AndroidEntryPoint // Убедитесь, что эта аннотация есть

// Ваши существующие импорты экранов
import ru.hoster.inprogress.navigation.addeditactivity.AddEditActivityScreen
import ru.hoster.inprogress.navigation.AddEditGoalScreen
import ru.hoster.inprogress.navigation.bottomNavItems
import ru.hoster.inprogress.navigation.groups.GroupsScreen
import ru.hoster.inprogress.navigation.HelpFaqScreen
import ru.hoster.inprogress.navigation.HomeViewModel
import ru.hoster.inprogress.navigation.ProfileScreen
import ru.hoster.inprogress.navigation.Route // Ваш объект Route
import ru.hoster.inprogress.navigation.Screen // Ваш sealed class Screen для BottomNav
import ru.hoster.inprogress.navigation.StatisticsScreen
import ru.hoster.inprogress.navigation.UserSettingsScreen
import ru.hoster.inprogress.navigation.achievements.AchievementsScreen
import ru.hoster.inprogress.navigation.groups.AddEditGroupScreen
import ru.hoster.inprogress.navigation.groups.GroupDetailsScreen

// НОВЫЕ ИМПОРТЫ для экранов аутентификации (пути могут отличаться в зависимости от вашей структуры)
import ru.hoster.inprogress.navigation.LoginScreen // Предполагая, что LoginScreen.kt в пакете navigation
import ru.hoster.inprogress.navigation.MainScreenUiState
import ru.hoster.inprogress.navigation.MainScreenUiStatePlaceholder
import ru.hoster.inprogress.navigation.SignUpScreen // Предполагая, что SignUpScreen.kt в пакете navigation


import ru.hoster.inprogress.ui.theme.InProgressTheme

@AndroidEntryPoint // Важно для Hilt
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Опционально, если используете

        setContent {
            InProgressTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Определяем, нужно ли показывать BottomBar
                // Не показываем на экранах Login и SignUp
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
                            val placeholderUiState = MainScreenUiState() // Используйте ваше имя класса

                            MainScreen(
                                onDailyTimerClick = { navController.navigate(Route.STATISTICS) },
                                onAddActivityClick = { navController.navigate(Route.addEditActivity()) },
                                onEditGoalClick = { goalId ->
                                    navController.navigate(Route.addEditGoal(goalId = goalId))
                                },
                                // Замените
                                onDeleteActivityClick = { activityId -> homeViewModel.onDeleteActivityClick(activityId)},
                                onActivityTimerToggle = {  activityId, isActive -> homeViewModel.onActivityTimerToggle(activityId, isActive)  /* Пока ничего не делаем */ },
                                uiState = placeholderUiState, // <--- ЗАМЕНА
                                onAddNewGoalClick = { /* Пока ничего не делаем */ },
                                onViewAllGoalsClick = { /* Пока ничего не делаем */ }
                            )
                        }
                        composable(Screen.Groups.route) {
                            GroupsScreen(
                                onNavigateToGroupDetails = { groupId -> // Реализуем навигацию
                                    navController.navigate(Route.groupDetails(groupId))
                                }
                                // navController = navController // Передаем, если GroupsScreen имеет свою под-навигацию
                                // ViewModel будет внедрен через Hilt позже
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
                            StatisticsScreen(navController)
                        }
                        composable(
                            route = Route.addEditActivity(), // Используем helper функцию
                            arguments = listOf(navArgument("activityId") {
                                type = NavType.StringType
                                nullable = true
                            })
                        ) {
                            AddEditActivityScreen(navController = navController)
                        }
                        composable(
                            route = Route.addEditGoal(), // Используем helper функцию
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
                            // Маршрут должен соответствовать определению в Route.kt, включая обязательный аргумент
                            route = Route.GROUP_DETAILS + "/{groupId}",
                            arguments = listOf(navArgument("groupId") {
                                type = NavType.StringType
                                // nullable = false // groupId здесь обязателен
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
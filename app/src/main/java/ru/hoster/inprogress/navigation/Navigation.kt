package ru.hoster.inprogress.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Main : Screen("main", "Главная", Icons.Filled.Home)
    object Groups : Screen("groups", "Группы", Icons.Filled.Group)
    object Profile : Screen("profile", "Профиль", Icons.Filled.AccountCircle)
}

val bottomNavItems = listOf(
    Screen.Main,
    Screen.Groups,
    Screen.Profile
)

object Route {
    const val STATISTICS = "statistics_screen"
    const val ADD_EDIT_ACTIVITY = "add_edit_activity_screen"
    const val ADD_EDIT_GOAL = "add_edit_goal_screen"
    const val ACHIEVEMENTS = "achievements_screen"
    const val HELP_FAQ = "help_faq_screen"
    const val USER_SETTINGS = "user_settings_screen"


    const val LOGIN = "login_screen"
    const val SIGN_UP = "signup_screen"
    const val ADD_EDIT_GROUP = "add_edit_group_screen"
    const val GROUP_DETAILS = "group_details_screen"


    fun addEditActivity(activityId: String? = null): String {
        return if (activityId != null) "$ADD_EDIT_ACTIVITY?activityId=$activityId" else ADD_EDIT_ACTIVITY
    }

    fun addEditGoal(goalId: String? = null): String {
        return if (goalId != null) "$ADD_EDIT_GOAL?goalId=$goalId" else ADD_EDIT_GOAL
    }

    fun addEditGroup(groupId: String? = null): String {
        return if (groupId != null) "$ADD_EDIT_GROUP?groupId=$groupId" else ADD_EDIT_GROUP
    }

    fun groupDetails(groupId: String): String {
        return "$GROUP_DETAILS/$groupId"
    }
}
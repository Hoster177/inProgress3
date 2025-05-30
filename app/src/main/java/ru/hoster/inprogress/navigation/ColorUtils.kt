package ru.hoster.inprogress.navigation

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

fun parseColor(colorString: String): Color {
    return try {
        if (colorString.startsWith("#") && (colorString.length == 7 || colorString.length == 9)) {
            Color(colorString.toColorInt())
        } else {
            Color.Gray
        }
    } catch (e: IllegalArgumentException) {
        Color.Gray
    }
}

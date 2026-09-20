package com.neelpatel.todo.tasktracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BluePrimary = Color(0xFF0056D2)
val BluePrimaryDark = Color(0xFF3B82F6) // A slightly lighter blue for dark mode visibility

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = Color.White,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onBackground = Color.White,
    onSurface = Color.White,
    outline = Color(0xFF334155),
    surfaceVariant = Color(0xFF2D3748), // Darker shade for contrast in containers
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    background = Color(0xFFF8F9FB),
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    outline = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFFF3F4F6), // Light grey for containers
    onSurfaceVariant = Color.Gray
)

@Composable
fun ToDoTaskTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

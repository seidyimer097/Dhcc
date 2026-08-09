package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.PanelRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sealed class representing screens in the ShM School system.
 */
sealed class Screen(val title: String, val route: String) {
    data object Login : Screen("Authentication", "login")
    data object AdminDashboard : Screen("Admin Dashboard", "admin_dashboard")
    data object TeacherDashboard : Screen("Teacher Dashboard", "teacher_dashboard")
    data object StudentDashboard : Screen("Student Dashboard", "student_dashboard")
    data object UserProfile : Screen("User Profile", "user_profile")
}

/**
 * Simple state-based NavigationManager to manage app navigation stack and active screen state.
 */
class NavigationManager(initialScreen: Screen = Screen.StudentDashboard) {

    private val _currentScreen = MutableStateFlow(initialScreen)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val backStack = mutableListOf<Screen>()

    /**
     * Navigate to a target screen.
     */
    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            backStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    /**
     * Navigate back to the previous screen if available.
     * @return true if popped, false if backStack was empty.
     */
    fun popBackStack(): Boolean {
        if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeAt(backStack.size - 1)
            return true
        }
        return false
    }

    /**
     * Map PanelRole to corresponding Screen destination.
     */
    fun navigateForRole(role: PanelRole) {
        val targetScreen = when (role) {
            PanelRole.ADMIN -> Screen.AdminDashboard
            PanelRole.TEACHER -> Screen.TeacherDashboard
            PanelRole.STUDENT -> Screen.StudentDashboard
        }
        navigateTo(targetScreen)
    }

    companion object {
        fun roleToScreen(role: PanelRole): Screen = when (role) {
            PanelRole.ADMIN -> Screen.AdminDashboard
            PanelRole.TEACHER -> Screen.TeacherDashboard
            PanelRole.STUDENT -> Screen.StudentDashboard
        }
    }
}

/**
 * Composable helper to remember a NavigationManager instance.
 */
@Composable
fun rememberNavigationManager(initialScreen: Screen = Screen.StudentDashboard): NavigationManager {
    return remember { NavigationManager(initialScreen) }
}

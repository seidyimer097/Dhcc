package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.navigation.NavigationManager
import com.example.navigation.Screen
import com.example.navigation.rememberNavigationManager
import com.example.ui.PanelRole
import com.example.ui.SchoolViewModel
import com.example.ui.components.AuthModal
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.TopHeader
import com.example.ui.panels.AdminPanel
import com.example.ui.panels.StudentPanel
import com.example.ui.panels.TeacherPanel
import com.example.ui.theme.ShMSchoolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SchoolViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            ShMSchoolTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: SchoolViewModel,
    navManager: NavigationManager = rememberNavigationManager(Screen.StudentDashboard)
) {
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val systemSettings by viewModel.systemSettings.collectAsStateWithLifecycle()
    val adminExists by viewModel.adminExists.collectAsStateWithLifecycle()
    val studentProfileState by viewModel.studentProfileState.collectAsStateWithLifecycle()

    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allClasses by viewModel.allClasses.collectAsStateWithLifecycle()
    val allAssignments by viewModel.allAssignments.collectAsStateWithLifecycle()
    val allSubmissions by viewModel.allSubmissions.collectAsStateWithLifecycle()

    val teacherClasses by viewModel.teacherClasses.collectAsStateWithLifecycle()
    val teacherAssignments by viewModel.teacherAssignments.collectAsStateWithLifecycle()

    val studentClasses by viewModel.studentClasses.collectAsStateWithLifecycle()
    val studentSubmissions by viewModel.studentSubmissions.collectAsStateWithLifecycle()

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotificationCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

    val currentScreen by navManager.currentScreen.collectAsStateWithLifecycle()
    var showAuthModal by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    // Synchronize navManager screen with activeRole changes from ViewModel
    LaunchedEffect(activeRole) {
        navManager.navigateForRole(activeRole)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopHeader(
                activeRole = activeRole,
                currentUser = currentUser,
                isDarkMode = isDarkMode,
                toastMessage = toastMessage,
                unreadNotificationCount = unreadNotificationCount,
                onRoleSelected = { role ->
                    viewModel.setPanelRole(role)
                    navManager.navigateForRole(role)
                },
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onOpenAuthModal = {
                    navManager.navigateTo(Screen.Login)
                    showAuthModal = true
                },
                onClearToast = { viewModel.clearToast() },
                onOpenProfile = {
                    navManager.navigateTo(Screen.UserProfile)
                },
                onOpenNotifications = {
                    showNotificationsDialog = true
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                is Screen.Login -> {
                    // Render current active role screen with AuthModal displayed
                    when (activeRole) {
                        PanelRole.ADMIN -> AdminPanel(
                            allUsers = allUsers,
                            totalClassesCount = allClasses.size,
                            totalSubmissionsCount = allSubmissions.size,
                            systemSettings = systemSettings,
                            onToggleTeacherReg = { allowed -> viewModel.updateTeacherRegistrationAllowed(allowed) },
                            onToggleStudentReg = { allowed -> viewModel.updateStudentRegistrationAllowed(allowed) }
                        )
                        PanelRole.TEACHER -> TeacherPanel(
                            currentUser = currentUser,
                            classes = teacherClasses,
                            assignments = teacherAssignments,
                            allSubmissions = allSubmissions,
                            onCreateClass = { className, subject, bannerUrl, callback ->
                                viewModel.createClass(className, subject, bannerUrl, callback)
                            },
                            onCreateAssignment = { classId, className, title, desc, dueDate, points, attachment, callback ->
                                viewModel.createAssignment(classId, className, title, desc, dueDate, points, attachment, callback)
                            },
                            onUploadToCloudinary = { filename, onUrl ->
                                viewModel.uploadToCloudinary(filename, onUrl)
                            }
                        )
                        PanelRole.STUDENT -> StudentPanel(
                            currentUser = currentUser,
                            studentProfileState = studentProfileState,
                            enrolledClasses = studentClasses,
                            assignments = allAssignments.filter { assignment -> studentClasses.any { it.id == assignment.classId } },
                            submissions = studentSubmissions,
                            onJoinClass = { code, callback ->
                                viewModel.joinClassWithCode(code, callback)
                            },
                            onSubmitWork = { assignmentId, assignmentTitle, classId, fileUrl, comment, callback ->
                                viewModel.submitAssignmentWork(assignmentId, assignmentTitle, classId, fileUrl, comment, callback)
                            },
                            onUploadToCloudinary = { filename, onUrl ->
                                viewModel.uploadToCloudinary(filename, onUrl)
                            }
                        )
                    }
                }

                is Screen.AdminDashboard -> AdminPanel(
                    allUsers = allUsers,
                    allClasses = allClasses,
                    totalClassesCount = allClasses.size,
                    totalSubmissionsCount = allSubmissions.size,
                    systemSettings = systemSettings,
                    onToggleTeacherReg = { allowed -> viewModel.updateTeacherRegistrationAllowed(allowed) },
                    onToggleStudentReg = { allowed -> viewModel.updateStudentRegistrationAllowed(allowed) },
                    onUpdateUserRole = { targetUser, newRole ->
                        viewModel.updateUserRole(targetUser, newRole) { _, _ -> }
                    }
                )

                is Screen.TeacherDashboard -> TeacherPanel(
                    currentUser = currentUser,
                    classes = teacherClasses,
                    assignments = teacherAssignments,
                    allSubmissions = allSubmissions,
                    onCreateClass = { className, subject, bannerUrl, callback ->
                        viewModel.createClass(className, subject, bannerUrl, callback)
                    },
                    onCreateAssignment = { classId, className, title, desc, dueDate, points, attachment, callback ->
                        viewModel.createAssignment(classId, className, title, desc, dueDate, points, attachment, callback)
                    },
                    onUploadToCloudinary = { filename, onUrl ->
                        viewModel.uploadToCloudinary(filename, onUrl)
                    }
                )

                is Screen.StudentDashboard -> StudentPanel(
                    currentUser = currentUser,
                    studentProfileState = studentProfileState,
                    enrolledClasses = studentClasses,
                    assignments = allAssignments.filter { assignment -> studentClasses.any { it.id == assignment.classId } },
                    submissions = studentSubmissions,
                    onJoinClass = { code, callback ->
                        viewModel.joinClassWithCode(code, callback)
                    },
                    onSubmitWork = { assignmentId, assignmentTitle, classId, fileUrl, comment, callback ->
                        viewModel.submitAssignmentWork(assignmentId, assignmentTitle, classId, fileUrl, comment, callback)
                    },
                    onUploadToCloudinary = { filename, onUrl ->
                        viewModel.uploadToCloudinary(filename, onUrl)
                    }
                )

                is Screen.UserProfile -> com.example.ui.screens.UserProfileScreen(
                    currentUser = currentUser,
                    onUpdateProfile = { name, avatarUrl, callback ->
                        viewModel.updateUserProfile(name, avatarUrl, callback)
                    },
                    onUploadToCloudinary = { filename, onUrl ->
                        viewModel.uploadToCloudinary(filename, onUrl)
                    },
                    onNavigateBack = {
                        if (!navManager.popBackStack()) {
                            navManager.navigateForRole(activeRole)
                        }
                    }
                )
            }
        }
    }

    if (showAuthModal || currentScreen is Screen.Login) {
        AuthModal(
            activeRole = activeRole,
            currentUser = currentUser,
            adminExists = adminExists,
            systemSettings = systemSettings,
            onDismiss = {
                showAuthModal = false
                navManager.navigateForRole(activeRole)
            },
            onLogin = { email, role, callback ->
                viewModel.loginUser(email, role) { success, msg ->
                    if (success) {
                        navManager.navigateForRole(role)
                        showAuthModal = false
                    }
                    callback(success, msg)
                }
            },
            onRegister = { name, email, role, callback ->
                viewModel.registerUser(name, email, role) { success, msg ->
                    if (success) {
                        navManager.navigateForRole(role)
                        showAuthModal = false
                    }
                    callback(success, msg)
                }
            }
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            notifications = notifications,
            onMarkAsRead = { notifId ->
                viewModel.markNotificationAsRead(notifId)
            },
            onClearAll = {
                viewModel.clearAllNotifications()
            },
            onDismiss = {
                showNotificationsDialog = false
            }
        )
    }
}

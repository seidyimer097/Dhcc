package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthHelper
import com.example.data.AuthResult
import com.example.data.DatabaseHelper
import kotlinx.coroutines.launch
import com.example.data.SystemSettingsEntity
import com.example.ui.PanelRole
import com.example.ui.components.GlassCard
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    modifier: Modifier = Modifier,
    initialRole: PanelRole = PanelRole.STUDENT,
    onRegistrationSuccess: (PanelRole) -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(initialRole) }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingRestrictions by remember { mutableStateOf(true) }
    var adminExists by remember { mutableStateOf(false) }
    var systemSettings by remember { mutableStateOf(SystemSettingsEntity()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Check if admin exists and load settings via DatabaseHelper
    LaunchedEffect(Unit) {
        isCheckingRestrictions = true
        try {
            val dbHelper = DatabaseHelper.getInstance()
            adminExists = dbHelper.hasAdminUser()
            systemSettings = dbHelper.fetchSystemSettings() ?: SystemSettingsEntity()

            // If initial role is ADMIN but admin exists, fallback to STUDENT
            if (selectedRole == PanelRole.ADMIN && adminExists) {
                selectedRole = PanelRole.STUDENT
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isCheckingRestrictions = false
        }
    }

    val disableAdminSelection = adminExists
    val disableTeacherSelection = !systemSettings.allowTeacherRegistration
    val disableStudentSelection = !systemSettings.allowStudentRegistration

    val currentRoleRestricted = when (selectedRole) {
        PanelRole.ADMIN -> adminExists
        PanelRole.TEACHER -> disableTeacherSelection
        PanelRole.STUDENT -> disableStudentSelection
    }

    Scaffold(
        modifier = modifier.testTag("registration_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin, modifier = Modifier.testTag("btn_back_to_login")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Login")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "School Management System",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Register account with assigned permissions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Error / Restriction Notice Banner
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("registration_error_banner")
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                if (isCheckingRestrictions) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Verifying database registration rules...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Role Selection Segmented Buttons
                Text(
                    text = "Select Account Role *",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("role_selection_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedRole == PanelRole.ADMIN,
                        onClick = {
                            if (!disableAdminSelection) {
                                selectedRole = PanelRole.ADMIN
                                errorMessage = null
                            }
                        },
                        label = { Text("Admin") },
                        leadingIcon = {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        enabled = !disableAdminSelection,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_role_admin")
                    )

                    FilterChip(
                        selected = selectedRole == PanelRole.TEACHER,
                        onClick = {
                            if (!disableTeacherSelection) {
                                selectedRole = PanelRole.TEACHER
                                errorMessage = null
                            }
                        },
                        label = { Text("Teacher") },
                        enabled = !disableTeacherSelection,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_role_teacher")
                    )

                    FilterChip(
                        selected = selectedRole == PanelRole.STUDENT,
                        onClick = {
                            if (!disableStudentSelection) {
                                selectedRole = PanelRole.STUDENT
                                errorMessage = null
                            }
                        },
                        label = { Text("Student") },
                        enabled = !disableStudentSelection,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_role_student")
                    )
                }

                // Role Restriction Callout Box
                if (selectedRole == PanelRole.ADMIN && adminExists) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_restriction_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Admin Registration Restricted: An Admin account already exists in Database. Only 1 Admin is permitted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (currentRoleRestricted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Registration for ${selectedRole.name.uppercase()} is currently disabled by administrator settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Form Fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_registration_name")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_registration_email")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password * (Min 6 chars)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_registration_password")
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_registration_confirm_password")
                )

                Spacer(modifier = Modifier.height(8.dp))

                val scope = rememberCoroutineScope()

                Button(
                    onClick = {
                        errorMessage = null
                        if (name.isBlank()) {
                            errorMessage = "Full Name is required."
                            return@Button
                        }
                        if (email.isBlank() || !email.contains("@")) {
                            errorMessage = "Please enter a valid email address."
                            return@Button
                        }
                        if (password.length < 6) {
                            errorMessage = "Password must be at least 6 characters long."
                            return@Button
                        }
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match."
                            return@Button
                        }
                        if (currentRoleRestricted) {
                            errorMessage = "Registration for ${selectedRole.name.lowercase()} is restricted."
                            return@Button
                        }

                        isLoading = true
                        scope.launch {
                            try {
                                val result = AuthHelper.getInstance().signUpWithEmail(
                                    email = email.trim(),
                                    password = password,
                                    displayName = name.trim(),
                                    role = selectedRole.name.lowercase()
                                )
                                when (result) {
                                    is AuthResult.Success -> {
                                        snackbarHostState.showSnackbar("Account created successfully as ${selectedRole.name.uppercase()}!")
                                        onRegistrationSuccess(selectedRole)
                                    }
                                    is AuthResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Registration failed."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && !currentRoleRestricted,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_submit_registration")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating Account...")
                    } else {
                        Text(
                            text = "Register as ${selectedRole.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.testTag("btn_switch_to_login")
                ) {
                    Text("Already have an account? Sign In")
                }
            }
        }
    }
}

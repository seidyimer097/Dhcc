package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SystemSettingsEntity
import com.example.data.UserEntity
import com.example.ui.PanelRole

@Composable
fun AuthModal(
    activeRole: PanelRole,
    currentUser: UserEntity?,
    adminExists: Boolean,
    systemSettings: SystemSettingsEntity,
    onDismiss: () -> Unit,
    onLogin: (String, PanelRole, (Boolean, String) -> Unit) -> Unit,
    onRegister: (String, String, PanelRole, (Boolean, String) -> Unit) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedRole by remember { mutableStateOf(activeRole) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRegisterMode) "Create Account" else "User Authentication")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (currentUser != null) {
                    Text(
                        text = "Logged in as: ${currentUser.name} (${currentUser.role.uppercase()})",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Email: ${currentUser.email}", style = MaterialTheme.typography.bodySmall)
                }

                Text("Select Role Context:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Hide/disable Admin option in register mode if Admin already exists
                    val disableAdminRegister = isRegisterMode && adminExists

                    FilterChip(
                        selected = selectedRole == PanelRole.ADMIN,
                        onClick = { if (!disableAdminRegister) selectedRole = PanelRole.ADMIN },
                        label = { Text("Admin") },
                        enabled = !disableAdminRegister,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedRole == PanelRole.TEACHER,
                        onClick = { selectedRole = PanelRole.TEACHER },
                        label = { Text("Teacher") },
                        enabled = !isRegisterMode || systemSettings.allowTeacherRegistration,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedRole == PanelRole.STUDENT,
                        onClick = { selectedRole = PanelRole.STUDENT },
                        label = { Text("Student") },
                        enabled = !isRegisterMode || systemSettings.allowStudentRegistration,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isRegisterMode && selectedRole == PanelRole.ADMIN && adminExists) {
                    Text(
                        text = "Admin account already exists. Only 1 Admin is allowed in the system.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_name")
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_auth_email")
                )

                errorMessage?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        errorMessage = null
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (isRegisterMode) "Already have an account? Login" else "Don't have an account? Register")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isBlank()) {
                        errorMessage = "Please enter an email address."
                        return@Button
                    }
                    if (isRegisterMode) {
                        onRegister(name, email, selectedRole) { success, msg ->
                            if (success) onDismiss() else errorMessage = msg
                        }
                    } else {
                        onLogin(email, selectedRole) { success, msg ->
                            if (success) onDismiss() else errorMessage = msg
                        }
                    }
                },
                modifier = Modifier.testTag("btn_auth_submit")
            ) {
                Text(if (isRegisterMode) "Register" else "Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

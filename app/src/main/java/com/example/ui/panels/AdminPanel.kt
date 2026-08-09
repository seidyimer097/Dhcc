package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SystemSettingsEntity
import com.example.data.UserEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.MetricBarChart
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.StatusSuccess
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun AdminPanel(
    allUsers: List<UserEntity>,
    allClasses: List<com.example.data.SchoolClassEntity> = emptyList(),
    totalClassesCount: Int,
    totalSubmissionsCount: Int,
    systemSettings: SystemSettingsEntity,
    onToggleTeacherReg: (Boolean) -> Unit,
    onToggleStudentReg: (Boolean) -> Unit,
    onUpdateUserRole: ((UserEntity, String) -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Users, 2: Audit Logs, 3: Settings
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserDetail by remember { mutableStateOf<UserEntity?>(null) }
    var selectedRoleToAssign by remember(selectedUserDetail) { mutableStateOf(selectedUserDetail?.role ?: "student") }

    val teachers = allUsers.filter { it.role == "teacher" }
    val students = allUsers.filter { it.role == "student" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Sub-Navigation Tabs: Dashboard | Users | Audit Logs | Settings
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 0.dp,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Users (${allUsers.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Audit Logs", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_audit_logs")
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Settings", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> com.example.ui.screens.AdminDashboardContent(
                stats = com.example.data.AdminStats(
                    totalTeachers = teachers.size,
                    totalStudents = students.size,
                    totalClasses = totalClassesCount,
                    totalSubmissions = totalSubmissionsCount,
                    totalAdmins = allUsers.count { it.role.equals("admin", ignoreCase = true) }
                ),
                users = allUsers,
                classes = allClasses,
                onNavigateToUserManagement = { selectedTab = 1 }
            )
            1 -> UserManagementTab(
                users = allUsers,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onSelectUser = { selectedUserDetail = it }
            )
            2 -> AuditLogsTab()
            3 -> AdminSettingsTab(
                settings = systemSettings,
                onToggleTeacherReg = onToggleTeacherReg,
                onToggleStudentReg = onToggleStudentReg
            )
        }
    }

    // User Detail & Role Change Dialog
    selectedUserDetail?.let { user ->
        AlertDialog(
            onDismissRequest = { selectedUserDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("User Profile & Role Settings")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRow("ID", user.id)
                    DetailRow("Name", user.name)
                    DetailRow("Email", user.email)
                    DetailRow("Current Role", user.role.uppercase())
                    DetailRow("Registered", SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(user.registeredAt)))

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Change User Role:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("student", "teacher", "admin").forEach { role ->
                            FilterChip(
                                selected = selectedRoleToAssign.lowercase() == role,
                                onClick = { selectedRoleToAssign = role },
                                label = { Text(role.replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.testTag("chip_role_$role")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedRoleToAssign.lowercase() != user.role.lowercase() && onUpdateUserRole != null) {
                            onUpdateUserRole(user, selectedRoleToAssign)
                        }
                        selectedUserDetail = null
                    },
                    modifier = Modifier.testTag("button_save_role")
                ) {
                    Text("Save Role Change")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedUserDetail = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminDashboardTab(
    teacherCount: Int,
    studentCount: Int,
    classCount: Int,
    submissionCount: Int
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "System Metrics & Analytics",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 4 Stat Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Total Teachers",
                        value = "$teacherCount",
                        icon = Icons.Default.Group,
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Students",
                        value = "$studentCount",
                        icon = Icons.Default.School,
                        color = SecondaryTeal,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Total Classes",
                        value = "$classCount",
                        icon = Icons.Default.Class,
                        color = AccentViolet,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Submissions",
                        value = "$submissionCount",
                        icon = Icons.Default.AssignmentTurnedIn,
                        color = StatusSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Chart.js Style Canvas Visualizer
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_metric_chart")
            ) {
                MetricBarChart(
                    teacherCount = teacherCount,
                    studentCount = studentCount,
                    classCount = classCount,
                    submissionCount = submissionCount
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color)
            }
        }
    }
}

@Composable
private fun UserManagementTab(
    users: List<UserEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectUser: (UserEntity) -> Unit
) {
    val filteredUsers = users.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                it.role.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_users"),
            placeholder = { Text("Search teachers, students, or email...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Users") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredUsers.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_users_search_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("No Users Found", fontWeight = FontWeight.Bold)
                    Text(
                        "No users match \"$searchQuery\". Try searching by name, email, or role.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredUsers) { user ->
                    UserRowCard(user = user, onClick = { onSelectUser(user) })
                }
            }
        }
    }
}

@Composable
private fun UserRowCard(user: UserEntity, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("user_item_${user.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (user.role) {
                                "admin" -> AccentViolet.copy(alpha = 0.2f)
                                "teacher" -> PrimaryBlue.copy(alpha = 0.2f)
                                else -> SecondaryTeal.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = when (user.role) {
                            "admin" -> AccentViolet
                            "teacher" -> PrimaryBlue
                            else -> SecondaryTeal
                        }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (user.role) {
                    "admin" -> AccentViolet.copy(alpha = 0.2f)
                    "teacher" -> PrimaryBlue.copy(alpha = 0.2f)
                    else -> SecondaryTeal.copy(alpha = 0.2f)
                }
            ) {
                Text(
                    text = user.role.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = when (user.role) {
                        "admin" -> AccentViolet
                        "teacher" -> PrimaryBlue
                        else -> SecondaryTeal
                    }
                )
            }
        }
    }
}

@Composable
private fun AdminSettingsTab(
    settings: SystemSettingsEntity,
    onToggleTeacherReg: (Boolean) -> Unit,
    onToggleStudentReg: (Boolean) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "System Registration Controls",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Teacher Registration",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enable or disable new teachers from signing up.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = settings.allowTeacherRegistration,
                            onCheckedChange = onToggleTeacherReg,
                            modifier = Modifier.testTag("toggle_teacher_reg")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Student Registration",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enable or disable new students from signing up.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = settings.allowStudentRegistration,
                            onCheckedChange = onToggleStudentReg,
                            modifier = Modifier.testTag("toggle_student_reg")
                        )
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = AccentViolet,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Admin Single Account Policy",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Only 1 Admin document is allowed in the Firestore database. Admin registration is automatically hidden if an admin exists.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AuditLogsTab() {
    val coroutineScope = rememberCoroutineScope()
    var logs by remember { mutableStateOf<List<com.example.data.AuditLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var logFilter by remember { mutableStateOf("") }

    fun refreshLogs() {
        isLoading = true
        coroutineScope.launch {
            logs = com.example.data.AuditLogger.getInstance().fetchRecentAuditLogs()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshLogs()
    }

    val filteredLogs = logs.filter {
        logFilter.isBlank() ||
                it.action.contains(logFilter, ignoreCase = true) ||
                it.actorEmail.contains(logFilter, ignoreCase = true) ||
                it.details.contains(logFilter, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("audit_logs_tab")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "System Audit Trail",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Administrative oversight & Firestore logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { refreshLogs() },
                modifier = Modifier.testTag("button_refresh_audit_logs")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Logs")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = logFilter,
            onValueChange = { logFilter = it },
            placeholder = { Text("Filter logs by action, actor, or detail...") },
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_filter_audit_logs")
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredLogs.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (logFilter.isBlank()) "No Audit Logs Yet" else "No matching logs found",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sensitive actions like role changes, class creation, and setting changes will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredLogs) { log ->
                    AuditLogRowCard(log)
                }
            }
        }
    }
}

@Composable
private fun AuditLogRowCard(log: com.example.data.AuditLog) {
    val actionColor = when (log.action) {
        "ROLE_CHANGE" -> PrimaryBlue
        "CLASS_CREATED" -> StatusSuccess
        "ASSIGNMENT_CREATED" -> SecondaryTeal
        "SETTINGS_CHANGED" -> AccentViolet
        "PROFILE_UPDATED" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audit_log_item_${log.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = actionColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = log.action,
                        color = actionColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = log.details.ifBlank { "Action performed by ${log.actorEmail}" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Actor: ${log.actorEmail.ifBlank { log.actorId }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (log.targetId.isNotBlank()) {
                    Text(
                        text = "Target: ${log.targetId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

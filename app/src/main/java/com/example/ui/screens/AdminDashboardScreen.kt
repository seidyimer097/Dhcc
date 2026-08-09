package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AdminStats
import com.example.data.SchoolClassEntity
import com.example.data.UserEntity
import com.example.ui.AdminDashboardViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.MetricBarChart
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusSuccess
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import com.example.util.CsvExportUtil
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminDashboardViewModel = viewModel(),
    onNavigateToUserManagement: (() -> Unit)? = null,
    onNavigateToClasses: (() -> Unit)? = null
) {
    val adminStats by viewModel.adminStats.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    AdminDashboardContent(
        modifier = modifier,
        stats = adminStats,
        users = users,
        classes = classes,
        isLoading = isLoading,
        onRefresh = { viewModel.refreshData() },
        onNavigateToUserManagement = onNavigateToUserManagement,
        onNavigateToClasses = onNavigateToClasses
    )
}

@Composable
fun AdminDashboardContent(
    modifier: Modifier = Modifier,
    stats: AdminStats,
    users: List<UserEntity> = emptyList(),
    classes: List<SchoolClassEntity> = emptyList(),
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onNavigateToUserManagement: (() -> Unit)? = null,
    onNavigateToClasses: (() -> Unit)? = null
) {
    var classSearchQuery by remember { mutableStateOf("") }
    var userSearchQuery by remember { mutableStateOf("") }

    val filteredClasses = classes.filter { schoolClass ->
        classSearchQuery.isBlank() ||
                schoolClass.className.contains(classSearchQuery, ignoreCase = true) ||
                schoolClass.subject.contains(classSearchQuery, ignoreCase = true) ||
                schoolClass.teacherName.contains(classSearchQuery, ignoreCase = true) ||
                schoolClass.joinCode.contains(classSearchQuery, ignoreCase = true)
    }

    val filteredUsers = users.filter { user ->
        userSearchQuery.isBlank() ||
                user.name.contains(userSearchQuery, ignoreCase = true) ||
                user.email.contains(userSearchQuery, ignoreCase = true) ||
                user.role.contains(userSearchQuery, ignoreCase = true)
    }

    Box(modifier = modifier.fillMaxSize().testTag("admin_dashboard_screen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_dashboard_header")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Admin Overview",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Real-time school management metrics & system statistics",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .testTag("btn_refresh_admin_stats")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Stats",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Primary Stat Cards (Teachers, Students, Classes)
            item {
                Text(
                    text = "Core School Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_total_teachers"),
                        title = "Total Teachers",
                        value = stats.totalTeachers.toString(),
                        icon = Icons.Outlined.Person,
                        accentColor = PrimaryBlue
                    )

                    StatCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_total_students"),
                        title = "Total Students",
                        value = stats.totalStudents.toString(),
                        icon = Icons.Outlined.School,
                        accentColor = SecondaryTeal
                    )

                    StatCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_total_classes"),
                        title = "Total Classes",
                        value = stats.totalClasses.toString(),
                        icon = Icons.Outlined.Class,
                        accentColor = AccentViolet
                    )
                }
            }

            // Secondary Stats Cards (Assignments, Submissions, Admins)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryStatCard(
                        modifier = Modifier.weight(1f).testTag("stat_total_assignments"),
                        title = "Assignments",
                        value = stats.totalAssignments.toString(),
                        icon = Icons.Default.Assignment,
                        color = StatusSuccess
                    )
                    SecondaryStatCard(
                        modifier = Modifier.weight(1f).testTag("stat_total_submissions"),
                        title = "Submissions",
                        value = stats.totalSubmissions.toString(),
                        icon = Icons.Default.UploadFile,
                        color = PrimaryBlue
                    )
                    SecondaryStatCard(
                        modifier = Modifier.weight(1f).testTag("stat_total_admins"),
                        title = "Admins",
                        value = stats.totalAdmins.toString(),
                        icon = Icons.Default.AdminPanelSettings,
                        color = AccentViolet
                    )
                }
            }

            // User Distribution Metric Visualizer
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_distribution_chart")
                ) {
                    Text(
                        text = "User Base Composition",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    MetricBarChart(
                        teacherCount = stats.totalTeachers,
                        studentCount = stats.totalStudents,
                        classCount = stats.totalClasses,
                        submissionCount = stats.totalSubmissions
                    )
                }
            }

            // CSV Export Action Card
            item {
                var showExportModal by remember { mutableStateOf(false) }

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_csv_export")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryBlue.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Export CSV",
                                        tint = PrimaryBlue
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Export Reporting Data (CSV)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Download or share CSV reports for users & classes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showExportModal = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("button_open_csv_export")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV")
                        }
                    }

                    if (showExportModal) {
                        CsvExportModal(
                            users = users,
                            classes = classes,
                            onDismiss = { showExportModal = false }
                        )
                    }
                }
            }

            // Active Classes Summary Section
            if (classes.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Classes (${filteredClasses.size}/${classes.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (onNavigateToClasses != null) {
                                TextButton(onClick = onNavigateToClasses) {
                                    Text("View All")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = classSearchQuery,
                            onValueChange = { classSearchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_admin_search_classes"),
                            placeholder = { Text("Filter classes by name or subject...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Classes") },
                            trailingIcon = {
                                if (classSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { classSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                if (filteredClasses.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth().testTag("empty_admin_classes_search")) {
                            Text(
                                text = "No classes match \"$classSearchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    items(filteredClasses.take(5)) { classEntity ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("class_item_${classEntity.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = AccentViolet.copy(alpha = 0.15f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Class,
                                                contentDescription = null,
                                                tint = AccentViolet
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = classEntity.className,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${classEntity.subject} • Teacher: ${classEntity.teacherName.ifBlank { "Unassigned" }}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Code: ${classEntity.joinCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Registered Users Section
            if (users.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Registered Users (${filteredUsers.size}/${users.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (onNavigateToUserManagement != null) {
                                TextButton(onClick = onNavigateToUserManagement) {
                                    Text("Manage Users")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = userSearchQuery,
                            onValueChange = { userSearchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_admin_search_users"),
                            placeholder = { Text("Filter users by name, email, or role...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Users") },
                            trailingIcon = {
                                if (userSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { userSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                if (filteredUsers.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth().testTag("empty_admin_users_search")) {
                            Text(
                                text = "No users match \"$userSearchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    items(filteredUsers.take(5)) { user ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_summary_item_${user.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = when (user.role.lowercase()) {
                                            "teacher" -> PrimaryBlue.copy(alpha = 0.2f)
                                            "admin" -> AccentViolet.copy(alpha = 0.2f)
                                            else -> SecondaryTeal.copy(alpha = 0.2f)
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = user.name.take(1).uppercase().ifBlank { "U" },
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = user.name.ifBlank { "Unnamed User" },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = user.email,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Badge(
                                    containerColor = when (user.role.lowercase()) {
                                        "teacher" -> PrimaryBlue
                                        "admin" -> AccentViolet
                                        else -> SecondaryTeal
                                    },
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = user.role.replaceFirstChar { it.uppercase() },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color
) {
    GlassCard(
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SecondaryStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CsvExportModal(
    users: List<UserEntity>,
    classes: List<SchoolClassEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf("Users") } // "Users", "Classes", "Combined"
    var showCopiedToast by remember { mutableStateOf(false) }

    val csvContent = remember(selectedType, users, classes) {
        when (selectedType) {
            "Classes" -> CsvExportUtil.generateClassesCsv(classes)
            "Combined" -> CsvExportUtil.generateCombinedCsv(users, classes)
            else -> CsvExportUtil.generateUsersCsv(users)
        }
    }

    val filename = remember(selectedType) {
        val dateStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        "${selectedType.lowercase()}_export_$dateStr.csv"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Data (CSV)", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select dataset to export for administrative reporting:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Filter chips for dataset selection
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedType == "Users",
                        onClick = {
                            selectedType = "Users"
                            showCopiedToast = false
                        },
                        label = { Text("Users (${users.size})") },
                        modifier = Modifier.testTag("chip_export_users")
                    )
                    FilterChip(
                        selected = selectedType == "Classes",
                        onClick = {
                            selectedType = "Classes"
                            showCopiedToast = false
                        },
                        label = { Text("Classes (${classes.size})") },
                        modifier = Modifier.testTag("chip_export_classes")
                    )
                    FilterChip(
                        selected = selectedType == "Combined",
                        onClick = {
                            selectedType = "Combined"
                            showCopiedToast = false
                        },
                        label = { Text("Combined") },
                        modifier = Modifier.testTag("chip_export_combined")
                    )
                }

                Text(
                    text = "CSV Preview:",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium
                )

                // Code/CSV Preview box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = csvContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("text_csv_preview")
                        )
                    }
                }

                if (showCopiedToast) {
                    Text(
                        text = "✓ CSV content copied to clipboard!",
                        color = StatusSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    CsvExportUtil.shareCsvFile(context, csvContent, filename)
                    coroutineScope.launch {
                        com.example.data.AuditLogger.getInstance().logAction(
                            action = "DATA_EXPORTED",
                            actorId = "admin",
                            actorEmail = "admin@school.com",
                            details = "Exported $selectedType report ($filename)"
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("button_confirm_share_csv")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export / Share")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(csvContent))
                        showCopiedToast = true
                        coroutineScope.launch {
                            com.example.data.AuditLogger.getInstance().logAction(
                                action = "DATA_EXPORTED",
                                actorId = "admin",
                                actorEmail = "admin@school.com",
                                details = "Copied $selectedType CSV report to clipboard"
                            )
                        }
                    },
                    modifier = Modifier.testTag("button_copy_csv")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy CSV")
                }

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

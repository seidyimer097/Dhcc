package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Code
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AssignmentEntity
import com.example.data.SchoolClassEntity
import com.example.data.UserEntity
import com.example.ui.TeacherDashboardViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusSuccess

@Composable
fun TeacherDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: TeacherDashboardViewModel = viewModel(),
    onNavigateToClassDetails: ((String) -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Classes, 1: Assignments
    var showCreateClassDialog by remember { mutableStateOf(false) }
    var showCreateAssignmentDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("teacher_dashboard_screen")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Teacher Header Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_header_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Teacher Profile",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Teacher Workspace",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser?.name ?: "Teacher Account",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentUser?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.loadData() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("btn_refresh_teacher_data")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Quick Stats Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_classes_count"),
                    shape = RoundedCornerShape(16.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = classes.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "My Classes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_assignments_count"),
                    shape = RoundedCornerShape(16.dp),
                    color = SecondaryTeal.copy(alpha = 0.12f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = assignments.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryTeal
                        )
                        Text(
                            text = "Assignments",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tabs & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabRow(
                    selectedTabIndex = activeTab,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                "Classes (${classes.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text(
                                "Assignments (${assignments.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCreateClassDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_add_class")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Class",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Class")
                }

                Button(
                    onClick = { showCreateAssignmentDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_add_assignment")
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "New Assignment",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Assignment")
                }
            }

            // Content Area
            when (activeTab) {
                0 -> ClassesListSection(
                    classes = classes,
                    assignments = assignments,
                    onNavigateToClassDetails = onNavigateToClassDetails,
                    onOpenCreateClass = { showCreateClassDialog = true }
                )
                1 -> AssignmentsListSection(
                    assignments = assignments,
                    onOpenCreateAssignment = { showCreateAssignmentDialog = true }
                )
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Dialogs
    if (showCreateClassDialog) {
        CreateClassModal(
            onDismiss = { showCreateClassDialog = false },
            onCreate = { className, subject, bannerUrl, callback ->
                viewModel.createClass(className, subject, bannerUrl) { success, msg ->
                    callback(success, msg)
                    if (success) showCreateClassDialog = false
                }
            }
        )
    }

    if (showCreateAssignmentDialog) {
        CreateAssignmentModal(
            classes = classes,
            onDismiss = { showCreateAssignmentDialog = false },
            onCreate = { classId, className, title, desc, dueDate, points, attachmentUrl, callback ->
                viewModel.createAssignment(
                    classId = classId,
                    className = className,
                    title = title,
                    description = desc,
                    dueDate = dueDate,
                    points = points,
                    attachmentUrl = attachmentUrl
                ) { success, msg ->
                    callback(success, msg)
                    if (success) showCreateAssignmentDialog = false
                }
            }
        )
    }
}

@Composable
private fun ClassesListSection(
    classes: List<SchoolClassEntity>,
    assignments: List<AssignmentEntity>,
    onNavigateToClassDetails: ((String) -> Unit)?,
    onOpenCreateClass: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredClasses = classes.filter { schoolClass ->
        searchQuery.isBlank() ||
                schoolClass.className.contains(searchQuery, ignoreCase = true) ||
                schoolClass.subject.contains(searchQuery, ignoreCase = true) ||
                schoolClass.joinCode.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (classes.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("input_search_classes"),
                placeholder = { Text("Search classes by name or subject...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Classes") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        if (classes.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_classes_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Class,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No Classes Created Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create your first class to share join codes with students and post assignments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onOpenCreateClass,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create Class Now")
                    }
                }
            }
        } else if (filteredClasses.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_classes_search_card")
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
                    Text(
                        text = "No Classes Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "No classes match \"$searchQuery\". Try searching by class name or subject.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredClasses) { schoolClass ->
                    val classAssignmentCount = assignments.count { it.classId == schoolClass.id }

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToClassDetails?.invoke(schoolClass.id) }
                        .testTag("teacher_class_item_${schoolClass.id}")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PrimaryBlue.copy(alpha = 0.15f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Class,
                                            contentDescription = null,
                                            tint = PrimaryBlue
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = schoolClass.className,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Subject: ${schoolClass.subject}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Code,
                                        contentDescription = "Join Code",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = schoolClass.joinCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Assignments: $classAssignmentCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = SecondaryTeal,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Teacher: ${schoolClass.teacherName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun AssignmentsListSection(
    assignments: List<AssignmentEntity>,
    onOpenCreateAssignment: () -> Unit
) {
    if (assignments.isEmpty()) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("empty_assignments_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Assignment,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = SecondaryTeal
                )
                Text(
                    text = "No Assignments Posted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Create new assignments for your classes to engage students.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onOpenCreateAssignment,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                ) {
                    Text("Create Assignment")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(assignments) { assignment ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("assignment_card_${assignment.id}")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = assignment.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Class: ${assignment.className}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SecondaryTeal.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${assignment.points} Pts",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryTeal
                                )
                            }
                        }

                        if (assignment.description.isNotBlank()) {
                            Text(
                                text = assignment.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = "Due Date",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Due: ${assignment.dueDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (assignment.attachmentUrl.isNotBlank()) {
                                Text(
                                    text = "Attachment Attached",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateClassModal(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, (Boolean, String) -> Unit) -> Unit
) {
    var className by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var bannerUrl by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Class", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_modal_class_name")
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Department *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_modal_subject")
                )

                OutlinedTextField(
                    value = bannerUrl,
                    onValueChange = { bannerUrl = it },
                    label = { Text("Banner Image URL (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (className.isBlank() || subject.isBlank()) {
                        errorMsg = "Please fill in all required fields."
                        return@Button
                    }
                    onCreate(className, subject, bannerUrl) { success, msg ->
                        if (!success) errorMsg = msg
                    }
                },
                modifier = Modifier.testTag("btn_modal_save_class")
            ) {
                Text("Create Class")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CreateAssignmentModal(
    classes: List<SchoolClassEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, Int, String, (Boolean, String) -> Unit) -> Unit
) {
    var selectedClass by remember { mutableStateOf(classes.firstOrNull()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("2026-08-25") }
    var pointsStr by remember { mutableStateOf("100") }
    var attachmentUrl by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Assignment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                if (classes.isEmpty()) {
                    Text(
                        text = "You must create a class before posting an assignment.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Select Target Class:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    classes.forEach { schoolClass ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedClass = schoolClass }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedClass?.id == schoolClass.id,
                                onClick = { selectedClass = schoolClass }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${schoolClass.className} (${schoolClass.subject})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Assignment Title *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_modal_assignment_title")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Instructions / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Due Date") },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = pointsStr,
                            onValueChange = { pointsStr = it },
                            label = { Text("Points") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = attachmentUrl,
                        onValueChange = { attachmentUrl = it },
                        label = { Text("Attachment URL (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (classes.isNotEmpty()) {
                Button(
                    onClick = {
                        val cls = selectedClass
                        if (cls == null || title.isBlank()) {
                            errorMsg = "Please select a class and enter a title."
                            return@Button
                        }
                        val pts = pointsStr.toIntOrNull() ?: 100
                        onCreate(
                            cls.id,
                            cls.className,
                            title,
                            description,
                            dueDate,
                            pts,
                            attachmentUrl
                        ) { success, msg ->
                            if (!success) errorMsg = msg
                        }
                    },
                    modifier = Modifier.testTag("btn_modal_save_assignment")
                ) {
                    Text("Post Assignment")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

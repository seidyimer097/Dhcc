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
import androidx.compose.material.icons.outlined.School
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
import com.example.data.SubmissionEntity
import com.example.data.UserEntity
import com.example.ui.StudentDashboardViewModel
import com.example.ui.UiState
import com.example.ui.components.GlassCard
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun StudentDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentDashboardViewModel = viewModel(),
    onNavigateToClassDetails: ((String) -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val enrolledClasses by viewModel.enrolledClasses.collectAsStateWithLifecycle()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val submissions by viewModel.submissions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val profileState by viewModel.studentProfileState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    StudentDashboardContent(
        modifier = modifier,
        currentUser = currentUser,
        profileState = profileState,
        enrolledClasses = enrolledClasses,
        assignments = assignments,
        submissions = submissions,
        isLoading = isLoading,
        snackbarHostState = snackbarHostState,
        onRefresh = { viewModel.loadStudentData() },
        onJoinClass = { code, callback -> viewModel.joinClassWithCode(code, callback) },
        onSubmitWork = { assignmentId, title, classId, fileUrl, comment, callback ->
            viewModel.submitWork(assignmentId, title, classId, fileUrl, comment, callback)
        },
        onNavigateToClassDetails = onNavigateToClassDetails
    )
}

@Composable
fun StudentDashboardContent(
    modifier: Modifier = Modifier,
    currentUser: UserEntity?,
    profileState: UiState<UserEntity> = UiState.Idle,
    enrolledClasses: List<SchoolClassEntity>,
    assignments: List<AssignmentEntity>,
    submissions: List<SubmissionEntity>,
    isLoading: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onRefresh: () -> Unit = {},
    onJoinClass: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onSubmitWork: (String, String, String, String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onNavigateToClassDetails: ((String) -> Unit)? = null
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0: Enrolled Classes, 1: Assignment Deadlines
    var showJoinModal by remember { mutableStateOf(false) }
    var selectedAssignmentForSubmission by remember { mutableStateOf<AssignmentEntity?>(null) }

    Box(modifier = modifier.fillMaxSize().testTag("student_dashboard_screen")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Status Bar
            when (profileState) {
                is UiState.Loading -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Syncing Student Profile & Enrollments...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = profileState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                else -> {}
            }

            // Student Header Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_header_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SecondaryTeal.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = "Student Profile",
                                    tint = SecondaryTeal,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Student Portal",
                                style = MaterialTheme.typography.labelMedium,
                                color = SecondaryTeal,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser?.name ?: "Student Account",
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
                        onClick = onRefresh,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("btn_refresh_student_data")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Quick Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_enrolled_classes"),
                    shape = RoundedCornerShape(16.dp),
                    color = SecondaryTeal.copy(alpha = 0.12f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = enrolledClasses.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryTeal
                        )
                        Text(
                            text = "Enrolled Classes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_upcoming_assignments"),
                    shape = RoundedCornerShape(16.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = assignments.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Assignments",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_completed_submissions"),
                    shape = RoundedCornerShape(16.dp),
                    color = StatusSuccess.copy(alpha = 0.12f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = submissions.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                        Text(
                            text = "Submissions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tabs & Join Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabRow(
                    selectedTabIndex = activeSubTab,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = SecondaryTeal
                ) {
                    Tab(
                        selected = activeSubTab == 0,
                        onClick = { activeSubTab = 0 },
                        text = {
                            Text(
                                "Enrolled (${enrolledClasses.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        text = {
                            Text(
                                "Deadlines (${assignments.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showJoinModal = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                    modifier = Modifier.testTag("btn_join_class_dialog")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Join Class",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Join")
                }
            }

            // Tab Content
            when (activeSubTab) {
                0 -> EnrolledClassesSection(
                    classes = enrolledClasses,
                    onNavigateToClassDetails = onNavigateToClassDetails,
                    onOpenJoin = { showJoinModal = true }
                )
                1 -> AssignmentDeadlinesSection(
                    assignments = assignments,
                    submissions = submissions,
                    onSubmitClick = { assignment -> selectedAssignmentForSubmission = assignment }
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

    // Join Class Dialog
    if (showJoinModal) {
        JoinClassModal(
            onDismiss = { showJoinModal = false },
            onJoin = { code, callback ->
                onJoinClass(code) { success, msg ->
                    callback(success, msg)
                    if (success) showJoinModal = false
                }
            }
        )
    }

    // Submit Work Dialog
    selectedAssignmentForSubmission?.let { assignment ->
        SubmitWorkModal(
            assignment = assignment,
            onDismiss = { selectedAssignmentForSubmission = null },
            onSubmit = { fileUrl, comment, callback ->
                onSubmitWork(assignment.id, assignment.title, assignment.classId, fileUrl, comment) { success, msg ->
                    callback(success, msg)
                    if (success) selectedAssignmentForSubmission = null
                }
            }
        )
    }
}

@Composable
private fun EnrolledClassesSection(
    classes: List<SchoolClassEntity>,
    onNavigateToClassDetails: ((String) -> Unit)?,
    onOpenJoin: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredClasses = classes.filter { schoolClass ->
        searchQuery.isBlank() ||
                schoolClass.className.contains(searchQuery, ignoreCase = true) ||
                schoolClass.subject.contains(searchQuery, ignoreCase = true) ||
                schoolClass.teacherName.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (classes.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("input_search_student_classes"),
                placeholder = { Text("Search enrolled classes by name or subject...") },
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
                    .testTag("empty_enrolled_classes_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = SecondaryTeal
                    )
                    Text(
                        text = "No Classes Enrolled",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ask your teacher for a 6-digit Join Code to enroll in a class.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onOpenJoin,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                    ) {
                        Text("Join Class Now")
                    }
                }
            }
        } else if (filteredClasses.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_student_classes_search_card")
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
                        text = "No enrolled classes match \"$searchQuery\". Try searching by class name or subject.",
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
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToClassDetails?.invoke(schoolClass.id) }
                        .testTag("enrolled_class_item_${schoolClass.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SecondaryTeal.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Class,
                                        contentDescription = null,
                                        tint = SecondaryTeal
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
                                    text = "${schoolClass.subject} • Teacher: ${schoolClass.teacherName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun AssignmentDeadlinesSection(
    assignments: List<AssignmentEntity>,
    submissions: List<SubmissionEntity>,
    onSubmitClick: (AssignmentEntity) -> Unit
) {
    if (assignments.isEmpty()) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("empty_student_assignments_card")
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
                    tint = PrimaryBlue
                )
                Text(
                    text = "No Upcoming Deadlines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All caught up! Any new assignments from your teachers will appear here.",
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
            items(assignments) { assignment ->
                val submission = submissions.firstOrNull { it.assignmentId == assignment.id }

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_assignment_item_${assignment.id}")
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
                                    color = SecondaryTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (submission != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StatusSuccess.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Submitted (${submission.grade})",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusSuccess
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StatusWarning.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Pending Work",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusWarning
                                    )
                                }
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

                            Button(
                                onClick = { onSubmitClick(assignment) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (submission != null) MaterialTheme.colorScheme.surfaceVariant else SecondaryTeal,
                                    contentColor = if (submission != null) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_submit_work_${assignment.id}")
                            ) {
                                Text(
                                    text = if (submission != null) "Resubmit" else "Submit Work",
                                    fontSize = 12.sp,
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
private fun JoinClassModal(
    onDismiss: () -> Unit,
    onJoin: (String, (Boolean, String) -> Unit) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Class with Code", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Text(
                    text = "Enter the 6-digit Join Code provided by your teacher:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Join Code") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_join_code")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank()) {
                        errorMsg = "Please enter a valid join code."
                        return@Button
                    }
                    onJoin(code) { success, msg ->
                        if (!success) errorMsg = msg
                    }
                },
                modifier = Modifier.testTag("btn_confirm_join")
            ) {
                Text("Join Class")
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
private fun SubmitWorkModal(
    assignment: AssignmentEntity,
    onDismiss: () -> Unit,
    onSubmit: (String, String, (Boolean, String) -> Unit) -> Unit
) {
    var fileUrl by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Work for ${assignment.title}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Text(
                    text = "Class: ${assignment.className}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryTeal,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = fileUrl,
                    onValueChange = { fileUrl = it },
                    label = { Text("Submission File / Google Drive URL *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_submission_file_url")
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comments for Teacher (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileUrl.isBlank()) {
                        errorMsg = "Submission file URL is required."
                        return@Button
                    }
                    onSubmit(fileUrl, comment) { success, msg ->
                        if (!success) errorMsg = msg
                    }
                },
                modifier = Modifier.testTag("btn_confirm_submit_work")
            ) {
                Text("Submit Work")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

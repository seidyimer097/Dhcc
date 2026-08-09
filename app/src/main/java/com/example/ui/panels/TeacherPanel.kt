package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Assignment
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
import com.example.data.AssignmentEntity
import com.example.data.SchoolClassEntity
import com.example.data.SubmissionEntity
import com.example.data.UserEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StatusSuccess

@Composable
fun TeacherPanel(
    currentUser: UserEntity?,
    classes: List<SchoolClassEntity>,
    assignments: List<AssignmentEntity>,
    allSubmissions: List<SubmissionEntity>,
    onCreateClass: (String, String, String, (Boolean, String) -> Unit) -> Unit,
    onCreateAssignment: (String, String, String, String, String, Int, String, (Boolean, String) -> Unit) -> Unit,
    onUploadToCloudinary: (String, (String) -> Unit) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0: My Classes, 1: Assignments & Submissions
    var showCreateClassDialog by remember { mutableStateOf(false) }
    var showCreateAssignmentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Teacher Profile Header Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Teacher Workspace",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentUser?.name ?: "Teacher Account",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    Button(
                        onClick = { showCreateClassDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_create_class")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Class", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("My Classes (${classes.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Assignments (${assignments.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeTab) {
            0 -> TeacherClassesTab(classes = classes)
            1 -> TeacherAssignmentsTab(
                assignments = assignments,
                submissions = allSubmissions,
                onOpenCreateAssignment = { showCreateAssignmentDialog = true }
            )
        }
    }

    // Dialog: Create Class
    if (showCreateClassDialog) {
        CreateClassDialog(
            onDismiss = { showCreateClassDialog = false },
            onCreate = onCreateClass,
            onUploadToCloudinary = onUploadToCloudinary
        )
    }

    // Dialog: Create Assignment
    if (showCreateAssignmentDialog) {
        CreateAssignmentDialog(
            classes = classes,
            onDismiss = { showCreateAssignmentDialog = false },
            onCreate = onCreateAssignment,
            onUploadToCloudinary = onUploadToCloudinary
        )
    }
}

@Composable
private fun TeacherClassesTab(classes: List<SchoolClassEntity>) {
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
                    .testTag("input_search_classes_panel"),
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
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Class, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No classes created yet", fontWeight = FontWeight.Bold)
                    Text("Click '+ Class' above to create a class and generate a 6-character Join Code.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        } else if (filteredClasses.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth().testTag("empty_classes_panel_search")) {
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
                    Text("No classes found", fontWeight = FontWeight.Bold)
                    Text("No classes match \"$searchQuery\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredClasses) { schoolClass ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("teacher_class_${schoolClass.id}")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = schoolClass.className,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Subject: ${schoolClass.subject}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            // 6-Character Join Code Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryBlue.copy(alpha = 0.2f),
                                border = ButtonDefaults.outlinedButtonBorder
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VpnKey, contentDescription = "Join Code", tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Code: ${schoolClass.joinCode}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }

                        if (schoolClass.bannerUrl.isNotBlank()) {
                            Text(
                                text = "Cloudinary Banner: ${schoolClass.bannerUrl.take(45)}...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
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
private fun TeacherAssignmentsTab(
    assignments: List<AssignmentEntity>,
    submissions: List<SubmissionEntity>,
    onOpenCreateAssignment: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Class Assignments",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Button(
                onClick = onOpenCreateAssignment,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_create_assignment")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Assignment")
            }
        }

        if (assignments.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("No assignments created yet. Click 'New Assignment' above.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(assignments) { assignment ->
                    val assignmentSubmissions = submissions.filter { it.assignmentId == assignment.id }

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = assignment.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Class: ${assignment.className}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SecondaryTeal.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${assignment.points} Pts",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SecondaryTeal
                                    )
                                }
                            }

                            Text(
                                text = assignment.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Due: ${assignment.dueDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "Submissions: ${assignmentSubmissions.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StatusSuccess
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
private fun CreateClassDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, (Boolean, String) -> Unit) -> Unit,
    onUploadToCloudinary: (String, (String) -> Unit) -> Unit
) {
    var className by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var bannerUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Class") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("input_class_name")
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject *") },
                    modifier = Modifier.fillMaxWidth().testTag("input_subject")
                )
                OutlinedTextField(
                    value = bannerUrl,
                    onValueChange = { bannerUrl = it },
                    label = { Text("Banner Image URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onUploadToCloudinary("class_banner_${System.currentTimeMillis()}.jpg") { url ->
                            bannerUrl = url
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_upload_banner")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Banner to Cloudinary")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(className, subject, bannerUrl) { success, _ ->
                        if (success) onDismiss()
                    }
                },
                modifier = Modifier.testTag("btn_save_class")
            ) {
                Text("Create Class")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateAssignmentDialog(
    classes: List<SchoolClassEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, Int, String, (Boolean, String) -> Unit) -> Unit,
    onUploadToCloudinary: (String, (String) -> Unit) -> Unit
) {
    var selectedClass by remember { mutableStateOf(classes.firstOrNull()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("2026-08-25") }
    var pointsStr by remember { mutableStateOf("100") }
    var attachmentUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Assignment") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Select Class:", style = MaterialTheme.typography.labelMedium)
                    classes.forEach { c ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedClass = c }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = selectedClass?.id == c.id, onClick = { selectedClass = c })
                            Text(c.className, fontSize = 14.sp)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Assignment Title *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_assignment_title")
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Instructions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
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
                }
                item {
                    Button(
                        onClick = {
                            onUploadToCloudinary("attachment_${System.currentTimeMillis()}.pdf") { url ->
                                attachmentUrl = url
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_upload_attachment")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Attachment to Cloudinary")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = selectedClass
                    if (target != null) {
                        val points = pointsStr.toIntOrNull() ?: 100
                        onCreate(target.id, target.className, title, description, dueDate, points, attachmentUrl) { success, _ ->
                            if (success) onDismiss()
                        }
                    }
                },
                modifier = Modifier.testTag("btn_save_assignment")
            ) {
                Text("Publish Assignment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

package com.example.ui.panels

import androidx.compose.runtime.Composable
import com.example.data.AssignmentEntity
import com.example.data.SchoolClassEntity
import com.example.data.SubmissionEntity
import com.example.data.UserEntity
import com.example.ui.UiState
import com.example.ui.screens.StudentDashboardContent

@Composable
fun StudentPanel(
    currentUser: UserEntity?,
    studentProfileState: UiState<UserEntity>,
    enrolledClasses: List<SchoolClassEntity>,
    assignments: List<AssignmentEntity>,
    submissions: List<SubmissionEntity>,
    onJoinClass: (String, (Boolean, String) -> Unit) -> Unit,
    onSubmitWork: (String, String, String, String, String, (Boolean, String) -> Unit) -> Unit,
    onUploadToCloudinary: (String, (String) -> Unit) -> Unit
) {
    StudentDashboardContent(
        currentUser = currentUser,
        profileState = studentProfileState,
        enrolledClasses = enrolledClasses,
        assignments = assignments,
        submissions = submissions,
        onJoinClass = onJoinClass,
        onSubmitWork = { assignmentId, title, classId, fileUrl, comment, callback ->
            onSubmitWork(assignmentId, title, classId, fileUrl, comment, callback)
        }
    )
}

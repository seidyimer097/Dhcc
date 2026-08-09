package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AssignmentEntity
import com.example.data.AuthHelper
import com.example.data.DatabaseHelper
import com.example.data.EnrollmentEntity
import com.example.data.SchoolClassEntity
import com.example.data.SubmissionEntity
import com.example.data.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for StudentDashboardScreen handling fetching student's enrolled classes,
 * assignment deadlines, submissions, joining classes with join codes, and submitting work via DatabaseHelper.
 */
class StudentDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = DatabaseHelper.getInstance()
    private val authHelper = AuthHelper.getInstance()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _enrolledClasses = MutableStateFlow<List<SchoolClassEntity>>(emptyList())
    val enrolledClasses: StateFlow<List<SchoolClassEntity>> = _enrolledClasses.asStateFlow()

    private val _assignments = MutableStateFlow<List<AssignmentEntity>>(emptyList())
    val assignments: StateFlow<List<AssignmentEntity>> = _assignments.asStateFlow()

    private val _submissions = MutableStateFlow<List<SubmissionEntity>>(emptyList())
    val submissions: StateFlow<List<SubmissionEntity>> = _submissions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _studentProfileState = MutableStateFlow<UiState<UserEntity>>(UiState.Idle)
    val studentProfileState: StateFlow<UiState<UserEntity>> = _studentProfileState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadStudentData()
    }

    fun loadStudentData(studentId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _studentProfileState.value = UiState.Loading
            try {
                val uid = studentId ?: authHelper.currentUserId
                if (uid != null) {
                    val user = dbHelper.getUserById(uid)
                    _currentUser.value = user
                    if (user != null) {
                        _studentProfileState.value = UiState.Success(user)
                    } else {
                        _studentProfileState.value = UiState.Error("Student profile not found.")
                    }

                    // Fetch enrollments and classes
                    val enrollments = dbHelper.fetchEnrollmentsByStudent(uid)
                    val enrolledClassIds = enrollments.map { it.classId }.toSet()
                    val allClasses = dbHelper.fetchAllClasses()
                    val myClasses = allClasses.filter { it.id in enrolledClassIds }
                    _enrolledClasses.value = myClasses

                    // Fetch assignments for enrolled classes
                    val allAssignments = mutableListOf<AssignmentEntity>()
                    enrolledClassIds.forEach { classId ->
                        val classAssignments = dbHelper.fetchAssignmentsByClass(classId)
                        allAssignments.addAll(classAssignments)
                    }
                    _assignments.value = allAssignments.distinctBy { it.id }

                    // Fetch student submissions
                    val studentSubmissions = dbHelper.fetchSubmissionsByStudent(uid)
                    _submissions.value = studentSubmissions
                } else {
                    _studentProfileState.value = UiState.Error("Student not authenticated.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _studentProfileState.value = UiState.Error(e.localizedMessage ?: "Error loading student data")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinClassWithCode(code: String, onResult: (Boolean, String) -> Unit) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            onResult(false, "Please enter a valid join code.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val student = _currentUser.value ?: dbHelper.getUserById(authHelper.currentUserId ?: "")
                if (student == null) {
                    onResult(false, "Authentication required to join classes.")
                    return@launch
                }

                val allClasses = dbHelper.fetchAllClasses()
                val targetClass = allClasses.firstOrNull { it.joinCode.trim().uppercase() == cleanCode }
                if (targetClass == null) {
                    onResult(false, "Invalid Join Code. Class not found.")
                    return@launch
                }

                val existingEnrollments = dbHelper.fetchEnrollmentsByStudent(student.id)
                if (existingEnrollments.any { it.classId == targetClass.id }) {
                    onResult(false, "You are already enrolled in ${targetClass.className}.")
                    return@launch
                }

                val enrollment = EnrollmentEntity(
                    id = "e_${UUID.randomUUID().toString().take(8)}",
                    classId = targetClass.id,
                    studentId = student.id
                )

                val success = dbHelper.saveEnrollment(enrollment)
                if (success) {
                    _toastMessage.value = "Successfully joined ${targetClass.className}!"
                    loadStudentData(student.id)
                    onResult(true, "Successfully joined ${targetClass.className}!")
                } else {
                    onResult(false, "Failed to join class. Please try again.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Error joining class")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitWork(
        assignmentId: String,
        assignmentTitle: String,
        classId: String,
        fileUrl: String,
        comment: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val student = _currentUser.value ?: dbHelper.getUserById(authHelper.currentUserId ?: "")
                if (student == null) {
                    onResult(false, "Authentication required to submit work.")
                    return@launch
                }

                val submission = SubmissionEntity(
                    id = "s_${UUID.randomUUID().toString().take(8)}",
                    assignmentId = assignmentId,
                    assignmentTitle = assignmentTitle,
                    classId = classId,
                    studentId = student.id,
                    studentName = student.name,
                    fileUrl = fileUrl,
                    comment = comment.trim(),
                    submittedAt = System.currentTimeMillis(),
                    grade = "Pending"
                )

                val success = dbHelper.saveSubmission(submission)
                if (success) {
                    _toastMessage.value = "Submission uploaded for $assignmentTitle!"
                    loadStudentData(student.id)
                    onResult(true, "Work submitted successfully!")
                } else {
                    onResult(false, "Failed to submit work to Firestore.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Error submitting work")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}

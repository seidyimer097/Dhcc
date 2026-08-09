package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel structure to expose data from DatabaseHelper (Firestore)
 * to Jetpack Compose UI components.
 */
class DatabaseViewModel(application: Application) : AndroidViewModel(application) {

    val dbHelper: DatabaseHelper = DatabaseHelper.getInstance()

    // Exposed StateFlows for Compose UI
    private val _firestoreUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val firestoreUsers: StateFlow<List<UserEntity>> = _firestoreUsers.asStateFlow()

    private val _firestoreClasses = MutableStateFlow<List<SchoolClassEntity>>(emptyList())
    val firestoreClasses: StateFlow<List<SchoolClassEntity>> = _firestoreClasses.asStateFlow()

    private val _firestoreAssignments = MutableStateFlow<List<AssignmentEntity>>(emptyList())
    val firestoreAssignments: StateFlow<List<AssignmentEntity>> = _firestoreAssignments.asStateFlow()

    private val _firestoreSubmissions = MutableStateFlow<List<SubmissionEntity>>(emptyList())
    val firestoreSubmissions: StateFlow<List<SubmissionEntity>> = _firestoreSubmissions.asStateFlow()

    private val _firestoreSettings = MutableStateFlow<SystemSettingsEntity?>(null)
    val firestoreSettings: StateFlow<SystemSettingsEntity?> = _firestoreSettings.asStateFlow()

    private val _adminStats = MutableStateFlow(AdminStats())
    val adminStats: StateFlow<AdminStats> = _adminStats.asStateFlow()

    private val _syncState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val syncState: StateFlow<UiState<String>> = _syncState.asStateFlow()

    private val _isCloudSynced = MutableStateFlow(false)
    val isCloudSynced: StateFlow<Boolean> = _isCloudSynced.asStateFlow()

    init {
        refreshAllFirestoreData()
    }

    // --- REFRESH & FETCH ALL DATA FROM FIRESTORE ---

    fun refreshAllFirestoreData() {
        viewModelScope.launch {
            _syncState.value = UiState.Loading
            try {
                val users = dbHelper.fetchAllUsers()
                val classes = dbHelper.fetchAllClasses()
                val assignments = dbHelper.fetchAllAssignments()
                val settings = dbHelper.fetchSystemSettings()
                val stats = dbHelper.fetchAdminStats()

                _firestoreUsers.value = users
                _firestoreClasses.value = classes
                _firestoreAssignments.value = assignments
                _firestoreSettings.value = settings
                _adminStats.value = stats
                _isCloudSynced.value = true
                _syncState.value = UiState.Success("Firestore data loaded successfully")
            } catch (e: Exception) {
                _syncState.value = UiState.Error(e.message ?: "Failed to refresh Firestore data")
            }
        }
    }

    fun fetchAdminStats(onResult: ((AdminStats) -> Unit)? = null) {
        viewModelScope.launch {
            val stats = dbHelper.fetchAdminStats()
            _adminStats.value = stats
            onResult?.invoke(stats)
        }
    }

    // --- USER OPERATIONS ---

    fun saveUser(user: UserEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = dbHelper.saveUser(user)
            if (success) {
                _firestoreUsers.value = _firestoreUsers.value.filter { it.id != user.id } + user
            }
            onComplete?.invoke(success)
        }
    }

    fun fetchUserById(userId: String, onResult: (UserEntity?) -> Unit) {
        viewModelScope.launch {
            val user = dbHelper.getUserById(userId)
            onResult(user)
        }
    }

    fun fetchUsersByRole(role: String, onResult: (List<UserEntity>) -> Unit) {
        viewModelScope.launch {
            val users = dbHelper.fetchUsersByRole(role)
            onResult(users)
        }
    }

    fun checkAdminExists(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exists = dbHelper.hasAdminUser()
            onResult(exists)
        }
    }

    fun updateUserRole(userId: String, newRole: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = dbHelper.updateUserRole(userId, newRole)
            if (success) {
                _firestoreUsers.value = _firestoreUsers.value.map {
                    if (it.id == userId) it.copy(role = newRole) else it
                }
            }
            onComplete?.invoke(success)
        }
    }

    // --- CLASS OPERATIONS ---

    fun saveClass(schoolClass: SchoolClassEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = dbHelper.saveClass(schoolClass)
            if (success) {
                _firestoreClasses.value = _firestoreClasses.value.filter { it.id != schoolClass.id } + schoolClass
            }
            onComplete?.invoke(success)
        }
    }

    fun fetchClassByJoinCode(code: String, onResult: (SchoolClassEntity?) -> Unit) {
        viewModelScope.launch {
            val schoolClass = dbHelper.fetchClassByJoinCode(code)
            onResult(schoolClass)
        }
    }

    fun fetchClassesByTeacher(teacherId: String, onResult: (List<SchoolClassEntity>) -> Unit) {
        viewModelScope.launch {
            val classes = dbHelper.fetchClassesByTeacher(teacherId)
            onResult(classes)
        }
    }

    // --- ASSIGNMENT OPERATIONS ---

    fun saveAssignment(assignment: AssignmentEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = dbHelper.saveAssignment(assignment)
            if (success) {
                _firestoreAssignments.value = _firestoreAssignments.value.filter { it.id != assignment.id } + assignment
            }
            onComplete?.invoke(success)
        }
    }

    fun fetchAssignmentsByClass(classId: String, onResult: (List<AssignmentEntity>) -> Unit) {
        viewModelScope.launch {
            val assignments = dbHelper.fetchAssignmentsByClass(classId)
            onResult(assignments)
        }
    }

    // --- ENROLLMENT OPERATIONS ---

    fun saveEnrollment(enrollment: EnrollmentEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = dbHelper.saveEnrollment(enrollment)
            onComplete?.invoke(success)
        }
    }

    fun fetchEnrollmentsByStudent(studentId: String, onResult: (List<EnrollmentEntity>) -> Unit) {
        viewModelScope.launch {
            val enrollments = dbHelper.fetchEnrollmentsByStudent(studentId)
            onResult(enrollments)
        }
    }

    // --- SUBMISSION OPERATIONS ---

    fun saveSubmission(submission: SubmissionEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = dbHelper.saveSubmission(submission)
            if (success) {
                _firestoreSubmissions.value = _firestoreSubmissions.value.filter { it.id != submission.id } + submission
            }
            onComplete?.invoke(success)
        }
    }

    fun fetchSubmissionsByAssignment(assignmentId: String, onResult: (List<SubmissionEntity>) -> Unit) {
        viewModelScope.launch {
            val submissions = dbHelper.fetchSubmissionsByAssignment(assignmentId)
            onResult(submissions)
        }
    }

    fun fetchSubmissionsByStudent(studentId: String, onResult: (List<SubmissionEntity>) -> Unit) {
        viewModelScope.launch {
            val submissions = dbHelper.fetchSubmissionsByStudent(studentId)
            onResult(submissions)
        }
    }

    fun updateSubmissionGrade(submissionId: String, grade: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = dbHelper.updateSubmissionGrade(submissionId, grade)
            if (success) {
                _firestoreSubmissions.value = _firestoreSubmissions.value.map {
                    if (it.id == submissionId) it.copy(grade = grade) else it
                }
            }
            onComplete?.invoke(success)
        }
    }

    // --- SYSTEM SETTINGS OPERATIONS ---

    fun saveSystemSettings(settings: SystemSettingsEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = dbHelper.saveSystemSettings(settings)
            if (success) {
                _firestoreSettings.value = settings
            }
            onComplete?.invoke(success)
        }
    }

    fun fetchSystemSettings(onResult: ((SystemSettingsEntity?) -> Unit)? = null) {
        viewModelScope.launch {
            val settings = dbHelper.fetchSystemSettings()
            _firestoreSettings.value = settings
            onResult?.invoke(settings)
        }
    }
}

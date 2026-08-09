package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

enum class PanelRole {
    ADMIN, TEACHER, STUDENT
}

class SchoolViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SchoolRepository
    private val dbHelper = DatabaseHelper.getInstance()

    init {
        val dao = SchoolDatabase.getDatabase(application).schoolDao()
        repository = SchoolRepository(dao)
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    // Active Panel Role
    private val _activeRole = MutableStateFlow(PanelRole.ADMIN)
    val activeRole: StateFlow<PanelRole> = _activeRole.asStateFlow()

    // Active User
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Dark Mode Toggle
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Toast / Message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Student Profile Loading Retry State
    private val _studentProfileState = MutableStateFlow<UiState<UserEntity>>(UiState.Idle)
    val studentProfileState: StateFlow<UiState<UserEntity>> = _studentProfileState.asStateFlow()

    // Flow Queries from Repository
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClasses: StateFlow<List<SchoolClassEntity>> = repository.allClasses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAssignments: StateFlow<List<AssignmentEntity>> = repository.allAssignments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubmissions: StateFlow<List<SubmissionEntity>> = repository.allSubmissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemSettings: StateFlow<SystemSettingsEntity> = repository.systemSettings
        .map { it ?: SystemSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SystemSettingsEntity())

    // Admin Dashboard Stats Flow
    val adminStats: StateFlow<AdminStats> = combine(allUsers, allClasses, allAssignments, allSubmissions) { users, classes, assignments, submissions ->
        AdminStats(
            totalTeachers = users.count { it.role.equals("teacher", ignoreCase = true) },
            totalStudents = users.count { it.role.equals("student", ignoreCase = true) },
            totalClasses = classes.size,
            totalAssignments = assignments.size,
            totalSubmissions = submissions.size,
            totalAdmins = users.count { it.role.equals("admin", ignoreCase = true) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminStats())

    // Filtered Teacher Classes
    val teacherClasses: StateFlow<List<SchoolClassEntity>> = combine(allClasses, currentUser) { classes, user ->
        if (user?.role == "teacher") {
            classes.filter { it.teacherId == user.id }
        } else {
            classes
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Teacher Assignments
    val teacherAssignments: StateFlow<List<AssignmentEntity>> = combine(allAssignments, currentUser) { assignments, user ->
        if (user?.role == "teacher") {
            assignments.filter { it.teacherId == user.id }
        } else {
            assignments
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Student Enrollments & Classes
    val studentEnrollments: StateFlow<List<EnrollmentEntity>> = currentUser.flatMapLatest { user ->
        if (user != null && user.role == "student") {
            repository.getEnrollmentsByStudent(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentClasses: StateFlow<List<SchoolClassEntity>> = combine(allClasses, studentEnrollments) { classes, enrollments ->
        val enrolledClassIds = enrollments.map { it.classId }.toSet()
        classes.filter { enrolledClassIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentSubmissions: StateFlow<List<SubmissionEntity>> = currentUser.flatMapLatest { user ->
        if (user != null && user.role == "student") {
            repository.getSubmissionsByStudent(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Exist Check
    private val _adminExists = MutableStateFlow(false)
    val adminExists: StateFlow<Boolean> = _adminExists.asStateFlow()

    // Real-Time Notifications State
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    val unreadNotificationsCount: StateFlow<Int> = _notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var assignmentListenerReg: com.google.firebase.firestore.ListenerRegistration? = null
    private var submissionListenerReg: com.google.firebase.firestore.ListenerRegistration? = null
    private var notificationListenerReg: com.google.firebase.firestore.ListenerRegistration? = null

    private val seenAssignmentIds = mutableSetOf<String>()
    private val seenSubmissionIds = mutableSetOf<String>()
    private var isFirstAssignmentSnapshot = true
    private var isFirstSubmissionSnapshot = true

    init {
        viewModelScope.launch {
            allUsers.collect { users ->
                _adminExists.value = users.any { it.role == "admin" }
            }
        }

        viewModelScope.launch {
            _currentUser.collect { user ->
                setupRealtimeListeners(user)
            }
        }
    }

    private fun setupRealtimeListeners(user: UserEntity?) {
        assignmentListenerReg?.remove()
        submissionListenerReg?.remove()
        notificationListenerReg?.remove()

        if (user == null) return

        // 1. Listen to User Notifications in Firestore
        notificationListenerReg = dbHelper.listenToUserNotifications(
            userId = user.id,
            userRole = user.role
        ) { updatedNotifs ->
            _notifications.value = updatedNotifs
        }

        // 2. Real-Time Snapshot Listener for New Assignments (Alerts Students)
        if (user.role == "student" || user.role == "all") {
            isFirstAssignmentSnapshot = true
            assignmentListenerReg = dbHelper.listenToAssignmentsRealtime { assignment ->
                if (!seenAssignmentIds.contains(assignment.id)) {
                    seenAssignmentIds.add(assignment.id)
                    if (!isFirstAssignmentSnapshot) {
                        showToast("🔔 New Assignment: '${assignment.title}' in ${assignment.className}")
                    }
                }
            }
            viewModelScope.launch {
                delay(1500)
                isFirstAssignmentSnapshot = false
            }
        }

        // 3. Real-Time Snapshot Listener for New Student Submissions (Alerts Teachers)
        if (user.role == "teacher" || user.role == "admin") {
            isFirstSubmissionSnapshot = true
            submissionListenerReg = dbHelper.listenToSubmissionsRealtime { submission ->
                if (!seenSubmissionIds.contains(submission.id)) {
                    seenSubmissionIds.add(submission.id)
                    if (!isFirstSubmissionSnapshot) {
                        showToast("🔔 New Submission from ${submission.studentName} for '${submission.assignmentTitle}'")
                    }
                }
            }
            viewModelScope.launch {
                delay(1500)
                isFirstSubmissionSnapshot = false
            }
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun setPanelRole(role: PanelRole) {
        _activeRole.value = role
        // Default select an existing demo user for the active role if current user doesn't match
        viewModelScope.launch {
            val users = allUsers.value
            val matchRoleStr = when (role) {
                PanelRole.ADMIN -> "admin"
                PanelRole.TEACHER -> "teacher"
                PanelRole.STUDENT -> "student"
            }
            if (_currentUser.value?.role != matchRoleStr) {
                val foundUser = users.firstOrNull { it.role == matchRoleStr }
                _currentUser.value = foundUser
                if (role == PanelRole.STUDENT && foundUser != null) {
                    loadStudentProfileWithRetry(foundUser.id)
                }
            }
        }
    }

    // Login / Register Logic
    fun loginUser(email: String, role: PanelRole, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val roleStr = when (role) {
                PanelRole.ADMIN -> "admin"
                PanelRole.TEACHER -> "teacher"
                PanelRole.STUDENT -> "student"
            }
            val user = repository.allUsers.first().firstOrNull { it.email.equals(email.trim(), ignoreCase = true) && it.role == roleStr }
            if (user != null) {
                _currentUser.value = user
                if (role == PanelRole.STUDENT) {
                    loadStudentProfileWithRetry(user.id)
                }
                onResult(true, "Welcome back, ${user.name}!")
            } else {
                onResult(false, "User not found for $roleStr role with email: $email")
            }
        }
    }

    fun registerUser(name: String, email: String, role: PanelRole, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val settings = systemSettings.value
            val roleStr = when (role) {
                PanelRole.ADMIN -> "admin"
                PanelRole.TEACHER -> "teacher"
                PanelRole.STUDENT -> "student"
            }

            if (role == PanelRole.ADMIN) {
                val existingAdmin = repository.getAdminUser()
                if (existingAdmin != null) {
                    onResult(false, "System constraint: Only 1 Admin is allowed.")
                    return@launch
                }
            } else if (role == PanelRole.TEACHER && !settings.allowTeacherRegistration) {
                onResult(false, "Teacher registration is currently disabled by Admin.")
                return@launch
            } else if (role == PanelRole.STUDENT && !settings.allowStudentRegistration) {
                onResult(false, "Student registration is currently disabled by Admin.")
                return@launch
            }

            val newId = "${roleStr}_${UUID.randomUUID().toString().take(6)}"
            val newUser = UserEntity(
                id = newId,
                name = name.ifBlank { "User ${newId.takeLast(4)}" },
                email = email.trim(),
                role = roleStr
            )
            repository.insertUser(newUser)
            dbHelper.saveUser(newUser)
            _currentUser.value = newUser

            AuditLogger.getInstance().logAction(
                action = "USER_REGISTERED",
                actorId = newUser.id,
                actorEmail = newUser.email,
                targetId = newUser.id,
                details = "New user registered: ${newUser.name} as ${roleStr.uppercase()}"
            )

            if (role == PanelRole.STUDENT) {
                loadStudentProfileWithRetry(newId)
            }

            onResult(true, "Registration successful for $name!")
        }
    }

    // Critical Student Timing Fix Requirement: Retry loop up to 5s, checking every 500ms
    fun loadStudentProfileWithRetry(studentId: String) {
        viewModelScope.launch {
            _studentProfileState.value = UiState.Loading
            var attempts = 0
            val maxAttempts = 10 // 10 * 500ms = 5000ms
            var foundUser: UserEntity? = null

            while (attempts < maxAttempts) {
                foundUser = repository.getUserById(studentId)
                if (foundUser != null) break
                delay(500)
                attempts++
            }

            if (foundUser != null) {
                _currentUser.value = foundUser
                _studentProfileState.value = UiState.Success(foundUser)
            } else {
                _studentProfileState.value = UiState.Error("Access Denied: Student profile could not be retrieved.")
            }
        }
    }

    // Teacher: Create Class
    fun createClass(className: String, subject: String, bannerUrl: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val teacher = _currentUser.value
            if (teacher == null || teacher.role != "teacher") {
                onResult(false, "Only authenticated teachers can create classes.")
                return@launch
            }
            if (className.isBlank() || subject.isBlank()) {
                onResult(false, "Class Name and Subject are required.")
                return@launch
            }

            val joinCode = repository.generateJoinCode()
            val finalBanner = bannerUrl.ifBlank { "https://images.unsplash.com/photo-1522202176988-66273c2fd55f" }

            val newClass = SchoolClassEntity(
                id = "c_${UUID.randomUUID().toString().take(6)}",
                className = className,
                subject = subject,
                bannerUrl = finalBanner,
                joinCode = joinCode,
                teacherId = teacher.id,
                teacherName = teacher.name
            )

            repository.insertClass(newClass)
            dbHelper.saveClass(newClass)

            AuditLogger.getInstance().logAction(
                action = "CLASS_CREATED",
                actorId = teacher.id,
                actorEmail = teacher.email,
                targetId = newClass.id,
                details = "Class created: '${className}' (${subject}) with join code ${joinCode}"
            )

            onResult(true, "Class created! Join Code: $joinCode")
        }
    }

    // Teacher: Create Assignment
    fun createAssignment(
        classId: String,
        className: String,
        title: String,
        description: String,
        dueDate: String,
        points: Int,
        attachmentUrl: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val teacher = _currentUser.value
            if (teacher == null || teacher.role != "teacher") {
                onResult(false, "Only teachers can create assignments.")
                return@launch
            }
            if (title.isBlank() || classId.isBlank()) {
                onResult(false, "Title and Class selection are required.")
                return@launch
            }

            val assignment = AssignmentEntity(
                id = "a_${UUID.randomUUID().toString().take(6)}",
                classId = classId,
                className = className,
                title = title,
                description = description,
                dueDate = dueDate.ifBlank { "2026-08-30" },
                points = if (points > 0) points else 100,
                attachmentUrl = attachmentUrl,
                teacherId = teacher.id
            )

            repository.insertAssignment(assignment)
            dbHelper.saveAssignment(assignment)

            // Save Real-Time Notification for Students
            val notification = AppNotification(
                recipientId = "student",
                title = "New Assignment: '$title'",
                message = "Published in class '$className' (Due: ${assignment.dueDate})",
                type = "NEW_ASSIGNMENT",
                targetId = assignment.id,
                className = className,
                timestamp = System.currentTimeMillis()
            )
            dbHelper.saveNotification(notification)

            AuditLogger.getInstance().logAction(
                action = "ASSIGNMENT_CREATED",
                actorId = teacher.id,
                actorEmail = teacher.email,
                targetId = assignment.id,
                details = "Assignment published: '${title}' for class '${className}'"
            )

            onResult(true, "Assignment published successfully!")
        }
    }

    // Student: Join Class by Join Code
    fun joinClassWithCode(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val student = _currentUser.value
            if (student == null || student.role != "student") {
                onResult(false, "Only students can join classes.")
                return@launch
            }
            val cleanCode = code.trim().uppercase()
            val targetClass = repository.getClassByJoinCode(cleanCode)
            if (targetClass == null) {
                onResult(false, "Invalid Join Code. Please check and try again.")
                return@launch
            }

            val existingEnrollment = repository.getEnrollment(targetClass.id, student.id)
            if (existingEnrollment != null) {
                onResult(false, "You are already enrolled in ${targetClass.className}.")
                return@launch
            }

            val enrollment = EnrollmentEntity(
                id = "e_${UUID.randomUUID().toString().take(6)}",
                classId = targetClass.id,
                studentId = student.id
            )
            repository.insertEnrollment(enrollment)
            dbHelper.saveEnrollment(enrollment)
            onResult(true, "Successfully joined ${targetClass.className}!")
        }
    }

    // Student: Submit Work
    fun submitAssignmentWork(
        assignmentId: String,
        assignmentTitle: String,
        classId: String,
        fileUrl: String,
        comment: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val student = _currentUser.value
            if (student == null || student.role != "student") {
                onResult(false, "Only students can submit work.")
                return@launch
            }
            if (fileUrl.isBlank()) {
                onResult(false, "File URL or attachment is required.")
                return@launch
            }

            val submission = SubmissionEntity(
                id = "sub_${UUID.randomUUID().toString().take(6)}",
                assignmentId = assignmentId,
                assignmentTitle = assignmentTitle,
                classId = classId,
                studentId = student.id,
                studentName = student.name,
                fileUrl = fileUrl,
                comment = comment,
                grade = "Submitted"
            )

            repository.insertSubmission(submission)
            dbHelper.saveSubmission(submission)

            // Save Real-Time Notification for Teachers
            val targetClass = repository.getClassById(classId)
            val notification = AppNotification(
                recipientId = targetClass?.teacherId?.ifBlank { "teacher" } ?: "teacher",
                title = "New Student Submission",
                message = "${student.name} submitted work for '$assignmentTitle'",
                type = "NEW_SUBMISSION",
                targetId = submission.id,
                className = targetClass?.className ?: "",
                timestamp = System.currentTimeMillis()
            )
            dbHelper.saveNotification(notification)

            onResult(true, "Work submitted successfully!")
        }
    }

    // Notification Actions
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            dbHelper.markNotificationAsRead(notificationId)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                dbHelper.clearUserNotifications(user.id, user.role)
                _notifications.value = emptyList()
            }
        }
    }

    // Admin: Toggle Registration Settings
    fun updateTeacherRegistrationAllowed(allowed: Boolean) {
        viewModelScope.launch {
            val current = systemSettings.value
            val updated = current.copy(allowTeacherRegistration = allowed)
            repository.updateSettings(updated)
            dbHelper.saveSystemSettings(updated)
            val admin = _currentUser.value
            AuditLogger.getInstance().logAction(
                action = "SETTINGS_CHANGED",
                actorId = admin?.id ?: "system",
                actorEmail = admin?.email ?: "admin@school.com",
                details = "Teacher registration allowed set to $allowed"
            )
            showToast("Teacher Registration set to: ${if (allowed) "Enabled" else "Disabled"}")
        }
    }

    fun updateStudentRegistrationAllowed(allowed: Boolean) {
        viewModelScope.launch {
            val current = systemSettings.value
            val updated = current.copy(allowStudentRegistration = allowed)
            repository.updateSettings(updated)
            dbHelper.saveSystemSettings(updated)
            val admin = _currentUser.value
            AuditLogger.getInstance().logAction(
                action = "SETTINGS_CHANGED",
                actorId = admin?.id ?: "system",
                actorEmail = admin?.email ?: "admin@school.com",
                details = "Student registration allowed set to $allowed"
            )
            showToast("Student Registration set to: ${if (allowed) "Enabled" else "Disabled"}")
        }
    }

    // Admin: Update User Role
    fun updateUserRole(targetUser: UserEntity, newRole: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val admin = _currentUser.value
            val updatedUser = targetUser.copy(role = newRole.lowercase())
            repository.insertUser(updatedUser)
            dbHelper.saveUser(updatedUser)

            AuditLogger.getInstance().logAction(
                action = "ROLE_CHANGE",
                actorId = admin?.id ?: "system",
                actorEmail = admin?.email ?: "admin@school.com",
                targetId = targetUser.id,
                details = "User role changed for '${targetUser.name}' (${targetUser.email}) from ${targetUser.role.uppercase()} to ${newRole.uppercase()}"
            )

            showToast("Role updated for ${targetUser.name} to ${newRole.uppercase()}")
            onResult(true, "Role updated successfully.")
        }
    }

    // Cloudinary Unsigned Upload Simulation Helper (Unsigned preset: ml_default, cloud name: dbfdguefj)
    fun uploadToCloudinary(filename: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            showToast("Uploading $filename to Cloudinary (Preset: ml_default)...")
            delay(1200) // Simulating network upload
            val cloudUrl = "https://res.cloudinary.com/dbfdguefj/image/upload/v17231456/shmschool/${filename.replace(" ", "_")}"
            showToast("Uploaded successfully to Cloudinary!")
            onComplete(cloudUrl)
        }
    }

    // User Profile Update
    fun updateUserProfile(
        newName: String,
        newAvatarUrl: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                onResult(false, "No user is currently logged in.")
                return@launch
            }

            if (newName.isBlank()) {
                onResult(false, "Display name cannot be empty.")
                return@launch
            }

            val updatedUser = user.copy(
                name = newName.trim(),
                avatarUrl = newAvatarUrl.trim()
            )

            try {
                // 1. Update AuthHelper (FirebaseUser)
                AuthHelper.getInstance().updateUserProfile(
                    newDisplayName = newName.trim(),
                    photoUrl = newAvatarUrl.trim().ifEmpty { null }
                )

                // 2. Update Firestore
                dbHelper.saveUser(updatedUser)

                // 3. Update Room Local Database
                repository.insertUser(updatedUser)

                // 4. Update ViewModel current user state
                _currentUser.value = updatedUser

                // 5. Audit Log Profile Update
                AuditLogger.getInstance().logAction(
                    action = "PROFILE_UPDATED",
                    actorId = user.id,
                    actorEmail = user.email,
                    targetId = user.id,
                    details = "User updated profile display name to '${newName.trim()}'"
                )

                showToast("Profile updated successfully!")
                onResult(true, "Profile updated successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Failed to update profile.")
            }
        }
    }

    // Admin Dashboard Stats from Firestore
    fun fetchAdminStatsFromFirestore(onResult: (AdminStats) -> Unit) {
        viewModelScope.launch {
            val stats = dbHelper.fetchAdminStats()
            onResult(stats)
        }
    }

    // Fetch Audit Logs for Administrative Oversight
    fun fetchAuditLogs(onResult: (List<AuditLog>) -> Unit) {
        viewModelScope.launch {
            val logs = AuditLogger.getInstance().fetchRecentAuditLogs()
            onResult(logs)
        }
    }

    override fun onCleared() {
        super.onCleared()
        assignmentListenerReg?.remove()
        submissionListenerReg?.remove()
        notificationListenerReg?.remove()
    }
}

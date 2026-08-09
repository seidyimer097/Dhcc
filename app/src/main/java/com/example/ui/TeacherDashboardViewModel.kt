package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AssignmentEntity
import com.example.data.AuthHelper
import com.example.data.DatabaseHelper
import com.example.data.SchoolClassEntity
import com.example.data.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for the Teacher Dashboard Screen, managing teacher classes,
 * fetching assignments from DatabaseHelper (Firestore), and handling assignment/class creation.
 */
class TeacherDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = DatabaseHelper.getInstance()
    private val authHelper = AuthHelper.getInstance()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _classes = MutableStateFlow<List<SchoolClassEntity>>(emptyList())
    val classes: StateFlow<List<SchoolClassEntity>> = _classes.asStateFlow()

    private val _assignments = MutableStateFlow<List<AssignmentEntity>>(emptyList())
    val assignments: StateFlow<List<AssignmentEntity>> = _assignments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadData()
    }

    fun loadData(teacherId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uid = teacherId ?: authHelper.currentUserId
                if (uid != null) {
                    val user = dbHelper.getUserById(uid)
                    _currentUser.value = user

                    val teacherClasses = dbHelper.fetchClassesByTeacher(uid)
                    _classes.value = teacherClasses

                    val allTeacherAssignments = mutableListOf<AssignmentEntity>()
                    teacherClasses.forEach { schoolClass ->
                        val classAssignments = dbHelper.fetchAssignmentsByClass(schoolClass.id)
                        allTeacherAssignments.addAll(classAssignments)
                    }
                    _assignments.value = allTeacherAssignments.distinctBy { it.id }
                } else {
                    val allClasses = dbHelper.fetchAllClasses()
                    _classes.value = allClasses
                    val allAssignments = dbHelper.fetchAllAssignments()
                    _assignments.value = allAssignments
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createAssignment(
        classId: String,
        className: String,
        title: String,
        description: String,
        dueDate: String,
        points: Int,
        attachmentUrl: String = "",
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        if (title.isBlank() || classId.isBlank()) {
            onComplete?.invoke(false, "Title and class selection are required.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val teacherId = authHelper.currentUserId ?: _currentUser.value?.id ?: "teacher_id"
                val newAssignment = AssignmentEntity(
                    id = "assignment_${UUID.randomUUID().toString().take(8)}",
                    classId = classId,
                    className = className,
                    title = title.trim(),
                    description = description.trim(),
                    dueDate = dueDate,
                    points = points,
                    attachmentUrl = attachmentUrl,
                    teacherId = teacherId,
                    createdAt = System.currentTimeMillis()
                )

                val success = dbHelper.saveAssignment(newAssignment)
                if (success) {
                    _toastMessage.value = "Assignment created successfully!"
                    loadData()
                    onComplete?.invoke(true, "Assignment created successfully!")
                } else {
                    onComplete?.invoke(false, "Failed to create assignment in Firestore.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete?.invoke(false, e.localizedMessage ?: "Error creating assignment")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createClass(
        className: String,
        subject: String,
        bannerUrl: String = "",
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        if (className.isBlank() || subject.isBlank()) {
            onComplete?.invoke(false, "Class name and subject are required.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val teacherId = authHelper.currentUserId ?: _currentUser.value?.id ?: "teacher_id"
                val teacherName = authHelper.currentDisplayName ?: _currentUser.value?.name ?: "Teacher"
                val joinCode = (100000..999999).random().toString()

                val newClass = SchoolClassEntity(
                    id = "class_${UUID.randomUUID().toString().take(8)}",
                    className = className.trim(),
                    subject = subject.trim(),
                    teacherId = teacherId,
                    teacherName = teacherName,
                    joinCode = joinCode,
                    bannerUrl = bannerUrl,
                    createdAt = System.currentTimeMillis()
                )

                val success = dbHelper.saveClass(newClass)
                if (success) {
                    _toastMessage.value = "Class '$className' created! Code: $joinCode"
                    loadData()
                    onComplete?.invoke(true, "Class created successfully!")
                } else {
                    onComplete?.invoke(false, "Failed to save class to Firestore.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete?.invoke(false, e.localizedMessage ?: "Error creating class")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String, // "admin", "teacher", "student"
    val avatarUrl: String = "",
    val registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classes")
data class SchoolClassEntity(
    @PrimaryKey val id: String,
    val className: String,
    val subject: String,
    val bannerUrl: String,
    val joinCode: String, // 6-character unique code
    val teacherId: String,
    val teacherName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val className: String,
    val title: String,
    val description: String,
    val dueDate: String,
    val points: Int,
    val attachmentUrl: String = "",
    val teacherId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "enrollments")
data class EnrollmentEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val studentId: String,
    val enrolledAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "submissions")
data class SubmissionEntity(
    @PrimaryKey val id: String,
    val assignmentId: String,
    val assignmentTitle: String,
    val classId: String,
    val studentId: String,
    val studentName: String,
    val fileUrl: String,
    val comment: String = "",
    val submittedAt: Long = System.currentTimeMillis(),
    val grade: String = "Pending"
)

@Entity(tableName = "system_settings")
data class SystemSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val allowTeacherRegistration: Boolean = true,
    val allowStudentRegistration: Boolean = true
)

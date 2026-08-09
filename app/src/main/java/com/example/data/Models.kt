package com.example.data

/**
 * Standard Kotlin data models for Firebase Firestore serialization/deserialization 
 * and usage within DatabaseHelper and ViewModel.
 */

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "student", // "admin", "teacher", "student"
    val avatarUrl: String = "",
    val registeredAt: Long = System.currentTimeMillis()
)

data class ClassModel(
    val id: String = "",
    val className: String = "",
    val subject: String = "",
    val bannerUrl: String = "",
    val joinCode: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// Alias Class to ClassModel for backtick compatibility if needed
typealias SchoolClass = ClassModel

data class Assignment(
    val id: String = "",
    val classId: String = "",
    val className: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",
    val points: Int = 100,
    val attachmentUrl: String = "",
    val teacherId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Submission(
    val id: String = "",
    val assignmentId: String = "",
    val assignmentTitle: String = "",
    val classId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val fileUrl: String = "",
    val comment: String = "",
    val submittedAt: Long = System.currentTimeMillis(),
    val grade: String = "Pending"
)

data class AdminStats(
    val totalTeachers: Int = 0,
    val totalStudents: Int = 0,
    val totalClasses: Int = 0,
    val totalAssignments: Int = 0,
    val totalSubmissions: Int = 0,
    val totalAdmins: Int = 0
)

data class AppNotification(
    val id: String = "",
    val recipientId: String = "",    // Specific User ID, or "student", "teacher", "all"
    val title: String = "",
    val message: String = "",
    val type: String = "",           // "NEW_ASSIGNMENT", "NEW_SUBMISSION"
    val targetId: String = "",       // assignmentId or submissionId
    val className: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

// Extension functions for converting between Entities and Models
fun UserEntity.toModel(): User = User(id, name, email, role, avatarUrl, registeredAt)
fun User.toEntity(): UserEntity = UserEntity(id, name, email, role, avatarUrl, registeredAt)

fun SchoolClassEntity.toModel(): ClassModel = ClassModel(id, className, subject, bannerUrl, joinCode, teacherId, teacherName, createdAt)
fun ClassModel.toEntity(): SchoolClassEntity = SchoolClassEntity(id, className, subject, bannerUrl, joinCode, teacherId, teacherName, createdAt)

fun AssignmentEntity.toModel(): Assignment = Assignment(id, classId, className, title, description, dueDate, points, attachmentUrl, teacherId, createdAt)
fun Assignment.toEntity(): AssignmentEntity = AssignmentEntity(id, classId, className, title, description, dueDate, points, attachmentUrl, teacherId, createdAt)

fun SubmissionEntity.toModel(): Submission = Submission(id, assignmentId, assignmentTitle, classId, studentId, studentName, fileUrl, comment, submittedAt, grade)
fun Submission.toEntity(): SubmissionEntity = SubmissionEntity(id, assignmentId, assignmentTitle, classId, studentId, studentName, fileUrl, comment, submittedAt, grade)

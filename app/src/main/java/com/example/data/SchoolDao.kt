package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {

    // Users
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'admin' LIMIT 1")
    suspend fun getAdminUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Classes
    @Query("SELECT * FROM classes ORDER BY createdAt DESC")
    fun getAllClasses(): Flow<List<SchoolClassEntity>>

    @Query("SELECT * FROM classes WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun getClassesByTeacher(teacherId: String): Flow<List<SchoolClassEntity>>

    @Query("SELECT * FROM classes WHERE joinCode = :code LIMIT 1")
    suspend fun getClassByJoinCode(code: String): SchoolClassEntity?

    @Query("SELECT * FROM classes WHERE id = :classId LIMIT 1")
    suspend fun getClassById(classId: String): SchoolClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(schoolClass: SchoolClassEntity)

    // Assignments
    @Query("SELECT * FROM assignments ORDER BY createdAt DESC")
    fun getAllAssignments(): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun getAssignmentsByTeacher(teacherId: String): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE classId = :classId ORDER BY createdAt DESC")
    fun getAssignmentsByClass(classId: String): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentEntity)

    // Enrollments
    @Query("SELECT * FROM enrollments WHERE studentId = :studentId")
    fun getEnrollmentsByStudent(studentId: String): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE classId = :classId")
    fun getEnrollmentsByClass(classId: String): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE classId = :classId AND studentId = :studentId LIMIT 1")
    suspend fun getEnrollment(classId: String, studentId: String): EnrollmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollment(enrollment: EnrollmentEntity)

    // Submissions
    @Query("SELECT * FROM submissions ORDER BY submittedAt DESC")
    fun getAllSubmissions(): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE studentId = :studentId ORDER BY submittedAt DESC")
    fun getSubmissionsByStudent(studentId: String): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE assignmentId = :assignmentId ORDER BY submittedAt DESC")
    fun getSubmissionsByAssignment(assignmentId: String): Flow<List<SubmissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: SubmissionEntity)

    // System Settings
    @Query("SELECT * FROM system_settings WHERE id = 1 LIMIT 1")
    fun getSystemSettings(): Flow<SystemSettingsEntity?>

    @Query("SELECT * FROM system_settings WHERE id = 1 LIMIT 1")
    suspend fun getSystemSettingsOnce(): SystemSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSystemSettings(settings: SystemSettingsEntity)
}

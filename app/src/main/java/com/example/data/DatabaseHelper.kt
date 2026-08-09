package com.example.data

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class DatabaseHelper private constructor() {

    val firestore: FirebaseFirestore by lazy {
        val db = FirebaseFirestore.getInstance()
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            db.firestoreSettings = settings
            Log.d("DatabaseHelper", "Firestore offline persistence enabled with unlimited cache size.")
        } catch (e: Exception) {
            Log.w("DatabaseHelper", "Firestore settings configuration note: ${e.message}")
        }
        db
    }

    companion object {
        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper().also { instance = it }
            }
        }

        const val COLLECTION_USERS = "users"
        const val COLLECTION_CLASSES = "classes"
        const val COLLECTION_ASSIGNMENTS = "assignments"
        const val COLLECTION_ENROLLMENTS = "enrollments"
        const val COLLECTION_SUBMISSIONS = "submissions"
        const val COLLECTION_SETTINGS = "settings"
        const val COLLECTION_AUDIT_LOGS = "audit_logs"
        const val COLLECTION_NOTIFICATIONS = "notifications"
    }

    // --- GENERIC FIRESTORE CRUD OPERATIONS ---

    suspend fun addDocument(collectionName: String, documentId: String, data: Any): Boolean {
        return try {
            firestore.collection(collectionName)
                .document(documentId)
                .set(data, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun <T> getDocument(collectionName: String, documentId: String, clazz: Class<T>): T? {
        return try {
            val snapshot = firestore.collection(collectionName)
                .document(documentId)
                .get()
                .await()
            snapshot.toObject(clazz)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun <T> getCollection(collectionName: String, clazz: Class<T>): List<T> {
        return try {
            val snapshot = firestore.collection(collectionName)
                .get()
                .await()
            snapshot.toObjects(clazz)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun updateDocument(collectionName: String, documentId: String, updates: Map<String, Any>): Boolean {
        return try {
            firestore.collection(collectionName)
                .document(documentId)
                .update(updates)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteDocument(collectionName: String, documentId: String): Boolean {
        return try {
            firestore.collection(collectionName)
                .document(documentId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- USER OPERATIONS ---

    suspend fun saveUser(user: User): Boolean = saveUser(user.toEntity())

    suspend fun saveUser(user: UserEntity): Boolean {
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(user.id)
                .set(user, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getUserById(userId: String): UserEntity? {
        return try {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            snapshot.toObject(UserEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateUserProfile(userId: String, name: String, avatarUrl: String): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>()
            if (name.isNotBlank()) updates["name"] = name.trim()
            if (avatarUrl.isNotBlank()) updates["avatarUrl"] = avatarUrl.trim()
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchUsersByRole(role: String): List<UserEntity> {
        return try {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .whereEqualTo("role", role)
                .get()
                .await()
            snapshot.toObjects(UserEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchAllUsers(): List<UserEntity> {
        return try {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .get()
                .await()
            snapshot.toObjects(UserEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Verifies if an 'Admin' user exists in the 'users' collection
     * to enforce admin registration restriction rules.
     */
    suspend fun hasAdminUser(): Boolean {
        return try {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .whereIn("role", listOf("admin", "ADMIN", "Admin"))
                .limit(1)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                true
            } else {
                // Fallback check against all users in case of case mismatch or unindexed query
                val allUsers = fetchAllUsers()
                allUsers.any { it.role.equals("admin", ignoreCase = true) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val allUsers = fetchAllUsers()
                allUsers.any { it.role.equals("admin", ignoreCase = true) }
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }
        }
    }

    suspend fun isAdminUserExists(): Boolean = hasAdminUser()

    suspend fun updateUserRole(userId: String, newRole: String): Boolean {
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update("role", newRole)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- CLASS OPERATIONS ---

    suspend fun saveClass(schoolClass: ClassModel): Boolean = saveClass(schoolClass.toEntity())

    suspend fun saveClass(schoolClass: SchoolClassEntity): Boolean {
        return try {
            firestore.collection(COLLECTION_CLASSES)
                .document(schoolClass.id)
                .set(schoolClass, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchClassByJoinCode(code: String): SchoolClassEntity? {
        return try {
            val snapshot = firestore.collection(COLLECTION_CLASSES)
                .whereEqualTo("joinCode", code.uppercase())
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(SchoolClassEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchClassesByTeacher(teacherId: String): List<SchoolClassEntity> {
        return try {
            val snapshot = firestore.collection(COLLECTION_CLASSES)
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()
            snapshot.toObjects(SchoolClassEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchAllClasses(): List<SchoolClassEntity> {
        return try {
            val snapshot = firestore.collection(COLLECTION_CLASSES)
                .get()
                .await()
            snapshot.toObjects(SchoolClassEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- ASSIGNMENT OPERATIONS ---

    suspend fun saveAssignment(assignment: Assignment): Boolean = saveAssignment(assignment.toEntity())

    suspend fun saveAssignment(assignment: AssignmentEntity): Boolean {
        return try {
            firestore.collection(COLLECTION_ASSIGNMENTS)
                .document(assignment.id)
                .set(assignment, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchAssignmentsByClass(classId: String): List<AssignmentEntity> {
        return try {
            val snapshot = firestore.collection(COLLECTION_ASSIGNMENTS)
                .whereEqualTo("classId", classId)
                .get()
                .await()
            snapshot.toObjects(AssignmentEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchAllAssignments(): List<AssignmentEntity> {
        return try {
            val snapshot = firestore.collection(COLLECTION_ASSIGNMENTS)
                .get()
                .await()
            snapshot.toObjects(AssignmentEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- ENROLLMENT OPERATIONS ---

    suspend fun saveEnrollment(enrollment: EnrollmentEntity): Boolean {
        return try {
            firestore.collection(COLLECTION_ENROLLMENTS)
                .document(enrollment.id)
                .set(enrollment, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchEnrollmentsByStudent(studentId: String): List<EnrollmentEntity> {
        return try {
            val snapshot = firestore.collection(COLLECTION_ENROLLMENTS)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            snapshot.toObjects(EnrollmentEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- SUBMISSION OPERATIONS ---

    suspend fun saveSubmission(submission: Submission): Boolean = saveSubmission(submission.toEntity())

    suspend fun saveSubmission(submission: SubmissionEntity): Boolean {
        return try {
            firestore.collection(COLLECTION_SUBMISSIONS)
                .document(submission.id)
                .set(submission, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchSubmissionsByAssignment(assignmentId: String): List<SubmissionEntity> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SUBMISSIONS)
                .whereEqualTo("assignmentId", assignmentId)
                .get()
                .await()
            snapshot.toObjects(SubmissionEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchSubmissionsByStudent(studentId: String): List<SubmissionEntity> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SUBMISSIONS)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            snapshot.toObjects(SubmissionEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun updateSubmissionGrade(submissionId: String, grade: String): Boolean {
        return try {
            firestore.collection(COLLECTION_SUBMISSIONS)
                .document(submissionId)
                .update("grade", grade)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- SYSTEM SETTINGS OPERATIONS ---

    suspend fun saveSystemSettings(settings: SystemSettingsEntity): Boolean {
        return try {
            firestore.collection(COLLECTION_SETTINGS)
                .document("system_settings")
                .set(settings, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchSystemSettings(): SystemSettingsEntity? {
        return try {
            val snapshot = firestore.collection(COLLECTION_SETTINGS)
                .document("system_settings")
                .get()
                .await()
            snapshot.toObject(SystemSettingsEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- ADMIN DASHBOARD STATS ---

    /**
     * Calculates total teachers, total students, total classes, assignments, and submissions from Firestore.
     */
    suspend fun fetchAdminStats(): AdminStats {
        return try {
            val users = fetchAllUsers()
            val classes = fetchAllClasses()
            val assignments = fetchAllAssignments()

            val teachers = users.count { it.role.equals("teacher", ignoreCase = true) }
            val students = users.count { it.role.equals("student", ignoreCase = true) }
            val admins = users.count { it.role.equals("admin", ignoreCase = true) }

            val submissionsSnapshot = try {
                firestore.collection(COLLECTION_SUBMISSIONS).get().await()
            } catch (e: Exception) {
                null
            }
            val submissionsCount = submissionsSnapshot?.size() ?: 0

            AdminStats(
                totalTeachers = teachers,
                totalStudents = students,
                totalClasses = classes.size,
                totalAssignments = assignments.size,
                totalSubmissions = submissionsCount,
                totalAdmins = admins
            )
        } catch (e: Exception) {
            e.printStackTrace()
            AdminStats()
        }
    }

    suspend fun calculateSystemStats(): AdminStats = fetchAdminStats()

    // --- REAL-TIME FIRESTORE SNAPSHOT LISTENERS ---

    /**
     * Attaches a real-time Firestore snapshot listener on the 'assignments' collection.
     * Fires onNewAssignment callback whenever a new assignment document is added.
     */
    fun listenToAssignmentsRealtime(
        onNewAssignment: (AssignmentEntity) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_ASSIGNMENTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DatabaseHelper", "Error listening to real-time assignments", error)
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        try {
                            val assignment = change.document.toObject(AssignmentEntity::class.java)
                            onNewAssignment(assignment)
                        } catch (e: Exception) {
                            Log.e("DatabaseHelper", "Error parsing assignment change", e)
                        }
                    }
                }
            }
    }

    /**
     * Attaches a real-time Firestore snapshot listener on the 'submissions' collection.
     * Fires onNewSubmission callback whenever a new student submission is added.
     */
    fun listenToSubmissionsRealtime(
        onNewSubmission: (SubmissionEntity) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_SUBMISSIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DatabaseHelper", "Error listening to real-time submissions", error)
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        try {
                            val submission = change.document.toObject(SubmissionEntity::class.java)
                            onNewSubmission(submission)
                        } catch (e: Exception) {
                            Log.e("DatabaseHelper", "Error parsing submission change", e)
                        }
                    }
                }
            }
    }

    /**
     * Attaches a real-time snapshot listener on the 'notifications' collection for a specific user.
     * Listens for notifications directed to user ID, user role ("student"/"teacher"), or "all".
     */
    fun listenToUserNotifications(
        userId: String,
        userRole: String,
        onNotificationsUpdate: (List<AppNotification>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_NOTIFICATIONS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DatabaseHelper", "Error listening to real-time notifications", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val allNotifs = snapshot.toObjects(AppNotification::class.java)
                    // Filter relevant notifications for this user
                    val filtered = allNotifs.filter { notif ->
                        notif.recipientId == userId ||
                                notif.recipientId == userRole.lowercase() ||
                                notif.recipientId == "all"
                    }
                    onNotificationsUpdate(filtered)
                }
            }
    }

    /**
     * Saves or creates a notification in Firestore.
     */
    suspend fun saveNotification(notification: AppNotification): Boolean {
        return try {
            val docId = if (notification.id.isNotBlank()) notification.id else "notif_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val finalNotif = notification.copy(id = docId)
            firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(docId)
                .set(finalNotif, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Failed to save notification", e)
            false
        }
    }

    /**
     * Marks a notification as read in Firestore.
     */
    suspend fun markNotificationAsRead(notificationId: String): Boolean {
        return try {
            firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .update("isRead", true)
                .await()
            true
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Failed to mark notification read", e)
            false
        }
    }

    /**
     * Clears/Deletes all notifications for a given user.
     */
    suspend fun clearUserNotifications(userId: String, userRole: String): Boolean {
        return try {
            val snapshot = firestore.collection(COLLECTION_NOTIFICATIONS).get().await()
            val docsToDelete = snapshot.documents.filter { doc ->
                val recipient = doc.getString("recipientId") ?: ""
                recipient == userId || recipient == userRole.lowercase() || recipient == "all"
            }
            docsToDelete.forEach { doc ->
                doc.reference.delete().await()
            }
            true
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Failed to clear notifications", e)
            false
        }
    }
}

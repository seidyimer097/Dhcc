package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Data model for Audit Logs stored in Firestore for administrative oversight.
 */
data class AuditLog(
    val id: String = "",
    val action: String = "",       // e.g. "ROLE_CHANGE", "CLASS_CREATED", "USER_REGISTERED", "SETTINGS_CHANGED", "ASSIGNMENT_CREATED", "PROFILE_UPDATED"
    val actorId: String = "",      // User ID performing the action
    val actorEmail: String = "",   // Email of actor
    val targetId: String = "",     // Target entity ID (User ID, Class ID, Assignment ID, etc.)
    val details: String = "",      // Human readable detail description
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Utility class to track and record sensitive actions in Firestore for administrative oversight.
 */
class AuditLogger private constructor() {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    companion object {
        const val COLLECTION_AUDIT_LOGS = "audit_logs"
        private const val TAG = "AuditLogger"

        @Volatile
        private var instance: AuditLogger? = null

        fun getInstance(): AuditLogger {
            return instance ?: synchronized(this) {
                instance ?: AuditLogger().also { instance = it }
            }
        }
    }

    /**
     * Records a sensitive administrative or system action to Firestore audit_logs collection.
     */
    suspend fun logAction(
        action: String,
        actorId: String,
        actorEmail: String,
        targetId: String = "",
        details: String = ""
    ): Boolean {
        return try {
            val logId = "log_${UUID.randomUUID().toString().take(8)}"
            val logEntry = AuditLog(
                id = logId,
                action = action,
                actorId = actorId,
                actorEmail = actorEmail,
                targetId = targetId,
                details = details,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_AUDIT_LOGS)
                .document(logId)
                .set(logEntry)
                .await()
            Log.d(TAG, "Audit action logged: $action by $actorEmail")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record audit log: $action", e)
            false
        }
    }

    /**
     * Retrieves recent audit logs from Firestore ordered by timestamp descending.
     */
    suspend fun fetchRecentAuditLogs(limit: Long = 50): List<AuditLog> {
        return try {
            val snapshot = firestore.collection(COLLECTION_AUDIT_LOGS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            snapshot.toObjects(AuditLog::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch audit logs", e)
            emptyList()
        }
    }
}

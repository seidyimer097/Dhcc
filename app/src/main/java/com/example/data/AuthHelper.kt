package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Encapsulates Firebase Authentication logic for ShM School system,
 * including email/password sign-in, registration, password reset, and session management.
 */
class AuthHelper private constructor() {

    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    companion object {
        @Volatile
        private var instance: AuthHelper? = null

        fun getInstance(): AuthHelper {
            return instance ?: synchronized(this) {
                instance ?: AuthHelper().also { instance = it }
            }
        }
    }

    // --- SESSION PROPERTIES ---

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val isUserLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    val currentUserEmail: String?
        get() = firebaseAuth.currentUser?.email

    val currentDisplayName: String?
        get() = firebaseAuth.currentUser?.displayName

    // --- AUTH STATE FLOW ---

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    // --- SIGN IN WITH EMAIL & PASSWORD ---

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        if (email.isBlank() || password.isBlank()) {
            return AuthResult.Error("Email and password cannot be empty.")
        }
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Failed to sign in. User is null.")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Authentication failed.")
        }
    }

    // --- SIGN UP / REGISTER WITH EMAIL & PASSWORD ---

    suspend fun isRoleRegistrationAllowed(role: String): Pair<Boolean, String?> {
        val normalizedRole = role.lowercase().trim()
        val dbHelper = DatabaseHelper.getInstance()
        if (normalizedRole == "admin") {
            if (dbHelper.hasAdminUser()) {
                return Pair(false, "Registration restricted: An Admin account already exists in the system.")
            }
        } else {
            val settings = dbHelper.fetchSystemSettings() ?: SystemSettingsEntity()
            if (normalizedRole == "teacher" && !settings.allowTeacherRegistration) {
                return Pair(false, "Registration restricted: Teacher self-registration is currently disabled.")
            }
            if (normalizedRole == "student" && !settings.allowStudentRegistration) {
                return Pair(false, "Registration restricted: Student self-registration is currently disabled.")
            }
        }
        return Pair(true, null)
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
        role: String = "student"
    ): AuthResult {
        if (email.isBlank() || password.isBlank()) {
            return AuthResult.Error("Email and password cannot be empty.")
        }
        if (password.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters long.")
        }

        val (allowed, restrictionMsg) = isRoleRegistrationAllowed(role)
        if (!allowed) {
            return AuthResult.Error(restrictionMsg ?: "Registration restricted for this role.")
        }

        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                // Update profile display name
                if (displayName.isNotBlank()) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    firebaseUser.updateProfile(profileUpdates).await()
                }

                // Sync new user record into Firestore via DatabaseHelper
                val normalizedRole = role.lowercase().trim()
                val newUser = User(
                    id = firebaseUser.uid,
                    name = displayName.ifBlank { email.substringBefore("@") },
                    email = email.trim(),
                    role = normalizedRole,
                    registeredAt = System.currentTimeMillis()
                )
                DatabaseHelper.getInstance().saveUser(newUser)

                AuthResult.Success(firebaseUser)
            } else {
                AuthResult.Error("Registration failed. Could not create account.")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Registration error.")
        }
    }

    // --- FETCH USER ROLE FROM FIRESTORE ---

    suspend fun fetchUserRole(userId: String): String? {
        val userEntity = DatabaseHelper.getInstance().getUserById(userId)
        return userEntity?.role
    }

    suspend fun fetchCurrentUserRole(): String? {
        val uid = currentUserId ?: return null
        return fetchUserRole(uid)
    }

    // --- PASSWORD RESET ---

    suspend fun sendPasswordResetEmail(email: String): Boolean {
        if (email.isBlank()) return false
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- PROFILE MANAGEMENT ---

    suspend fun updateDisplayName(newDisplayName: String): Boolean {
        val user = currentUser ?: return false
        return try {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newDisplayName)
                .build()
            user.updateProfile(profileUpdates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateUserProfile(newDisplayName: String, photoUrl: String? = null): Boolean {
        val user = currentUser ?: return false
        return try {
            val builder = UserProfileChangeRequest.Builder()
            if (newDisplayName.isNotBlank()) {
                builder.setDisplayName(newDisplayName.trim())
            }
            if (!photoUrl.isNullOrBlank()) {
                builder.setPhotoUri(android.net.Uri.parse(photoUrl.trim()))
            }
            user.updateProfile(builder.build()).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- SIGN OUT & SESSION TERMINATION ---

    fun signOut() {
        firebaseAuth.signOut()
    }
}

/**
 * Sealed class representing authentication result state.
 */
sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

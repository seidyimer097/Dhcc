package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminStats
import com.example.data.DatabaseHelper
import com.example.data.SchoolClassEntity
import com.example.data.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Admin Dashboard Screen, exposing total teachers,
 * total students, total classes, and system statistics from DatabaseHelper (Firestore).
 */
class AdminDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = DatabaseHelper.getInstance()

    private val _adminStats = MutableStateFlow(AdminStats())
    val adminStats: StateFlow<AdminStats> = _adminStats.asStateFlow()

    private val _users = MutableStateFlow<List<UserEntity>>(emptyList())
    val users: StateFlow<List<UserEntity>> = _users.asStateFlow()

    private val _classes = MutableStateFlow<List<SchoolClassEntity>>(emptyList())
    val classes: StateFlow<List<SchoolClassEntity>> = _classes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stats = dbHelper.fetchAdminStats()
                _adminStats.value = stats

                val allUsers = dbHelper.fetchAllUsers()
                _users.value = allUsers

                val allClasses = dbHelper.fetchAllClasses()
                _classes.value = allClasses
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        loadStats()
    }
}

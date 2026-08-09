package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.SchoolClassEntity
import com.example.data.UserEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportUtil {

    fun generateUsersCsv(users: List<UserEntity>): String {
        val sb = StringBuilder()
        sb.append("User ID,Full Name,Email Address,Role,Registration Date\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        users.forEach { user ->
            val cleanName = escapeCsv(user.name)
            val cleanEmail = escapeCsv(user.email)
            val cleanRole = escapeCsv(user.role.uppercase())
            val regDate = dateFormat.format(Date(user.registeredAt))
            sb.append("${user.id},$cleanName,$cleanEmail,$cleanRole,$regDate\n")
        }
        return sb.toString()
    }

    fun generateClassesCsv(classes: List<SchoolClassEntity>): String {
        val sb = StringBuilder()
        sb.append("Class ID,Class Name,Subject,Teacher Name,Teacher ID,Join Code,Created Date\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        classes.forEach { schoolClass ->
            val cleanClassName = escapeCsv(schoolClass.className)
            val cleanSubject = escapeCsv(schoolClass.subject)
            val cleanTeacherName = escapeCsv(schoolClass.teacherName)
            val cleanJoinCode = escapeCsv(schoolClass.joinCode)
            val createdDate = dateFormat.format(Date(schoolClass.createdAt))
            sb.append("${schoolClass.id},$cleanClassName,$cleanSubject,$cleanTeacherName,${schoolClass.teacherId},$cleanJoinCode,$createdDate\n")
        }
        return sb.toString()
    }

    fun generateCombinedCsv(users: List<UserEntity>, classes: List<SchoolClassEntity>): String {
        val sb = StringBuilder()
        sb.append("=== SCHOOL USERS REPORT ===\n")
        sb.append(generateUsersCsv(users))
        sb.append("\n=== SCHOOL CLASSES REPORT ===\n")
        sb.append(generateClassesCsv(classes))
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            val escaped = value.replace("\"", "\"\"")
            return "\"$escaped\""
        }
        return value
    }

    fun shareCsvFile(context: Context, csvContent: String, filename: String) {
        try {
            val file = File(context.cacheDir, filename)
            file.writeText(csvContent)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "School Management CSV Export - $filename")
                putExtra(Intent.EXTRA_TEXT, "Exported administrative CSV report from School Management App.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share / Export CSV File"))
        } catch (e: Exception) {
            e.printStackTrace()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, filename)
                putExtra(Intent.EXTRA_TEXT, csvContent)
            }
            context.startActivity(Intent.createChooser(intent, "Export CSV Text"))
        }
    }
}

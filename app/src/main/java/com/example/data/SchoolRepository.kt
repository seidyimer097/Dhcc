package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class SchoolRepository(private val schoolDao: SchoolDao) {

    val allUsers: Flow<List<UserEntity>> = schoolDao.getAllUsers()
    val allClasses: Flow<List<SchoolClassEntity>> = schoolDao.getAllClasses()
    val allAssignments: Flow<List<AssignmentEntity>> = schoolDao.getAllAssignments()
    val allSubmissions: Flow<List<SubmissionEntity>> = schoolDao.getAllSubmissions()
    val systemSettings: Flow<SystemSettingsEntity?> = schoolDao.getSystemSettings()

    fun getUsersByRole(role: String): Flow<List<UserEntity>> = schoolDao.getUsersByRole(role)
    fun getClassesByTeacher(teacherId: String): Flow<List<SchoolClassEntity>> = schoolDao.getClassesByTeacher(teacherId)
    fun getAssignmentsByTeacher(teacherId: String): Flow<List<AssignmentEntity>> = schoolDao.getAssignmentsByTeacher(teacherId)
    fun getAssignmentsByClass(classId: String): Flow<List<AssignmentEntity>> = schoolDao.getAssignmentsByClass(classId)
    fun getEnrollmentsByStudent(studentId: String): Flow<List<EnrollmentEntity>> = schoolDao.getEnrollmentsByStudent(studentId)
    fun getSubmissionsByStudent(studentId: String): Flow<List<SubmissionEntity>> = schoolDao.getSubmissionsByStudent(studentId)
    fun getSubmissionsByAssignment(assignmentId: String): Flow<List<SubmissionEntity>> = schoolDao.getSubmissionsByAssignment(assignmentId)

    suspend fun getAdminUser(): UserEntity? = schoolDao.getAdminUser()
    suspend fun getUserById(id: String): UserEntity? = schoolDao.getUserById(id)
    suspend fun getClassByJoinCode(code: String): SchoolClassEntity? = schoolDao.getClassByJoinCode(code)
    suspend fun getClassById(id: String): SchoolClassEntity? = schoolDao.getClassById(id)
    suspend fun getEnrollment(classId: String, studentId: String): EnrollmentEntity? = schoolDao.getEnrollment(classId, studentId)

    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        schoolDao.insertUser(user)
    }

    suspend fun insertClass(schoolClass: SchoolClassEntity) = withContext(Dispatchers.IO) {
        schoolDao.insertClass(schoolClass)
    }

    suspend fun insertAssignment(assignment: AssignmentEntity) = withContext(Dispatchers.IO) {
        schoolDao.insertAssignment(assignment)
    }

    suspend fun insertEnrollment(enrollment: EnrollmentEntity) = withContext(Dispatchers.IO) {
        schoolDao.insertEnrollment(enrollment)
    }

    suspend fun insertSubmission(submission: SubmissionEntity) = withContext(Dispatchers.IO) {
        schoolDao.insertSubmission(submission)
    }

    suspend fun updateSettings(settings: SystemSettingsEntity) = withContext(Dispatchers.IO) {
        schoolDao.updateSystemSettings(settings)
    }

    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        val admin = schoolDao.getAdminUser()
        if (admin == null) {
            // Seed Admin
            val adminUser = UserEntity(
                id = "admin_001",
                name = "System Administrator",
                email = "admin@shmschool.edu",
                role = "admin"
            )
            schoolDao.insertUser(adminUser)

            // Seed System Settings
            schoolDao.updateSystemSettings(
                SystemSettingsEntity(
                    id = 1,
                    allowTeacherRegistration = true,
                    allowStudentRegistration = true
                )
            )

            // Seed Teachers
            val t1 = UserEntity("t_001", "Prof. Alan Turing", "turing@shmschool.edu", "teacher")
            val t2 = UserEntity("t_002", "Dr. Margaret Hamilton", "hamilton@shmschool.edu", "teacher")
            val t3 = UserEntity("t_003", "Prof. Ada Lovelace", "lovelace@shmschool.edu", "teacher")
            schoolDao.insertUser(t1)
            schoolDao.insertUser(t2)
            schoolDao.insertUser(t3)

            // Seed Students
            val s1 = UserEntity("s_001", "Alex Rivera", "alex@student.shmschool.edu", "student")
            val s2 = UserEntity("s_002", "Sophia Chen", "sophia@student.shmschool.edu", "student")
            val s3 = UserEntity("s_003", "Marcus Vance", "marcus@student.shmschool.edu", "student")
            val s4 = UserEntity("s_004", "Elena Rostova", "elena@student.shmschool.edu", "student")
            schoolDao.insertUser(s1)
            schoolDao.insertUser(s2)
            schoolDao.insertUser(s3)
            schoolDao.insertUser(s4)

            // Seed Classes
            val c1 = SchoolClassEntity(
                id = "c_001",
                className = "CS101: Computer Science Fundamentals",
                subject = "Computer Science",
                bannerUrl = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97",
                joinCode = "CS101X",
                teacherId = "t_001",
                teacherName = "Prof. Alan Turing"
            )
            val c2 = SchoolClassEntity(
                id = "c_002",
                className = "PHY201: Quantum Mechanics & Space",
                subject = "Physics",
                bannerUrl = "https://images.unsplash.com/photo-1635070041078-e363dbe005cb",
                joinCode = "PHY202",
                teacherId = "t_002",
                teacherName = "Dr. Margaret Hamilton"
            )
            val c3 = SchoolClassEntity(
                id = "c_003",
                className = "MATH301: Linear Algebra & Logic",
                subject = "Mathematics",
                bannerUrl = "https://images.unsplash.com/photo-1509228468518-180dd4864904",
                joinCode = "MATH30",
                teacherId = "t_003",
                teacherName = "Prof. Ada Lovelace"
            )
            schoolDao.insertClass(c1)
            schoolDao.insertClass(c2)
            schoolDao.insertClass(c3)

            // Seed Enrollments
            schoolDao.insertEnrollment(EnrollmentEntity("e_001", "c_001", "s_001"))
            schoolDao.insertEnrollment(EnrollmentEntity("e_002", "c_001", "s_002"))
            schoolDao.insertEnrollment(EnrollmentEntity("e_003", "c_002", "s_001"))
            schoolDao.insertEnrollment(EnrollmentEntity("e_004", "c_003", "s_003"))
            schoolDao.insertEnrollment(EnrollmentEntity("e_005", "c_003", "s_004"))

            // Seed Assignments
            val a1 = AssignmentEntity(
                id = "a_001",
                classId = "c_001",
                className = "CS101: Computer Science Fundamentals",
                title = "Turing Machine & Complexity Analysis",
                description = "Implement a simple state transition machine and analyze time complexity.",
                dueDate = "2026-08-15",
                points = 100,
                attachmentUrl = "https://example.com/docs/cs101_assignment1.pdf",
                teacherId = "t_001"
            )
            val a2 = AssignmentEntity(
                id = "a_002",
                classId = "c_002",
                className = "PHY201: Quantum Mechanics & Space",
                title = "Apollo Flight Software Calculations",
                description = "Derive orbital insertion formulas and error correction algorithms.",
                dueDate = "2026-08-20",
                points = 100,
                attachmentUrl = "https://example.com/docs/phy201_lab.pdf",
                teacherId = "t_002"
            )
            schoolDao.insertAssignment(a1)
            schoolDao.insertAssignment(a2)

            // Seed Submissions
            val sub1 = SubmissionEntity(
                id = "sub_001",
                assignmentId = "a_001",
                assignmentTitle = "Turing Machine & Complexity Analysis",
                classId = "c_001",
                studentId = "s_001",
                studentName = "Alex Rivera",
                fileUrl = "https://cloudinary.com/shmschool/alex_turing_code.kt",
                comment = "Finished state transitions with unit tests.",
                grade = "98/100"
            )
            schoolDao.insertSubmission(sub1)
        }
    }

    fun generateJoinCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}

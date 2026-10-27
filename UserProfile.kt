package com.mad.talenttrack.data.model

import com.google.firebase.firestore.PropertyName

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val dateOfBirth: String = "",
    val location: String = "",
    val profilePhotoUrl: String = "",
    val resumeUrl: String = "",
    val resumeName: String = "",
    val resumeLastUpdated: Long = 0L,
    val resumeContent: String = "",
    val education: EducationDetails = EducationDetails(),
    val career: CareerPreferences = CareerPreferences(),
    val targetJob: JobTarget = JobTarget(),

val interviewDate: String = "",

val skills: List<Skill> = emptyList(),// Keeping legacy skills if needed, but we'll use the categorized ones
    
    // Categorized Skills
    val programmingLanguages: List<String> = emptyList(),
    val technicalSkills: List<String> = emptyList(),
    val softSkills: List<String> = emptyList(),
    
    // Career Interests
    val careerInterests: List<String> = emptyList(),

    val existingSkills: List<String> = emptyList(),
    val missingSkills: List<String> = emptyList(),
    val learningSchedule: Map<String, List<LearningTask>> = emptyMap(),
    val skillProgress: Map<String, Int> = emptyMap(),
    val readinessScore: Int = 0,
    @get:PropertyName("analyzed")
    val isAnalyzed: Boolean = false,
    val analysisSource: String = "", // "AI" or "Local"
    val analysisSummary: String = "",
    
    // Detailed AI Analysis Results
    val resumeMatchPercentage: Int = 0,
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val recommendedSkills: List<String> = emptyList(),
    val skillPriorities: Map<String, String> = emptyMap(), // Skill -> Priority (High/Medium/Low)
    val completedDays: List<Int> = emptyList(),
    val activeDayNumber: Int = 1
)

data class JobTarget(
    val companyId: String = "",
    val companyName: String = "",
    val roleId: String = "",
    val roleName: String = "",
    val jobDescription: String = "",
    val requiredSkills: List<String> = emptyList()
)

data class EducationDetails(
    val college: String = "",
    val degree: String = "",
    val department: String = "",
    val cgpa: String = "",
    val tenthPercentage: String = "",
    val twelfthPercentage: String = "",
    val graduationYear: String = "",
    val backlogs: Int = 0
)

data class CareerPreferences(
    val preferredRole: String = "",
    val preferredDomain: String = "",
    val preferredLocation: String = "",
    val expectedSalary: String = "",
    val preferredCompanyType: String = ""
)

data class Skill(
    val name: String = "",
    val level: Int = 1
)

data class LearningTask(
    val id: String = "",
    val title: String = "",
    val type: String = "",
    val isCompleted: Boolean = false,
    val skillRelated: String = ""
)
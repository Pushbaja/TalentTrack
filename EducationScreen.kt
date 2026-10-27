package com.mad.talenttrack.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mad.talenttrack.data.model.UserProfile
import com.mad.talenttrack.data.model.JobTarget
import com.mad.talenttrack.data.repository.JobRepository
import com.mad.talenttrack.ui.theme.*
import com.mad.talenttrack.utils.ResumeParser
import com.mad.talenttrack.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EducationScreen(
    onMenuClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val existingProfile by viewModel.userProfile
    val loading by viewModel.loading
    val loadingMessage by viewModel.loadingMessage
    val errorMessage by viewModel.errorMessage
    val context = LocalContext.current

    var profileState by remember(existingProfile) {
        mutableStateOf(existingProfile ?: UserProfile())
    }

    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (!ResumeParser.isFileSizeValid(context, it)) {
                Toast.makeText(context, "File too large. Max 5MB allowed.", Toast.LENGTH_SHORT).show()
                return@let
            }
            val fileName = ResumeParser.getFileName(context, it)
            if (ResumeParser.isValidFileType(fileName)) {
                val text = ResumeParser.extractTextFromPdf(context, it)
                if (!text.isNullOrBlank()) {
                    profileState = profileState.copy(
                        resumeUrl = "local://$fileName",
                        resumeName = fileName,
                        resumeContent = text
                    )
                    viewModel.saveResumeLocally(text, fileName)
                } else {
                    Toast.makeText(context, "Could not extract text from this file. Please upload a readable text-based PDF.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Invalid file type. Please select a PDF, DOC, or DOCX file.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resume & Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveProfile(profileState, {
                        Toast.makeText(context, "Profile Saved", Toast.LENGTH_SHORT).show()
                    }) }) {
                        Text("SAVE ALL", color = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentBlue)
                    Spacer(Modifier.height(16.dp))
                    Text(loadingMessage, color = Color.White)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Education Section
                Text("EDUCATION DETAILS", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val edu = profileState.education
                        EduTextField("Degree", edu.degree) { profileState = profileState.copy(education = edu.copy(degree = it)) }
                        EduTextField("Department", edu.department) { profileState = profileState.copy(education = edu.copy(department = it)) }
                        Row {
                            Box(modifier = Modifier.weight(1f)) {
                                EduTextField("CGPA", edu.cgpa, isNumeric = true) { profileState = profileState.copy(education = edu.copy(cgpa = it)) }
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                EduTextField("Grad Year", edu.graduationYear, isNumeric = true) { profileState = profileState.copy(education = edu.copy(graduationYear = it)) }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // 2. TARGET JOB SELECTION
                Text("TARGET JOB SELECTION", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val target = profileState.targetJob
                        
                        Text("Select a company and role to automatically load the Job Description.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Company Selection
                        var showCompanyDialog by remember { mutableStateOf(false) }
                        SelectionField(
                            label = "Select Company",
                            value = target.companyName.ifBlank { "Choose a company..." },
                            onClick = { showCompanyDialog = true }
                        )

                        if (showCompanyDialog) {
                            SelectionDialog(
                                title = "Select Company",
                                options = JobRepository.companies.map { it.name },
                                onSelect = { name ->
                                    val company = JobRepository.companies.find { it.name == name }
                                    if (company != null) {
                                        profileState = profileState.copy(
                                            targetJob = target.copy(
                                                companyId = company.id,
                                                companyName = company.name,
                                                roleId = "",
                                                roleName = "",
                                                jobDescription = "",
                                                requiredSkills = emptyList()
                                            )
                                        )
                                    }
                                    showCompanyDialog = false
                                },
                                onDismiss = { showCompanyDialog = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Job Role Selection
                        val selectedCompany = JobRepository.companies.find { it.id == target.companyId }
                        var showRoleDialog by remember { mutableStateOf(false) }
                        
                        SelectionField(
                            label = "Select Job Role",
                            value = target.roleName.ifBlank { "Choose a role..." },
                            enabled = target.companyId.isNotBlank(),
                            onClick = { showRoleDialog = true }
                        )

                        if (showRoleDialog && selectedCompany != null) {
                            SelectionDialog(
                                title = "Select Role for ${selectedCompany.name}",
                                options = selectedCompany.roles.map { it.title },
                                onSelect = { title ->
                                    val role = selectedCompany.roles.find { it.title == title }
                                    if (role != null) {
                                        profileState = profileState.copy(
                                            targetJob = target.copy(
                                                roleId = role.id,
                                                roleName = role.title,
                                                jobDescription = role.description,
                                                requiredSkills = role.requiredSkills
                                            )
                                        )
                                    }
                                    showRoleDialog = false
                                },
                                onDismiss = { showRoleDialog = false }
                            )
                        }

                        if (target.jobDescription.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Job Description", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(target.jobDescription, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(vertical = 4.dp))
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Required Skills", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            FlowRow(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                target.requiredSkills.forEach { skill ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(skill, fontSize = 10.sp) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color.White, containerColor = AccentBlue.copy(alpha = 0.2f))
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // 3. RESUME MANAGEMENT SECTION
                Text("RESUME MANAGEMENT", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (profileState.resumeUrl.isBlank()) {
                            Text("No Resume Uploaded", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Please upload your PDF resume to start analysis.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Resume: ${profileState.resumeName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                                    val dateStr = sdf.format(Date(profileState.resumeLastUpdated))
                                    Text("Uploaded: $dateStr", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { filePickerLauncher.launch("application/pdf") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !loading
                            ) {
                                Icon(if (profileState.resumeUrl.isBlank()) Icons.Default.Upload else Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (profileState.resumeUrl.isBlank()) "Upload" else "Replace")
                            }
                            
                            if (profileState.resumeUrl.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { 
                                        viewModel.analyzeResume(
                                            jobTarget = profileState.targetJob,
                                            resumeTextOverride = profileState.resumeContent
                                        ) 
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !loading && profileState.targetJob.jobDescription.isNotBlank()
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Analyze")
                                }
                            }
                        }
                        
                        if (errorMessage != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }

                // 4. AI RESULTS
                if (profileState.resumeUrl.isNotBlank() && profileState.isAnalyzed) {
                    Spacer(modifier = Modifier.height(24.dp))
                    AnalysisResultsSection(profileState)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SelectionField(label: String, value: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(enabled = enabled, onClick = onClick)) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (enabled) Color.Transparent else Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .height(56.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = if (enabled) Color.White else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = if (enabled) AccentBlue else Color.Gray)
            }
        }
        HorizontalDivider(color = if (enabled) Color.Gray else Color.DarkGray, thickness = 1.dp)
    }
}

@Composable
fun SelectionDialog(title: String, options: List<String>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Text(
                        text = option,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        color = Color.White
                    )
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AccentBlue) }
        },
        containerColor = CardBackground
    )
}

@Composable
private fun EduTextField(
    label: String, 
    value: String, 
    isNumeric: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = if (isNumeric) androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ) else androidx.compose.foundation.text.KeyboardOptions.Default,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Gray,
                focusedBorderColor = AccentBlue,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            )
        )
    }
}

@Composable
fun AnalysisResultsSection(profile: UserProfile) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ANALYSIS RESULT", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Surface(
                color = if (profile.analysisSource == "AI") AccentGreen.copy(alpha = 0.2f) else AccentOrange.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (profile.analysisSource == "AI") "AI Analysis" else "Local Demo Analysis",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (profile.analysisSource == "AI") AccentGreen else AccentOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ScoreInfo("Match Score", "${profile.resumeMatchPercentage}%", AccentBlue)
                    ScoreInfo("Readiness", "${profile.readinessScore}%", AccentGreen)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Existing Skills", color = AccentGreen, fontWeight = FontWeight.Bold)
                profile.existingSkills.forEach { Text("• $it", color = Color.White, fontSize = 13.sp) }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Missing Skills", color = Color.Red, fontWeight = FontWeight.Bold)
                profile.missingSkills.forEach { skill ->
                    val priority = profile.skillPriorities[skill] ?: "Low"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• $skill", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(
                            priority, 
                            color = when(priority) {
                                "High" -> Color.Red
                                "Medium" -> AccentOrange
                                else -> Color.Gray
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("AI Summary", color = AccentBlue, fontWeight = FontWeight.Bold)
                Text(profile.analysisSummary, color = Color.White, fontSize = 13.sp)
                
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "Check your Dashboard for the updated learning schedule!",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreInfo(label: String, score: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(score, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

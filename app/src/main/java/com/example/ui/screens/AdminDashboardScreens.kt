package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdminQuestionDraft
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.CsvResultExporter
import com.example.data.model.ExamCategory
import com.example.data.model.ExamSubject
import com.example.data.model.ExamTest
import com.example.data.model.MathChapters
import com.example.data.model.NoteMaterialType
import com.example.data.model.RankingMode
import com.example.data.model.StudentRosterEntry
import com.example.data.model.StudyNotePdf
import com.example.data.model.TestResult
import com.example.data.model.UserProfile
import com.example.ui.components.MbsEmptyStateCard
import com.example.ui.components.TeacherPortraitCircle
import com.example.ui.theme.AmberTint
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CoralTint
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoTint
import com.example.ui.theme.LocalMbsColors
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintTint
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkTint
import com.example.ui.viewmodel.MbsViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AdminSection(val label: String) {
    OVERVIEW("Overview"),
    STUDENTS("Students"),
    TESTS("Tests"),
    RESULTS("Results"),
    NOTES("Notes & PDF"),
    ALERTS("Alerts & Settings")
}

@Composable
fun AdminDashboardScreen(
    viewModel: MbsViewModel,
    onSwitchToStudentPreview: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalMbsColors.current

    val usersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val rosterState by viewModel.adminRosterState.collectAsStateWithLifecycle()
    val testsState by viewModel.adminTestsState.collectAsStateWithLifecycle()
    val resultsState by viewModel.adminResultsState.collectAsStateWithLifecycle()
    val notesState by viewModel.adminNotesState.collectAsStateWithLifecycle()
    val announcementsState by viewModel.adminAnnouncementsState.collectAsStateWithLifecycle()
    val settingsState by viewModel.appSettingsState.collectAsStateWithLifecycle()

    val users = (usersState as? UiState.Success)?.data.orEmpty()
    val roster = (rosterState as? UiState.Success)?.data.orEmpty()
    val tests = (testsState as? UiState.Success)?.data.orEmpty()
    val results = (resultsState as? UiState.Success)?.data.orEmpty()
    val notes = (notesState as? UiState.Success)?.data.orEmpty()
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
    val settings = (settingsState as? UiState.Success)?.data ?: AppSettings()

    var selectedSection by remember { mutableStateOf(AdminSection.OVERVIEW) }
    var showTestEditor by remember { mutableStateOf(false) }
    var editingTest by remember { mutableStateOf<ExamTest?>(null) }
    var editingTestQuestions by remember { mutableStateOf<List<AdminQuestionDraft>>(emptyList()) }
    var showAddRosterDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<UserProfile?>(null) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var pendingCsvContent by remember { mutableStateOf("") }

    // Zero-permission SAF CSV Result Export
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingCsvContent.isNotBlank()) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(pendingCsvContent.toByteArray(Charsets.UTF_8))
                        }
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                viewModel.postMessage(if (ok) "Results exported to CSV successfully!" else "CSV export failed.")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .testTag("admin_dashboard_screen")
    ) {
        // Deep Navy Admin Top Header (Matches Reference Image 9)
        Surface(color = NavyDeep) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeacherPortraitCircle(size = 44.dp, ringWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Maths By Mithlesh Sir",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldenYellow
                        )
                    }
                    Surface(
                        onClick = onSwitchToStudentPreview,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.testTag("admin_student_preview_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.School,
                                contentDescription = "Student view",
                                tint = GoldenYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Student", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AdminSection.entries) { section ->
                        val active = selectedSection == section
                        FilterChip(
                            selected = active,
                            onClick = { selectedSection = section },
                            label = { Text(section.label, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                labelColor = Color.White,
                                selectedContainerColor = GoldenYellow,
                                selectedLabelColor = NavyDeep
                            )
                        )
                    }
                }
            }
        }

        when (selectedSection) {
            AdminSection.OVERVIEW -> AdminOverviewTab(
                studentsCount = users.count { it.role != "admin" } + roster.count { it.linkedUid.isBlank() },
                testsCount = tests.size,
                attemptsCount = results.size,
                avgScore = if (results.isNotEmpty()) results.map { it.percentage }.average().toInt() else 0,
                recentResults = results.take(5),
                onCreateTest = {
                    editingTest = null
                    editingTestQuestions = emptyList()
                    showTestEditor = true
                },
                onAddStudent = { showAddRosterDialog = true },
                onSeedStarter = { viewModel.seedStarterCurriculum() }
            )

            AdminSection.STUDENTS -> AdminStudentsTab(
                users = users,
                roster = roster,
                onAddRoster = { showAddRosterDialog = true },
                onEditStudent = { editingStudent = it },
                onToggleRosterActive = { id, act -> viewModel.toggleRosterActive(id, act) },
                onResetInfo = {
                    viewModel.postMessage(
                        "Student accounts use secure Google Sign-In (zero passwords stored in Firestore). Manage recovery via Firebase Auth Console or Cloud Functions."
                    )
                }
            )

            AdminSection.TESTS -> AdminTestsTab(
                tests = tests,
                onCreateTest = {
                    editingTest = null
                    editingTestQuestions = emptyList()
                    showTestEditor = true
                },
                onEditTest = { test ->
                    viewModel.loadAdminQuestions(test.id) { loaded ->
                        editingTest = test
                        editingTestQuestions = loaded
                        showTestEditor = true
                    }
                },
                onTogglePublish = { test, pub -> viewModel.togglePublishTest(test, pub) },
                onDeleteTest = { viewModel.deleteTest(it.id) }
            )

            AdminSection.RESULTS -> AdminResultsTab(
                results = results,
                tests = tests,
                onExportCsv = { filtered ->
                    pendingCsvContent = CsvResultExporter.toCsv(filtered)
                    exportCsvLauncher.launch("MBS_Student_Results.csv")
                }
            )

            AdminSection.NOTES -> AdminNotesTab(
                notes = notes,
                onAddNote = { showAddNoteDialog = true },
                onDeleteNote = { viewModel.deleteNote(it.id) }
            )

            AdminSection.ALERTS -> AdminAlertsAndSettingsTab(
                announcements = announcements,
                settings = settings,
                onSendAnnouncement = { title, msg, cat, batch, done ->
                    viewModel.sendAnnouncement(title, msg, cat, batch, done)
                },
                onDeleteAnnouncement = { viewModel.deleteAnnouncement(it.id) },
                onUpdateSettings = { lb, mode, welcome ->
                    viewModel.updateSettings(lb, mode, welcome)
                }
            )
        }
    }

    if (showTestEditor) {
        AdminTestEditorDialog(
            editingTest = editingTest,
            initialQuestions = editingTestQuestions,
            onDismiss = { showTestEditor = false },
            onSave = { id, title, desc, cat, type, subj, chap, dur, marks, neg, pub, feat, mode, batch, sids, qs ->
                viewModel.saveTestAdmin(
                    existingTestId = id,
                    title = title,
                    description = desc,
                    category = cat,
                    testType = type,
                    subject = subj,
                    chapter = chap,
                    durationMinutes = dur,
                    totalMarks = marks,
                    negativeMarking = neg,
                    publishNow = pub,
                    featuredToday = feat,
                    assignmentMode = mode,
                    assignedBatch = batch,
                    assignedStudentIds = sids,
                    questions = qs,
                    onSuccess = { showTestEditor = false }
                )
            }
        )
    }

    if (showAddRosterDialog) {
        AddStudentIdDialog(
            onCreate = { sid, name, batch, cat ->
                viewModel.addRosterStudent(sid, name, batch, cat)
                showAddRosterDialog = false
            },
            onDismiss = { showAddRosterDialog = false }
        )
    }

    editingStudent?.let { stu ->
        EditStudentDialog(
            student = stu,
            onSave = { name, sid, batch, act ->
                viewModel.updateStudentAdmin(stu.userId, name, sid, batch, act)
                editingStudent = null
            },
            onDismiss = { editingStudent = null }
        )
    }

    if (showAddNoteDialog) {
        AddStudyNoteDialog(
            onPublish = { title, subj, chap, type, cat, desc, url, md, size ->
                viewModel.saveNoteAdmin(title, subj, chap, type, cat, desc, url, md, size) {
                    showAddNoteDialog = false
                }
            },
            onDismiss = { showAddNoteDialog = false }
        )
    }
}

@Composable
private fun AdminOverviewTab(
    studentsCount: Int,
    testsCount: Int,
    attemptsCount: Int,
    avgScore: Int,
    recentResults: List<TestResult>,
    onCreateTest: () -> Unit,
    onAddStudent: () -> Unit,
    onSeedStarter: () -> Unit
) {
    val ext = LocalMbsColors.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminKpiCard("Total Students", "$studentsCount", IndigoTint, IndigoAccent, Modifier.weight(1f))
                AdminKpiCard("Total Tests", "$testsCount", AmberTint, NavyDeep, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminKpiCard("Submissions", "$attemptsCount", MintTint, MintDark, Modifier.weight(1f))
                AdminKpiCard("Avg Score", "$avgScore%", PinkTint, PinkAccent, Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onCreateTest,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("admin_quick_create_test")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Test")
                }
                Button(
                    onClick = onAddStudent,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenYellow, contentColor = NavyDeep),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("admin_quick_add_student")
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Student ID", fontWeight = FontWeight.Bold)
                }
            }
        }

        // One-tap Bilingual Starter Curriculum Publisher (real Firestore write)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = IndigoTint),
                border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = IndigoAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Quick-Start Bilingual Curriculum",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NavyDeep
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Publish 5 real bilingual mock tests (Navodaya, Sainik School, Simultala, Olympiad), 3 PDF study notes, and an announcement directly to Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onSeedStarter,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        modifier = Modifier.testTag("publish_starter_curriculum_button")
                    ) {
                        Text("Publish Starter Tests & PDFs")
                    }
                }
            }
        }

        item {
            Text(
                "Recent Student Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyDeep
            )
        }

        if (recentResults.isEmpty()) {
            item {
                MbsEmptyStateCard(
                    title = "No Test Submissions Yet",
                    subtitle = "Real student exam submissions will stream here automatically."
                )
            }
        } else {
            items(recentResults, key = { it.id }) { res ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(res.studentName, fontWeight = FontWeight.Bold, color = NavyDeep)
                            Text(
                                "${res.testTitle} • ${res.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = MintTint) {
                            Text(
                                "${res.percentage.toInt()}%",
                                color = MintDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    bg: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(20.dp), color = bg, modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = NavyDeep.copy(alpha = 0.72f))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = accent)
        }
    }
}

@Composable
private fun AdminStudentsTab(
    users: List<UserProfile>,
    roster: List<StudentRosterEntry>,
    onAddRoster: () -> Unit,
    onEditStudent: (UserProfile) -> Unit,
    onToggleRosterActive: (String, Boolean) -> Unit,
    onResetInfo: () -> Unit
) {
    val ext = LocalMbsColors.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddRoster,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create Student ID")
                }
                OutlinedButton(
                    onClick = onResetInfo,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Password Policy")
                }
            }
        }

        item {
            Text(
                "Registered Students (${users.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyDeep
            )
        }
        items(users, key = { it.id }) { stu ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${stu.name} (${stu.studentId})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NavyDeep
                        )
                        Text(
                            "${stu.email} • Batch: ${stu.batch}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (stu.active) "Active • ${stu.totalTestsAttempted} tests • ${stu.averagePercentage.toInt()}% avg" else "Deactivated",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (stu.active) MintDark else CoralAccent
                        )
                    }
                    IconButton(onClick = { onEditStudent(stu) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit student", tint = NavyDeep)
                    }
                }
            }
        }

        item {
            Text(
                "Pre-Issued Student ID Roster (${roster.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyDeep
            )
        }
        items(roster, key = { it.id }) { entry ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${entry.studentId} • ${entry.name}", fontWeight = FontWeight.Bold, color = NavyDeep)
                        Text(
                            "${entry.batch} • ${entry.targetCategory} • ${if (entry.linkedUid.isBlank()) "Unclaimed" else "Linked"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = entry.active,
                        onCheckedChange = { onToggleRosterActive(entry.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminTestsTab(
    tests: List<ExamTest>,
    onCreateTest: () -> Unit,
    onEditTest: (ExamTest) -> Unit,
    onTogglePublish: (ExamTest, Boolean) -> Unit,
    onDeleteTest: (ExamTest) -> Unit
) {
    val ext = LocalMbsColors.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Button(
                onClick = onCreateTest,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admin_create_test_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create New Test & Add Questions", fontWeight = FontWeight.Bold)
            }
        }

        if (tests.isEmpty()) {
            item {
                MbsEmptyStateCard(
                    title = "No Tests Created Yet",
                    subtitle = "Create a Full or Sectional test or use Quick-Start on Overview to publish bilingual tests."
                )
            }
        } else {
            items(tests, key = { it.id }) { test ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(test.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NavyDeep)
                                Text(
                                    "${test.category} • ${test.testType} • ${test.questionCount} Qs • ${test.durationMinutes} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Target: ${test.assignmentMode}${if (test.assignedBatch.isNotBlank()) " (${test.assignedBatch})" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndigoAccent
                                )
                            }
                            Switch(
                                checked = test.published,
                                onCheckedChange = { onTogglePublish(test, it) }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (test.published) MintTint else AmberTint
                            ) {
                                Text(
                                    text = if (test.published) "PUBLISHED LIVE" else "DRAFT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (test.published) MintDark else NavyDeep,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { onEditTest(test) }) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit")
                            }
                            TextButton(onClick = { onDeleteTest(test) }) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete", color = CoralAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminResultsTab(
    results: List<TestResult>,
    tests: List<ExamTest>,
    onExportCsv: (List<TestResult>) -> Unit
) {
    val ext = LocalMbsColors.current
    var selectedTestId by remember { mutableStateOf("ALL") }
    val filtered = remember(results, selectedTestId) {
        if (selectedTestId == "ALL") results else results.filter { it.testId == selectedTestId }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Student Results (${filtered.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { onExportCsv(filtered) },
                    enabled = filtered.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenYellow, contentColor = NavyDeep),
                    modifier = Modifier.testTag("export_results_csv_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export CSV", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedTestId == "ALL",
                        onClick = { selectedTestId = "ALL" },
                        label = { Text("All Tests") }
                    )
                }
                items(tests, key = { it.id }) { t ->
                    FilterChip(
                        selected = selectedTestId == t.id,
                        onClick = { selectedTestId = t.id },
                        label = { Text(t.title, maxLines = 1) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                MbsEmptyStateCard(
                    title = "No Student Results",
                    subtitle = "Submitted results will appear here and can be exported as CSV."
                )
            }
        } else {
            items(filtered, key = { it.id }) { r ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${r.studentName} (${r.studentRollNo})", fontWeight = FontWeight.Bold, color = NavyDeep)
                            Text(r.testTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Correct ${r.correctCount} • Wrong ${r.wrongCount} • Accuracy ${r.accuracy.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoAccent
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${r.obtainedMarks}/${r.totalMarks.toInt()}", fontWeight = FontWeight.ExtraBold, color = NavyDeep)
                            Text("${r.percentage.toInt()}%", color = MintDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminNotesTab(
    notes: List<StudyNotePdf>,
    onAddNote: () -> Unit,
    onDeleteNote: (StudyNotePdf) -> Unit
) {
    val ext = LocalMbsColors.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Button(
                onClick = onAddNote,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Upload / Create Study PDF", fontWeight = FontWeight.Bold)
            }
        }
        items(notes, key = { it.id }) { note ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(note.title, fontWeight = FontWeight.Bold, color = NavyDeep)
                        Text(
                            "${note.subject} • ${note.chapter} • ${NoteMaterialType.fromValue(note.materialType).label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteNote(note) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = CoralAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAlertsAndSettingsTab(
    announcements: List<Announcement>,
    settings: AppSettings,
    onSendAnnouncement: (String, String, String, String, () -> Unit) -> Unit,
    onDeleteAnnouncement: (Announcement) -> Unit,
    onUpdateSettings: (Boolean, String, String) -> Unit
) {
    val ext = LocalMbsColors.current
    var annTitle by remember { mutableStateOf("") }
    var annMsg by remember { mutableStateOf("") }
    var annCategory by remember { mutableStateOf("ALL") }

    var lbEnabled by remember(settings.leaderboardEnabled) { mutableStateOf(settings.leaderboardEnabled) }
    var rankingMode by remember(settings.rankingMode) { mutableStateOf(settings.rankingMode) }
    var welcomeMsg by remember(settings.welcomeMessage) { mutableStateOf(settings.welcomeMessage) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Send Announcement Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = NavyDeep)
                        Spacer(Modifier.width(8.dp))
                        Text("Send Announcement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = annTitle,
                        onValueChange = { annTitle = it },
                        label = { Text("Announcement Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = annMsg,
                        onValueChange = { annMsg = it },
                        label = { Text("Message (Hindi or English)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("ALL") + ExamCategory.entries.map { it.firestoreValue }) { cat ->
                            FilterChip(
                                selected = annCategory == cat,
                                onClick = { annCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onSendAnnouncement(annTitle, annMsg, annCategory, "") {
                                annTitle = ""
                                annMsg = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Broadcast Announcement")
                    }
                }
            }
        }

        // Leaderboard Settings Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Leaderboard & App Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Leaderboard to Students", fontWeight = FontWeight.Medium)
                        Switch(checked = lbEnabled, onCheckedChange = { lbEnabled = it })
                    }
                    Text("Ranking Mode", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RankingMode.entries.forEach { mode ->
                            FilterChip(
                                selected = rankingMode == mode.firestoreValue,
                                onClick = { rankingMode = mode.firestoreValue },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = welcomeMsg,
                        onValueChange = { welcomeMsg = it },
                        label = { Text("Welcome Tagline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onUpdateSettings(lbEnabled, rankingMode, welcomeMsg) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenYellow, contentColor = NavyDeep),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Announcements List
        items(announcements, key = { it.id }) { ann ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ann.title, fontWeight = FontWeight.Bold, color = NavyDeep)
                        Text(ann.message, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onDeleteAnnouncement(ann) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = CoralAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddStudentIdDialog(
    onCreate: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var studentId by remember { mutableStateOf("MBS-2026-") }
    var name by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("Navodaya-A") }
    var cat by remember { mutableStateOf(ExamCategory.NAVODAYA.firestoreValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Student ID") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = studentId, onValueChange = { studentId = it.uppercase() }, label = { Text("Student ID / Roll No") }, singleLine = true)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Student Full Name") }, singleLine = true)
                OutlinedTextField(value = batch, onValueChange = { batch = it }, label = { Text("Batch Name") }, singleLine = true)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ExamCategory.entries) { c ->
                        FilterChip(selected = cat == c.firestoreValue, onClick = { cat = c.firestoreValue }, label = { Text(c.firestoreValue) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(studentId, name, batch, cat) },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
            ) { Text("Create ID") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditStudentDialog(
    student: UserProfile,
    onSave: (String, String, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(student.name) }
    var studentId by remember { mutableStateOf(student.studentId) }
    var batch by remember { mutableStateOf(student.batch) }
    var active by remember { mutableStateOf(student.active) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Student") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = studentId, onValueChange = { studentId = it }, label = { Text("Student ID") }, singleLine = true)
                OutlinedTextField(value = batch, onValueChange = { batch = it }, label = { Text("Batch") }, singleLine = true)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Active")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, studentId, batch, active) },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddStudyNoteDialog(
    onPublish: (String, String, String, String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(ExamSubject.MATHEMATICS.firestoreValue) }
    var chapter by remember { mutableStateOf(MathChapters.ALL.first()) }
    var type by remember { mutableStateOf(NoteMaterialType.FORMULA_SHEET.firestoreValue) }
    var category by remember { mutableStateOf(ExamCategory.NAVODAYA.firestoreValue) }
    var pdfUrl by remember { mutableStateOf("") }
    var contentMarkdown by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Study PDF / Note") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExamSubject.entries) { s ->
                            FilterChip(selected = subject == s.firestoreValue, onClick = { subject = s.firestoreValue }, label = { Text(s.firestoreValue) })
                        }
                    }
                }
                item { OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text("Chapter") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(NoteMaterialType.entries) { t ->
                            FilterChip(selected = type == t.firestoreValue, onClick = { type = t.firestoreValue }, label = { Text(t.label) })
                        }
                    }
                }
                item { OutlinedTextField(value = pdfUrl, onValueChange = { pdfUrl = it }, label = { Text("Hosted PDF URL (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    OutlinedTextField(
                        value = contentMarkdown,
                        onValueChange = { contentMarkdown = it },
                        label = { Text("Notes / Formulas (auto-generates PDF)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().length >= 2) {
                        onPublish(title, subject, chapter, type, category, "", pdfUrl, contentMarkdown, "1.2 MB")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
            ) { Text("Publish PDF") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

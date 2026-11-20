package com.example.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.PdfDocumentHelper
import com.example.data.model.AppSettings
import com.example.data.model.ExamCategory
import com.example.data.model.ExamSubject
import com.example.data.model.LeaderboardEntry
import com.example.data.model.NoteMaterialType
import com.example.data.model.RankingMode
import com.example.data.model.StudyNotePdf
import com.example.data.model.UserProfile
import com.example.ui.components.MbsEmptyStateCard
import com.example.ui.components.MbsLogoBadge
import com.example.ui.components.PdfPreviewDialog
import com.example.ui.components.TeacherPortraitCircle
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CoralTint
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.GoldenSoft
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoTint
import com.example.ui.theme.LocalMbsColors
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintTint
import com.example.ui.theme.NavyDeep
import kotlinx.coroutines.launch
import java.io.File

// --- NOTES & PDF LIBRARY SCREEN (Requirement 10) ---

@Composable
fun NotesPdfScreen(
    notes: List<StudyNotePdf>,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalMbsColors.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }
    var selectedType by remember { mutableStateOf("ALL") }

    var previewNote by remember { mutableStateOf<StudyNotePdf?>(null) }
    var previewLoading by remember { mutableStateOf(false) }
    var previewPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var previewError by remember { mutableStateOf<String?>(null) }
    var pendingDownloadFile by remember { mutableStateOf<File?>(null) }

    // Play-policy compliant zero-permission SAF PDF save
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val src = pendingDownloadFile
        if (targetUri != null && src != null) {
            scope.launch {
                PdfDocumentHelper.copyFileToSafUri(context, src, targetUri).fold(
                    onSuccess = { onNotify("PDF saved successfully!") },
                    onFailure = { onNotify(it.localizedMessage ?: "Failed to save PDF.") }
                )
            }
        }
    }

    fun openPreview(note: StudyNotePdf) {
        previewNote = note
        previewLoading = true
        previewError = null
        previewPages = emptyList()
        scope.launch {
            PdfDocumentHelper.preparePdfFile(context, note).fold(
                onSuccess = { file ->
                    pendingDownloadFile = file
                    PdfDocumentHelper.renderPdfPages(file).fold(
                        onSuccess = { pages ->
                            previewPages = pages
                            previewLoading = false
                        },
                        onFailure = { err ->
                            previewError = err.localizedMessage ?: "Unable to render PDF pages."
                            previewLoading = false
                        }
                    )
                },
                onFailure = { err ->
                    previewError = err.localizedMessage ?: "Unable to prepare PDF."
                    previewLoading = false
                }
            )
        }
    }

    fun triggerDownload(note: StudyNotePdf) {
        scope.launch {
            PdfDocumentHelper.preparePdfFile(context, note).fold(
                onSuccess = { file ->
                    pendingDownloadFile = file
                    val cleanName = note.title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
                        .ifBlank { "MBS_Study_Note" }
                    savePdfLauncher.launch("${cleanName}.pdf")
                },
                onFailure = { onNotify(it.localizedMessage ?: "Unable to prepare PDF for download.") }
            )
        }
    }

    val filteredNotes = remember(notes, searchQuery, selectedSubject, selectedType) {
        notes.filter { note ->
            val matchSubj = selectedSubject == "All" || note.subject.equals(selectedSubject, ignoreCase = true)
            val matchType = selectedType == "ALL" || note.materialType.equals(selectedType, ignoreCase = true)
            val q = searchQuery.trim()
            val matchQuery = q.isBlank() ||
                note.title.contains(q, ignoreCase = true) ||
                note.chapter.contains(q, ignoreCase = true) ||
                note.subject.contains(q, ignoreCase = true)
            matchSubj && matchType && matchQuery
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("notes_pdf_screen")
    ) {
        item {
            Text(
                text = "Notes & PDF Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = NavyDeep
            )
            Text(
                text = "विषयवार नोट्स, फॉर्मूला शीट, वर्कशीट और पिछले वर्ष के पेपर",
                fontFamily = DevanagariFontFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chapter, formula sheet, paper...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = NavyDeep) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = NavyDeep,
                    unfocusedBorderColor = ext.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_search_input")
            )
        }

        item {
            val subjects = listOf("All") + ExamSubject.entries.map { it.firestoreValue }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjects) { subj ->
                    FilterChip(
                        selected = selectedSubject == subj,
                        onClick = { selectedSubject = subj },
                        label = { Text(subj) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyDeep,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        item {
            val types = listOf("ALL" to "All Types") + NoteMaterialType.entries.map { it.firestoreValue to it.label }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(types) { (key, label) ->
                    FilterChip(
                        selected = selectedType == key,
                        onClick = { selectedType = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldenYellow,
                            selectedLabelColor = NavyDeep
                        )
                    )
                }
            }
        }

        if (filteredNotes.isEmpty()) {
            item {
                MbsEmptyStateCard(
                    title = "No Study PDFs Match",
                    subtitle = "Published chapter notes, formula sheets, worksheets, and previous year papers will appear here."
                )
            }
        } else {
            items(filteredNotes, key = { it.id }) { note ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_card_${note.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CoralTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = "PDF Document",
                                tint = CoralAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NavyDeep,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${note.subject} • ${note.chapter} • ${note.fileSizeLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = IndigoTint) {
                                Text(
                                    text = NoteMaterialType.fromValue(note.materialType).label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndigoAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { openPreview(note) },
                            modifier = Modifier.testTag("preview_pdf_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = "Preview PDF",
                                tint = NavyDeep
                            )
                        }
                        IconButton(
                            onClick = { triggerDownload(note) },
                            modifier = Modifier.testTag("download_pdf_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download PDF",
                                tint = IndigoAccent
                            )
                        }
                    }
                }
            }
        }
    }

    previewNote?.let { activeNote ->
        PdfPreviewDialog(
            note = activeNote,
            loading = previewLoading,
            pages = previewPages,
            error = previewError,
            onDownloadClick = {
                val cleanName = activeNote.title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
                    .ifBlank { "MBS_Study_Note" }
                savePdfLauncher.launch("${cleanName}.pdf")
            },
            onDismiss = { previewNote = null }
        )
    }
}

// --- LEADERBOARD SCREEN (Requirement 11) ---

private data class RankedRow(
    val userId: String,
    val studentName: String,
    val batch: String,
    val totalMarks: Double,
    val averagePercentage: Double,
    val testsCompleted: Int
)

@Composable
fun LeaderboardScreen(
    currentUserId: String,
    entries: List<LeaderboardEntry>,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val ext = LocalMbsColors.current
    var selectedScope by remember { mutableStateOf("Overall") }
    val scopes = listOf("Overall") + ExamCategory.entries.map { it.firestoreValue }

    val ranked = remember(entries, selectedScope, settings.rankingMode) {
        val rows = entries.mapNotNull { entry ->
            if (selectedScope == "Overall") {
                if (entry.testsCompleted <= 0L) null
                else RankedRow(
                    userId = entry.userId,
                    studentName = entry.studentName,
                    batch = entry.batch,
                    totalMarks = entry.totalMarks,
                    averagePercentage = entry.averagePercentage,
                    testsCompleted = entry.testsCompleted.toInt()
                )
            } else {
                val catMap = entry.categoryStats[selectedScope] ?: return@mapNotNull null
                val tests = (catMap["testsCompleted"] as? Number)?.toInt() ?: 0
                if (tests <= 0) null
                else RankedRow(
                    userId = entry.userId,
                    studentName = entry.studentName,
                    batch = entry.batch,
                    totalMarks = (catMap["totalMarks"] as? Number)?.toDouble() ?: 0.0,
                    averagePercentage = (catMap["averagePercentage"] as? Number)?.toDouble() ?: 0.0,
                    testsCompleted = tests
                )
            }
        }
        val mode = RankingMode.fromValue(settings.rankingMode)
        when (mode) {
            RankingMode.TOTAL_MARKS -> rows.sortedWith(
                compareByDescending<RankedRow> { it.totalMarks }.thenByDescending { it.averagePercentage }
            )
            RankingMode.PERCENTAGE -> rows.sortedWith(
                compareByDescending<RankedRow> { it.averagePercentage }.thenByDescending { it.totalMarks }
            )
        }
    }

    val myRankIndex = ranked.indexOfFirst { it.userId == currentUserId }
    val myRow = if (myRankIndex >= 0) ranked[myRankIndex] else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("leaderboard_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Text(
                    text = "Leaderboard • रैंक सूची",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyDeep
                )
                Text(
                    text = "Based on actual submitted test results only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!settings.leaderboardEnabled) {
                item {
                    MbsEmptyStateCard(
                        title = "Leaderboard Paused by Admin",
                        subtitle = "Mithlesh Sir has temporarily hidden leaderboard rankings in settings."
                    )
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(scopes) { sc ->
                            FilterChip(
                                selected = selectedScope == sc,
                                onClick = { selectedScope = sc },
                                label = { Text(sc) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyDeep,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                if (ranked.isEmpty()) {
                    item {
                        MbsEmptyStateCard(
                            title = "No Rankings Yet",
                            subtitle = "Submit your first mock test to appear on the real-time leaderboard!"
                        )
                    }
                } else {
                    itemsIndexed(ranked, key = { _, item -> item.userId }) { idx, row ->
                        val rank = idx + 1
                        val isMe = row.userId == currentUserId
                        val medalColor = when (rank) {
                            1 -> GoldenYellow
                            2 -> Color(0xFFB4B9C8)
                            3 -> Color(0xFFCD7F32)
                            else -> ext.neutralPill
                        }
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) GoldenSoft else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isMe) 2.dp else 1.dp,
                                color = if (isMe) GoldenYellow else ext.cardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(medalColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$rank",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (rank == 3) Color.White else NavyDeep
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isMe) "${row.studentName} (You)" else row.studentName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDeep
                                    )
                                    Text(
                                        text = "${row.testsCompleted} Tests • ${row.totalMarks.toInt()} Marks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MintTint
                                ) {
                                    Text(
                                        text = "${row.averagePercentage.toInt()}%",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MintDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navy Your Rank Card (Matches Reference Image 9)
        if (settings.leaderboardEnabled && myRow != null) {
            Surface(
                color = NavyDeep,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = GoldenYellow,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Your Rank ($selectedScope)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Text(
                                text = "#${myRankIndex + 1}  •  ${myRow.averagePercentage.toInt()}% Avg",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldenYellow
                            )
                        }
                    }
                    Text(
                        text = "${myRow.totalMarks.toInt()} Marks",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// --- STUDENT PROFILE & TEACHER PROFILE SCREEN ---

@Composable
fun StudentProfileScreen(
    profile: UserProfile,
    verifiedAdmin: Boolean,
    onSwitchToAdmin: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMbsColors.current
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("student_profile_screen")
    ) {
        // Student Identity Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(NavyDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.name.take(2).uppercase().ifBlank { "ST" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldenYellow
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NavyDeep
                            )
                            Text(
                                text = "ID: ${profile.studentId}  •  Batch: ${profile.batch}",
                                style = MaterialTheme.typography.bodySmall,
                                color = IndigoAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (profile.email.isNotBlank()) {
                                Text(
                                    text = profile.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileMiniStat("Streak", "${profile.streakDays}d", Modifier.weight(1f))
                        ProfileMiniStat("Tests", "${profile.totalTestsAttempted}", Modifier.weight(1f))
                        ProfileMiniStat("Average", "${profile.averagePercentage.toInt()}%", Modifier.weight(1f))
                    }
                }
            }
        }

        // Official Teacher Profile Card (Mithlesh Sir — uses corrected portrait)
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDeep),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_profile_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TeacherPortraitCircle(size = 110.dp, ringWidth = 3.5.dp)
                        Spacer(Modifier.width(14.dp))
                        MbsLogoBadge(size = 58.dp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.teacher_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldenYellow
                    )
                    Text(
                        text = stringResource(R.string.teacher_role),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.app_tagline_hi),
                        fontFamily = DevanagariFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldenYellow.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.teacher_bio),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                }
            }
        }

        // Admin Portal Switch (Only for Verified Admins)
        if (verifiedAdmin) {
            item {
                Button(
                    onClick = onSwitchToAdmin,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldenYellow,
                        contentColor = NavyDeep
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("switch_to_admin_button")
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Admin Dashboard", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Safe Sign Out Button
        item {
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, CoralAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CoralAccent)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = CoralAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LocalMbsColors.current.neutralPill,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = NavyDeep)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

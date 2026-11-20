package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AdminQuestionDraft
import com.example.data.model.AssignmentMode
import com.example.data.model.ExamCategory
import com.example.data.model.ExamSubject
import com.example.data.model.ExamTest
import com.example.data.model.MathChapters
import com.example.data.model.QuestionBulkParser
import com.example.data.model.TestType
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.LocalMbsColors
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintTint
import com.example.ui.theme.NavyDeep

@Composable
fun AdminTestEditorDialog(
    editingTest: ExamTest?,
    initialQuestions: List<AdminQuestionDraft>,
    onDismiss: () -> Unit,
    onSave: (
        existingTestId: String?,
        title: String,
        description: String,
        category: String,
        testType: String,
        subject: String,
        chapter: String,
        durationMinutes: Int,
        totalMarks: Double,
        negativeMarking: Double,
        publishNow: Boolean,
        featuredToday: Boolean,
        assignmentMode: String,
        assignedBatch: String,
        assignedStudentIds: List<String>,
        questions: List<AdminQuestionDraft>
    ) -> Unit
) {
    val ext = LocalMbsColors.current
    var title by remember { mutableStateOf(editingTest?.title ?: "") }
    var description by remember { mutableStateOf(editingTest?.description ?: "") }
    var category by remember { mutableStateOf(editingTest?.category ?: ExamCategory.NAVODAYA.firestoreValue) }
    var testType by remember { mutableStateOf(editingTest?.testType ?: TestType.FULL.firestoreValue) }
    var subject by remember { mutableStateOf(editingTest?.subject ?: "All Subjects") }
    var chapter by remember { mutableStateOf(editingTest?.chapter ?: "") }
    var durationStr by remember { mutableStateOf((editingTest?.durationMinutes ?: 30L).toString()) }
    var negativeStr by remember { mutableStateOf((editingTest?.negativeMarking ?: 0.0).toString()) }
    var publishNow by remember { mutableStateOf(editingTest?.published ?: true) }
    var featuredToday by remember { mutableStateOf(editingTest?.featuredToday ?: false) }
    var assignmentMode by remember { mutableStateOf(editingTest?.assignmentMode ?: AssignmentMode.ALL.firestoreValue) }
    var assignedBatch by remember { mutableStateOf(editingTest?.assignedBatch ?: "") }
    var assignedStudentsRaw by remember {
        mutableStateOf(editingTest?.assignedStudentUids?.joinToString(", ") ?: "")
    }

    val questions = remember(initialQuestions) {
        mutableStateListOf<AdminQuestionDraft>().apply { addAll(initialQuestions) }
    }

    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var showBulkImportDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(color = NavyDeep) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Text(
                            text = if (editingTest == null) "Create & Publish Test" else "Edit Test",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val duration = durationStr.toIntOrNull() ?: 0
                                val neg = negativeStr.toDoubleOrNull() ?: 0.0
                                if (title.trim().length < 2) {
                                    validationError = "Enter a valid test name (min 2 chars)."
                                    return@Button
                                }
                                if (duration !in 1..300) {
                                    validationError = "Duration must be 1–300 minutes."
                                    return@Button
                                }
                                if (publishNow && questions.isEmpty()) {
                                    validationError = "Add at least 1 question before publishing."
                                    return@Button
                                }
                                validationError = null
                                val totalMarks = questions.sumOf { it.marks }.coerceAtLeast(1.0)
                                val studentIds = assignedStudentsRaw.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                onSave(
                                    editingTest?.id,
                                    title,
                                    description,
                                    category,
                                    testType,
                                    if (testType == "FULL") "All Subjects" else subject,
                                    chapter,
                                    duration,
                                    totalMarks,
                                    neg,
                                    publishNow,
                                    featuredToday,
                                    assignmentMode,
                                    assignedBatch,
                                    studentIds,
                                    questions.toList()
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldenYellow,
                                contentColor = NavyDeep
                            ),
                            modifier = Modifier.testTag("save_publish_test_button")
                        ) {
                            Text(if (publishNow) "Publish" else "Save Draft", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (validationError != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ext.coralTint,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = validationError!!,
                                    color = CoralAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Test Name (e.g. Navodaya Mock 5)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_test_title_input")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description / Instructions (optional)") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Category chips
                    item {
                        Text("Exam Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ExamCategory.entries) { cat ->
                                FilterChip(
                                    selected = category == cat.firestoreValue,
                                    onClick = { category = cat.firestoreValue },
                                    label = { Text(cat.firestoreValue) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NavyDeep,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Test Type chips
                    item {
                        Text("Test Format", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TestType.entries.forEach { tt ->
                                FilterChip(
                                    selected = testType == tt.firestoreValue,
                                    onClick = {
                                        testType = tt.firestoreValue
                                        subject = if (tt == TestType.FULL) "All Subjects" else ExamSubject.MATHEMATICS.firestoreValue
                                    },
                                    label = { Text(tt.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldenYellow,
                                        selectedLabelColor = NavyDeep
                                    )
                                )
                            }
                        }
                    }

                    // Subject & Math Chapter selector for Sectional Tests
                    if (testType == TestType.SECTIONAL.firestoreValue) {
                        item {
                            Text("Subject", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ExamSubject.entries) { subj ->
                                    FilterChip(
                                        selected = subject == subj.firestoreValue,
                                        onClick = { subject = subj.firestoreValue },
                                        label = { Text(subj.firestoreValue) }
                                    )
                                }
                            }
                        }
                        if (subject == ExamSubject.MATHEMATICS.firestoreValue) {
                            item {
                                Text("Mathematics Chapter", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(MathChapters.ALL) { chap ->
                                        FilterChip(
                                            selected = chapter == chap,
                                            onClick = { chapter = chap },
                                            label = { Text(chap) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Duration & Negative Marking
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = durationStr,
                                onValueChange = { durationStr = it.filter(Char::isDigit) },
                                label = { Text("Duration (min)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = negativeStr,
                                onValueChange = { negativeStr = it },
                                label = { Text("Negative Marking") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Assignment Targeting
                    item {
                        Text("Assign To", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssignmentMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = assignmentMode == mode.firestoreValue,
                                    onClick = { assignmentMode = mode.firestoreValue },
                                    label = { Text(mode.label) }
                                )
                            }
                        }
                        if (assignmentMode == AssignmentMode.BATCH.firestoreValue) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = assignedBatch,
                                onValueChange = { assignedBatch = it },
                                label = { Text("Batch Name (e.g. Navodaya-A)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (assignmentMode == AssignmentMode.STUDENTS.firestoreValue) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = assignedStudentsRaw,
                                onValueChange = { assignedStudentsRaw = it },
                                label = { Text("Student IDs or UIDs (comma separated)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Publish & Featured Switches
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, ext.cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Publish Immediately", fontWeight = FontWeight.Bold)
                                        Text(
                                            "Show live on eligible students' dashboards",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(checked = publishNow, onCheckedChange = { publishNow = it })
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Feature as Today's Mock Test", fontWeight = FontWeight.Bold)
                                        Text(
                                            "Highlights this test in the student home hero card",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(checked = featuredToday, onCheckedChange = { featuredToday = it })
                                }
                            }
                        }
                    }

                    // Questions Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Questions (${questions.size}) • Total Marks: ${questions.sumOf { it.marks }.toInt()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NavyDeep
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showAddQuestionDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_single_question_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Question")
                            }
                            OutlinedButton(
                                onClick = { showBulkImportDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bulk_import_questions_button")
                            ) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Bulk Import")
                            }
                        }
                    }

                    itemsIndexed(questions) { idx, q ->
                        val letters = listOf("A", "B", "C", "D")
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, ext.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Q${idx + 1} • ${q.subject} (+${q.marks.toInt()})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NavyDeep,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(q.questionText, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Surface(shape = RoundedCornerShape(8.dp), color = MintTint) {
                                        Text(
                                            text = "Correct: ${letters.getOrElse(q.correctOptionIndex) { "A" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MintDark,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { questions.removeAt(idx) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove question", tint = CoralAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddQuestionDialog) {
        AddSingleQuestionDialog(
            defaultSubject = if (testType == "FULL") ExamSubject.MATHEMATICS.firestoreValue else subject,
            defaultChapter = chapter,
            onAdd = { q ->
                questions.add(q)
                showAddQuestionDialog = false
            },
            onDismiss = { showAddQuestionDialog = false }
        )
    }

    if (showBulkImportDialog) {
        BulkImportQuestionsDialog(
            defaultSubject = if (testType == "FULL") ExamSubject.MATHEMATICS.firestoreValue else subject,
            defaultChapter = chapter,
            onImport = { imported ->
                questions.addAll(imported)
                showBulkImportDialog = false
            },
            onDismiss = { showBulkImportDialog = false }
        )
    }
}

@Composable
private fun AddSingleQuestionDialog(
    defaultSubject: String,
    defaultChapter: String,
    onAdd: (AdminQuestionDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var qSubject by remember { mutableStateOf(defaultSubject) }
    var qText by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctIdx by remember { mutableIntStateOf(0) }
    var marksStr by remember { mutableStateOf("1") }
    var explanation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Question") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExamSubject.entries) { s ->
                            FilterChip(
                                selected = qSubject == s.firestoreValue,
                                onClick = { qSubject = s.firestoreValue },
                                label = { Text(s.firestoreValue) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = qText,
                        onValueChange = { qText = it },
                        label = { Text("Question Text (Hindi/English)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    Text("Correct Option", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("A", "B", "C", "D").forEachIndexed { idx, lbl ->
                            FilterChip(
                                selected = correctIdx == idx,
                                onClick = { correctIdx = idx },
                                label = { Text(lbl) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = marksStr,
                        onValueChange = { marksStr = it },
                        label = { Text("Marks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        label = { Text("Detailed Solution / Explanation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (qText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank() && optC.isNotBlank() && optD.isNotBlank()) {
                        onAdd(
                            AdminQuestionDraft(
                                subject = qSubject,
                                chapter = defaultChapter,
                                questionText = qText.trim(),
                                optionA = optA.trim(),
                                optionB = optB.trim(),
                                optionC = optC.trim(),
                                optionD = optD.trim(),
                                correctOptionIndex = correctIdx,
                                marks = marksStr.toDoubleOrNull()?.coerceIn(0.25, 20.0) ?: 1.0,
                                explanation = explanation.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
            ) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BulkImportQuestionsDialog(
    defaultSubject: String,
    defaultChapter: String,
    onImport: (List<AdminQuestionDraft>) -> Unit,
    onDismiss: () -> Unit
) {
    var rawText by remember {
        mutableStateOf(
            "Mathematics | What is 15% of 200? | 20 | 25 | 30 | 35 | C | 200 * 15 / 100 = 30\n" +
                "Reasoning | Next in series: 3, 6, 12, 24, ? | 36 | 42 | 48 | 54 | C | Doubles every step"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Import Questions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "One question per line using pipe (|) format:\nSubject | Question | A | B | C | D | Correct(A-D) | Explanation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = QuestionBulkParser.parseLines(rawText, defaultSubject, defaultChapter)
                    if (parsed.isNotEmpty()) onImport(parsed)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
            ) {
                Text("Import Questions")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

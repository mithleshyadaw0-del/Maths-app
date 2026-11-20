package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamScoringEngine
import com.example.data.model.QuestionAnswerKey
import com.example.data.model.TestResult
import com.example.ui.components.CircularScoreRing
import com.example.ui.components.MbsErrorBanner
import com.example.ui.components.formatCountdown
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CoralTint
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoTint
import com.example.ui.theme.LocalMbsColors
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintTint
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.ReviewPurple
import com.example.ui.theme.ReviewPurpleTint
import com.example.ui.viewmodel.ActiveExamUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamScreen(
    state: ActiveExamUiState,
    onSelectOption: (String, Int) -> Unit,
    onClearResponse: (String) -> Unit,
    onToggleReview: (String) -> Unit,
    onJumpToQuestion: (Int) -> Unit,
    onSubmitConfirm: () -> Unit,
    onPauseAndExit: () -> Unit
) {
    val ext = LocalMbsColors.current
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showFullPalette by remember { mutableStateOf(false) }

    BackHandler { showExitDialog = true }

    if (state.loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = NavyDeep)
                Spacer(Modifier.height(14.dp))
                Text("Loading examination paper...", fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    val test = state.test
    val question = state.questions.getOrNull(state.currentIndex)
    if (test == null || question == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MbsErrorBanner(state.errorMessage ?: "Unable to load exam questions.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onPauseAndExit) { Text("Go Back") }
            }
        }
        return
    }

    val selectedOption = state.answers[question.id]
    val isMarked = question.id in state.markedForReview
    val optionLabels = listOf("A", "B", "C", "D")
    val isLowTime = state.remainingSeconds <= 120L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("exam_screen")
    ) {
        // 1. Top Bar: Close X, Test Title, Countdown Timer Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                onClick = { showExitDialog = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("exam_exit_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, contentDescription = "Exit exam", tint = NavyDeep)
                }
            }

            Text(
                text = test.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyDeep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = if (isLowTime) CoralAccent else GoldenYellow,
                modifier = Modifier.testTag("exam_timer_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Remaining time",
                        tint = if (isLowTime) Color.White else NavyDeep,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = formatCountdown(state.remainingSeconds),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLowTime) Color.White else NavyDeep
                    )
                }
            }
        }

        // 2. Question Progress Row + Segmented Bar (Matches Reference Image 6)
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "सवाल ${state.currentIndex + 1} / ${state.questions.size}",
                    fontFamily = DevanagariFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDeep
                )
                Text(
                    text = question.subject,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (state.currentIndex + 1).toFloat() / state.questions.size.coerceAtLeast(1) },
                color = NavyDeep,
                trackColor = ext.cardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )
        }

        if (state.errorMessage != null) {
            Box(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                MbsErrorBanner(state.errorMessage)
            }
        }

        // 3. Scrollable Question Card + 4 Answer Options
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = question.chapter.ifBlank { question.subject },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+${question.marks.toInt()} अंक",
                            fontFamily = DevanagariFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MintDark
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = question.questionText,
                        fontFamily = DevanagariFontFamily,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDeep
                    )
                }
            }

            // Four Option Cards (Matches Reference Image 6 indigo selected styling)
            question.options.take(4).forEachIndexed { optIdx, optText ->
                val isSelected = selectedOption == optIdx
                Surface(
                    onClick = { onSelectOption(question.id, optIdx) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) IndigoTint else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) IndigoAccent else ext.cardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exam_option_$optIdx")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) IndigoAccent else ext.neutralPill),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = optionLabels.getOrElse(optIdx) { "${optIdx + 1}" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else NavyDeep
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = optText,
                            fontFamily = DevanagariFontFamily,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = NavyDeep,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Mark for Review + Clear Response + Full Palette Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { onToggleReview(question.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMarked) ReviewPurpleTint else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isMarked) ReviewPurple else ext.cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_review_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isMarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isMarked) ReviewPurple else NavyDeep,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isMarked) "Marked" else "Review",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isMarked) ReviewPurple else NavyDeep,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    onClick = { onClearResponse(question.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clear_response_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = null,
                            tint = CoralAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            color = CoralAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    onClick = { showFullPalette = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_palette_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = null,
                            tint = NavyDeep,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Palette",
                            style = MaterialTheme.typography.labelMedium,
                            color = NavyDeep,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 4. Bottom Dock: Horizontal Question Palette + Previous / Next / Submit
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(state.questions) { idx, q ->
                        val isCurrent = idx == state.currentIndex
                        val isAnswered = q.id in state.answers
                        val isReview = q.id in state.markedForReview
                        val bg = when {
                            isCurrent -> GoldenYellow
                            isReview -> ReviewPurple
                            isAnswered -> NavyDeep
                            else -> ext.neutralPill
                        }
                        val fg = when {
                            isCurrent -> NavyDeep
                            isReview || isAnswered -> Color.White
                            else -> NavyDeep
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .clickable { onJumpToQuestion(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = fg
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onJumpToQuestion(state.currentIndex - 1) },
                        enabled = state.currentIndex > 0,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, NavyDeep),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("exam_previous_button")
                    ) {
                        Text(
                            text = "पिछला",
                            fontFamily = DevanagariFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = NavyDeep
                        )
                    }

                    if (state.currentIndex < state.questions.lastIndex) {
                        Button(
                            onClick = { onJumpToQuestion(state.currentIndex + 1) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NavyDeep,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("exam_next_button")
                        ) {
                            Text(
                                text = "अगला",
                                fontFamily = DevanagariFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { showSubmitDialog = true },
                        enabled = !state.submitting,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.currentIndex == state.questions.lastIndex) NavyDeep else GoldenYellow,
                            contentColor = if (state.currentIndex == state.questions.lastIndex) Color.White else NavyDeep
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("exam_submit_button")
                    ) {
                        if (state.submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Submit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Full Question Palette Dialog
    if (showFullPalette) {
        AlertDialog(
            onDismissRequest = { showFullPalette = false },
            title = { Text("Question Palette (${state.answers.size}/${state.questions.size} answered)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PaletteLegendDot(NavyDeep, "Answered")
                        PaletteLegendDot(GoldenYellow, "Current")
                        PaletteLegendDot(ReviewPurple, "Review")
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.questions.forEachIndexed { idx, q ->
                            val bg = when {
                                idx == state.currentIndex -> GoldenYellow
                                q.id in state.markedForReview -> ReviewPurple
                                q.id in state.answers -> NavyDeep
                                else -> ext.neutralPill
                            }
                            val fg = if (bg == NavyDeep || bg == ReviewPurple) Color.White else NavyDeep
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .clickable {
                                        onJumpToQuestion(idx)
                                        showFullPalette = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${idx + 1}", color = fg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFullPalette = false }) { Text("Close") }
            }
        )
    }

    // Submit Test Confirmation Dialog
    if (showSubmitDialog) {
        val answered = state.answers.size
        val unanswered = (state.questions.size - answered).coerceAtLeast(0)
        val marked = state.markedForReview.size
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit Test? • टेस्ट सबमिट करें?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Total Questions: ${state.questions.size}")
                    Text("Answered: $answered", color = MintDark, fontWeight = FontWeight.SemiBold)
                    Text("Unanswered: $unanswered", color = CoralAccent, fontWeight = FontWeight.SemiBold)
                    Text("Marked for Review: $marked", color = ReviewPurple, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Once submitted, your answers will be locked and your final score & solutions will be generated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        onSubmitConfirm()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                    modifier = Modifier.testTag("confirm_submit_exam_button")
                ) {
                    Text("Submit Final")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Exit & Auto-save Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Pause & Save Exam?") },
            text = {
                Text("Your selected answers and remaining timer (${formatCountdown(state.remainingSeconds)}) are automatically saved. You can resume anytime.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onPauseAndExit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
                ) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Keep Writing") }
            }
        )
    }
}

@Composable
private fun PaletteLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

// --- RESULT & DETAILED SOLUTIONS SCREEN (Matches Reference Image 4 & 9) ---

@Composable
fun ResultScreen(
    result: TestResult,
    questions: List<ExamQuestion>,
    answerKeys: List<QuestionAnswerKey>,
    rankPosition: Int?,
    onBackToHome: () -> Unit
) {
    val ext = LocalMbsColors.current
    var showSolutions by remember { mutableStateOf(false) }

    BackHandler {
        if (showSolutions) showSolutions = false else onBackToHome()
    }

    val subjectBreakdown = remember(result.subjectBreakdown) {
        ExamScoringEngine.parseSubjectBreakdown(result.subjectBreakdown)
    }
    val keysByQId = remember(answerKeys) { answerKeys.associateBy { it.id } }
    val barColors = listOf(MintAccent, IndigoAccent, GoldenYellow, ext.pinkAccent, NavyDeep)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("result_screen")
    ) {
        item {
            Text(
                text = result.testTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Deep Navy Score Ring Hero Card
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDeep),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 26.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val marksText = if (result.obtainedMarks % 1.0 == 0.0 && result.totalMarks % 1.0 == 0.0) {
                        "${result.obtainedMarks.toInt()}/${result.totalMarks.toInt()}"
                    } else {
                        "${result.obtainedMarks}/${result.totalMarks.toInt()}"
                    }
                    CircularScoreRing(
                        obtainedText = marksText,
                        percentage = result.percentage.toFloat(),
                        size = 152.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    val firstName = result.studentName.split(" ").firstOrNull().orEmpty().ifBlank { "Student" }
                    Text(
                        text = if (result.percentage >= 60.0) "बहुत बढ़िया, $firstName!" else "अच्छा प्रयास, $firstName!",
                        fontFamily = DevanagariFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    val rankText = if (rankPosition != null) "रैंक #$rankPosition • " else ""
                    Text(
                        text = "${rankText}Accuracy ${result.accuracy.toInt()}% • समय ${formatCountdown(result.timeTakenSeconds)}",
                        fontFamily = DevanagariFontFamily,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }
        }

        // Three Pastel Metric Pills: Correct / Wrong / Unattempted
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResultPill("सही ${result.correctCount}", MintTint, MintDark, Modifier.weight(1f))
                ResultPill("गलत ${result.wrongCount}", CoralTint, CoralAccent, Modifier.weight(1f))
                ResultPill("छोड़े ${result.unattemptedCount}", ext.neutralPill, NavyDeep, Modifier.weight(1f))
            }
        }

        // Subject-Wise Performance Card ("विषय अनुसार")
        if (subjectBreakdown.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "विषय अनुसार • Subject-wise Performance",
                            fontFamily = DevanagariFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDeep
                        )
                        subjectBreakdown.forEachIndexed { idx, item ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.subject,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = NavyDeep
                                    )
                                    Text(
                                        text = "${item.correct}/${item.total}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDeep
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { item.ratio.coerceIn(0.03f, 1f) },
                                    color = barColors[idx % barColors.size],
                                    trackColor = ext.neutralPill,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons ("उत्तर देखें" & "होम पर जाएं")
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showSolutions = !showSolutions },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyDeep,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("toggle_solutions_button")
                ) {
                    Text(
                        text = if (showSolutions) "उत्तर छिपाएँ" else "उत्तर देखें",
                        fontFamily = DevanagariFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onBackToHome,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, NavyDeep),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("result_back_home_button")
                ) {
                    Text(
                        text = "होम पर जाएं",
                        fontFamily = DevanagariFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = NavyDeep
                    )
                }
            }
        }

        // Detailed Solutions Viewer (Unlocked post-submission)
        if (showSolutions && questions.isNotEmpty()) {
            item {
                Text(
                    text = "Detailed Solutions • विस्तृत समाधान",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyDeep
                )
            }
            itemsIndexed(questions) { idx, q ->
                val key = keysByQId[q.id]
                val chosenIdx = result.answers[q.id]?.toInt()
                val correctIdx = key?.correctOptionIndex?.toInt() ?: -1
                val isCorrect = chosenIdx != null && chosenIdx == correctIdx
                val letters = listOf("A", "B", "C", "D")

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (isCorrect) MintAccent.copy(alpha = 0.4f) else ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Q${idx + 1} • ${q.subject}",
                                style = MaterialTheme.typography.labelLarge,
                                color = NavyDeep,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    chosenIdx == null -> "Unattempted"
                                    isCorrect -> "Correct ✓"
                                    else -> "Wrong ✗"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    chosenIdx == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                    isCorrect -> MintDark
                                    else -> CoralAccent
                                }
                            )
                        }
                        Text(
                            text = q.questionText,
                            fontFamily = DevanagariFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NavyDeep
                        )
                        if (chosenIdx != null) {
                            Text(
                                text = "Your Answer: ${letters.getOrElse(chosenIdx) { "" }}) ${q.options.getOrElse(chosenIdx) { "" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCorrect) MintDark else CoralAccent
                            )
                        }
                        if (correctIdx in 0..3) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MintAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Correct Answer: ${letters[correctIdx]}) ${q.options.getOrElse(correctIdx) { "" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MintDark
                                )
                            }
                        }
                        if (!key?.explanation.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ext.amberTint,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "व्याख्या: ${key?.explanation}",
                                    fontFamily = DevanagariFontFamily,
                                    fontSize = 13.sp,
                                    color = NavyDeep,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultPill(
    label: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontFamily = DevanagariFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}

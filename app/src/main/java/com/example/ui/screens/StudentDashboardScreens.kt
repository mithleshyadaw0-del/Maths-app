package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Announcement
import com.example.data.model.ExamCategory
import com.example.data.model.ExamSubject
import com.example.data.model.ExamTest
import com.example.data.model.MathChapters
import com.example.data.model.TestAttempt
import com.example.data.model.TestResult
import com.example.data.model.TestType
import com.example.data.model.UserProfile
import com.example.ui.components.CircleBackButton
import com.example.ui.components.MbsEmptyStateCard
import com.example.ui.components.categoryStyle
import com.example.ui.components.subjectBadgeColor
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.LocalMbsColors
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintTint
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyTealGlow

@Composable
fun StudentHomeScreen(
    profile: UserProfile,
    eligibleTests: List<ExamTest>,
    results: List<TestResult>,
    attempts: List<TestAttempt>,
    announcements: List<Announcement>,
    welcomeTagline: String,
    onCategoryClick: (ExamCategory) -> Unit,
    onStartTest: (ExamTest) -> Unit,
    onViewResult: (TestResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMbsColors.current
    val resultsByTestId = remember(results) { results.associateBy { it.testId } }
    val attemptsByTestId = remember(attempts) { attempts.associateBy { it.testId } }

    val recommendedTest = remember(eligibleTests, resultsByTestId) {
        eligibleTests.firstOrNull { it.featuredToday && it.id !in resultsByTestId }
            ?: eligibleTests.firstOrNull { it.id !in resultsByTestId }
            ?: eligibleTests.firstOrNull { it.featuredToday }
            ?: eligibleTests.firstOrNull()
    }

    val firstName = profile.name.trim().split(" ").firstOrNull().orEmpty().ifBlank { "Student" }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("student_home_screen")
    ) {
        // 1. Greeting + Streak Pill (Matches Reference Image 1)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "नमस्ते, $firstName",
                        fontFamily = DevanagariFontFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "आज कितना\nपढ़ोगे?",
                        fontFamily = DevanagariFontFamily,
                        fontSize = 27.sp,
                        lineHeight = 33.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDeep
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = GoldenYellow,
                    shadowElevation = 2.dp,
                    modifier = Modifier.testTag("streak_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Activity Streak",
                            tint = NavyDeep,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${profile.streakDays} दिन की streak",
                            fontFamily = DevanagariFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDeep
                        )
                    }
                }
            }
        }

        // 2. Today's Recommended Test Hero Card (Deep Navy with subtle teal corner arc)
        item {
            if (recommendedTest != null) {
                val existingRes = resultsByTestId[recommendedTest.id]
                val inProgress = attemptsByTestId[recommendedTest.id]?.status == "in_progress"
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyDeep),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_recommended_test_card")
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(
                                color = NavyTealGlow.copy(alpha = 0.7f),
                                radius = size.height * 0.62f,
                                center = Offset(size.width * 0.90f, size.height * 0.18f)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "आज का मॉक टेस्ट",
                                fontFamily = DevanagariFontFamily,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.78f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = recommendedTest.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${recommendedTest.questionCount} सवाल  •  ${recommendedTest.durationMinutes} मिनट  •  ${recommendedTest.category}",
                                fontFamily = DevanagariFontFamily,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    if (existingRes != null) onViewResult(existingRes)
                                    else onStartTest(recommendedTest)
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldenYellow,
                                    contentColor = NavyDeep
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("start_recommended_test_button")
                            ) {
                                Text(
                                    text = when {
                                        existingRes != null -> "परिणाम देखें (${existingRes.percentage.toInt()}%)"
                                        inProgress -> "टेस्ट जारी रखें →"
                                        else -> "टेस्ट शुरू करें →"
                                    },
                                    fontFamily = DevanagariFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                MbsEmptyStateCard(
                    title = "No Published Tests Yet",
                    subtitle = "When Mithlesh Sir publishes mock tests for your batch (${profile.batch}), they will appear right here immediately."
                )
            }
        }

        // 3. Compact Performance Summary Strip (Total Attempted, Average Marks %, Batch)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudentQuickStatCard(
                    label = "Tests Attempted",
                    value = "${profile.totalTestsAttempted}",
                    accent = ext.indigoAccent,
                    bg = ext.indigoTint,
                    modifier = Modifier.weight(1f)
                )
                StudentQuickStatCard(
                    label = "Average Score",
                    value = "${profile.averagePercentage.toInt()}%",
                    accent = MintDark,
                    bg = MintTint,
                    modifier = Modifier.weight(1f)
                )
                StudentQuickStatCard(
                    label = "Total Marks",
                    value = "${profile.totalMarksObtained.toInt()}",
                    accent = NavyDeep,
                    bg = ext.amberTint,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Exam Categories 2x2 Grid ("परीक्षा चुनें")
        item {
            Text(
                text = "परीक्षा चुनें",
                fontFamily = DevanagariFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDeep
            )
            Spacer(Modifier.height(10.dp))

            val categories = ExamCategory.entries
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categories.chunked(2).forEach { rowCats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCats.forEach { cat ->
                            val catTests = eligibleTests.filter {
                                it.category.equals(cat.firestoreValue, ignoreCase = true)
                            }
                            val completed = catTests.count { it.id in resultsByTestId }
                            val progress = if (catTests.isNotEmpty()) {
                                completed.toFloat() / catTests.size.toFloat()
                            } else 0f

                            ExamCategoryTile(
                                category = cat,
                                testCount = catTests.size,
                                progress = progress,
                                onClick = { onCategoryClick(cat) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 5. Latest Announcements
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = NavyDeep,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Latest Announcements • सूचनाएँ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyDeep
                )
            }
        }

        if (announcements.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = welcomeTagline.ifBlank { "सीखें गणित, बढ़ें सफलता की ओर — कोई नई सूचना नहीं है।" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(announcements.take(4), key = { it.id }) { ann ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(GoldenYellow)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ann.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDeep,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = ext.indigoTint
                                ) {
                                    Text(
                                        text = ann.targetCategory,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.indigoAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = ann.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun StudentQuickStatCard(
    label: String,
    value: String,
    accent: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = NavyDeep.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExamCategoryTile(
    category: ExamCategory,
    testCount: Int,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = categoryStyle(category)
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = style.cardBg),
        modifier = modifier
            .testTag("category_card_${category.name.lowercase()}")
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(style.badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = style.shortCode,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = style.badgeTextColor
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = category.firestoreValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyDeep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$testCount टेस्ट",
                fontFamily = DevanagariFontFamily,
                fontSize = 13.sp,
                color = NavyDeep.copy(alpha = 0.68f)
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0.06f, 1f) },
                color = NavyDeep,
                trackColor = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

// --- TESTS DRILL-DOWN NAVIGATION (Category -> Full/Sectional -> Subjects -> Test List) ---

sealed interface TestsBrowseStep {
    data object AllCategories : TestsBrowseStep
    data class CategoryDetail(val category: ExamCategory) : TestsBrowseStep
    data class FullTestList(val category: ExamCategory) : TestsBrowseStep
    data class SectionalSubjects(val category: ExamCategory) : TestsBrowseStep
    data class SectionalTestList(val category: ExamCategory, val subject: ExamSubject) : TestsBrowseStep
}

@Composable
fun StudentTestsFlowScreen(
    initialCategory: ExamCategory?,
    eligibleTests: List<ExamTest>,
    results: List<TestResult>,
    attempts: List<TestAttempt>,
    onStartTest: (ExamTest) -> Unit,
    onViewResult: (TestResult) -> Unit,
    onClearInitialCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember(initialCategory) {
        mutableStateOf<TestsBrowseStep>(
            if (initialCategory != null) TestsBrowseStep.CategoryDetail(initialCategory)
            else TestsBrowseStep.AllCategories
        )
    }

    val resultsByTestId = remember(results) { results.associateBy { it.testId } }
    val attemptsByTestId = remember(attempts) { attempts.associateBy { it.testId } }

    // Support system back button inside drill-down steps
    BackHandler(enabled = step !is TestsBrowseStep.AllCategories) {
        step = when (val s = step) {
            is TestsBrowseStep.CategoryDetail -> {
                onClearInitialCategory()
                TestsBrowseStep.AllCategories
            }
            is TestsBrowseStep.FullTestList -> TestsBrowseStep.CategoryDetail(s.category)
            is TestsBrowseStep.SectionalSubjects -> TestsBrowseStep.CategoryDetail(s.category)
            is TestsBrowseStep.SectionalTestList -> TestsBrowseStep.SectionalSubjects(s.category)
            TestsBrowseStep.AllCategories -> TestsBrowseStep.AllCategories
        }
    }

    when (val current = step) {
        is TestsBrowseStep.AllCategories -> {
            AllTestsHubView(
                eligibleTests = eligibleTests,
                resultsByTestId = resultsByTestId,
                attemptsByTestId = attemptsByTestId,
                onSelectCategory = { step = TestsBrowseStep.CategoryDetail(it) },
                onStartTest = onStartTest,
                onViewResult = onViewResult,
                modifier = modifier
            )
        }
        is TestsBrowseStep.CategoryDetail -> {
            CategoryDetailView(
                category = current.category,
                categoryTests = eligibleTests.filter {
                    it.category.equals(current.category.firestoreValue, ignoreCase = true)
                },
                onBack = {
                    onClearInitialCategory()
                    step = TestsBrowseStep.AllCategories
                },
                onFullTestsClick = { step = TestsBrowseStep.FullTestList(current.category) },
                onSectionalTestsClick = { step = TestsBrowseStep.SectionalSubjects(current.category) },
                modifier = modifier
            )
        }
        is TestsBrowseStep.FullTestList -> {
            val fullTests = eligibleTests.filter {
                it.category.equals(current.category.firestoreValue, ignoreCase = true) &&
                    it.testType.equals(TestType.FULL.firestoreValue, ignoreCase = true)
            }
            TestListView(
                headerTitle = "Full Test",
                headerSubtitle = "${current.category.firestoreValue} • सभी विषय एक साथ",
                tests = fullTests,
                showMathChapterFilter = false,
                resultsByTestId = resultsByTestId,
                attemptsByTestId = attemptsByTestId,
                onBack = { step = TestsBrowseStep.CategoryDetail(current.category) },
                onStartTest = onStartTest,
                onViewResult = onViewResult,
                modifier = modifier
            )
        }
        is TestsBrowseStep.SectionalSubjects -> {
            val sectionalTests = eligibleTests.filter {
                it.category.equals(current.category.firestoreValue, ignoreCase = true) &&
                    it.testType.equals(TestType.SECTIONAL.firestoreValue, ignoreCase = true)
            }
            SectionalSubjectsView(
                category = current.category,
                sectionalTests = sectionalTests,
                resultsByTestId = resultsByTestId,
                onBack = { step = TestsBrowseStep.CategoryDetail(current.category) },
                onSelectSubject = { subj ->
                    step = TestsBrowseStep.SectionalTestList(current.category, subj)
                },
                modifier = modifier
            )
        }
        is TestsBrowseStep.SectionalTestList -> {
            val subjTests = eligibleTests.filter {
                it.category.equals(current.category.firestoreValue, ignoreCase = true) &&
                    it.testType.equals(TestType.SECTIONAL.firestoreValue, ignoreCase = true) &&
                    it.subject.equals(current.subject.firestoreValue, ignoreCase = true)
            }
            TestListView(
                headerTitle = current.subject.displayName,
                headerSubtitle = "Sectional • सिर्फ़ ${current.subject.firestoreValue} के टेस्ट",
                tests = subjTests,
                showMathChapterFilter = current.subject == ExamSubject.MATHEMATICS,
                resultsByTestId = resultsByTestId,
                attemptsByTestId = attemptsByTestId,
                onBack = { step = TestsBrowseStep.SectionalSubjects(current.category) },
                onStartTest = onStartTest,
                onViewResult = onViewResult,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AllTestsHubView(
    eligibleTests: List<ExamTest>,
    resultsByTestId: Map<String, TestResult>,
    attemptsByTestId: Map<String, TestAttempt>,
    onSelectCategory: (ExamCategory) -> Unit,
    onStartTest: (ExamTest) -> Unit,
    onViewResult: (TestResult) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "मॉक टेस्ट • Mock Tests",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = NavyDeep
            )
            Text(
                text = "Select an exam category for Full & Sectional Tests",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExamCategory.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { cat ->
                            val catTests = eligibleTests.filter {
                                it.category.equals(cat.firestoreValue, ignoreCase = true)
                            }
                            val done = catTests.count { it.id in resultsByTestId }
                            val prog = if (catTests.isNotEmpty()) done.toFloat() / catTests.size else 0f
                            ExamCategoryTile(
                                category = cat,
                                testCount = catTests.size,
                                progress = prog,
                                onClick = { onSelectCategory(cat) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "सभी उपलब्ध टेस्ट (${eligibleTests.size})",
                fontFamily = DevanagariFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDeep
            )
        }

        if (eligibleTests.isEmpty()) {
            item {
                MbsEmptyStateCard(
                    title = "No Published Tests Available",
                    subtitle = "Published tests assigned to your batch will appear here automatically."
                )
            }
        } else {
            items(eligibleTests, key = { it.id }) { test ->
                TestRowCard(
                    test = test,
                    result = resultsByTestId[test.id],
                    inProgress = attemptsByTestId[test.id]?.status == "in_progress",
                    onStart = { onStartTest(test) },
                    onViewResult = { resultsByTestId[test.id]?.let(onViewResult) }
                )
            }
        }
    }
}

@Composable
private fun CategoryDetailView(
    category: ExamCategory,
    categoryTests: List<ExamTest>,
    onBack: () -> Unit,
    onFullTestsClick: () -> Unit,
    onSectionalTestsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fullTests = categoryTests.filter { it.testType.equals("FULL", ignoreCase = true) }
    val sectionalTests = categoryTests.filter { it.testType.equals("SECTIONAL", ignoreCase = true) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("category_detail_screen")
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleBackButton(onClick = onBack)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = category.displayTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDeep
                    )
                    Text(
                        text = "${category.subtitleHi} • क्या देना है?",
                        fontFamily = DevanagariFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Full Test Card (Deep Navy — matches Reference Image 2)
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDeep),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .testTag("full_test_card")
                    .clickable(onClick = onFullTestsClick)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Full Test",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "सभी विषय एक साथ, असली परीक्षा जैसा",
                            fontFamily = DevanagariFontFamily,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val avgQ = fullTests.firstOrNull()?.questionCount ?: 30L
                            val avgMin = fullTests.firstOrNull()?.durationMinutes ?: 60L
                            DarkPill("$avgQ सवाल")
                            DarkPill("$avgMin मिनट")
                            DarkPill("${fullTests.size} टेस्ट")
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GoldenYellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open Full Tests",
                                tint = NavyDeep,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Sectional Test Card (Golden Yellow — matches Reference Image 2)
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = GoldenYellow),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .testTag("sectional_test_card")
                    .clickable(onClick = onSectionalTestsClick)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Sectional Test",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDeep
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "एक-एक विषय की अलग तैयारी",
                            fontFamily = DevanagariFontFamily,
                            fontSize = 14.sp,
                            color = NavyDeep.copy(alpha = 0.85f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldPill("5 विषय")
                            GoldPill("${sectionalTests.size} टेस्ट")
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NavyDeep.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open Sectional Tests",
                                tint = NavyDeep,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkPill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.16f)) {
        Text(
            text = text,
            fontFamily = DevanagariFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun GoldPill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.35f)) {
        Text(
            text = text,
            fontFamily = DevanagariFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = NavyDeep,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SectionalSubjectsView(
    category: ExamCategory,
    sectionalTests: List<ExamTest>,
    resultsByTestId: Map<String, TestResult>,
    onBack: () -> Unit,
    onSelectSubject: (ExamSubject) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMbsColors.current
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("sectional_subjects_screen")
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleBackButton(onClick = onBack)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Sectional Test",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDeep
                    )
                    Text(
                        text = "${category.firestoreValue} • विषय चुनिए",
                        fontFamily = DevanagariFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(ExamSubject.entries) { subj ->
            val testsForSubj = sectionalTests.filter {
                it.subject.equals(subj.firestoreValue, ignoreCase = true)
            }
            val completed = testsForSubj.count { it.id in resultsByTestId }
            val progress = if (testsForSubj.isNotEmpty()) completed.toFloat() / testsForSubj.size else 0f
            val badgeColor = subjectBadgeColor(subj)

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_card_${subj.name.lowercase()}")
                    .clickable { onSelectSubject(subj) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(badgeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = subj.shortBadge,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (subj == ExamSubject.EVS) NavyDeep else Color.White
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subj.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NavyDeep
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${testsForSubj.size} टेस्ट • $completed पूरे",
                            fontFamily = DevanagariFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0.05f, 1f) },
                            color = NavyDeep,
                            trackColor = ext.neutralPill,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open ${subj.displayName}",
                        tint = NavyDeep
                    )
                }
            }
        }
    }
}

@Composable
private fun TestListView(
    headerTitle: String,
    headerSubtitle: String,
    tests: List<ExamTest>,
    showMathChapterFilter: Boolean,
    resultsByTestId: Map<String, TestResult>,
    attemptsByTestId: Map<String, TestAttempt>,
    onBack: () -> Unit,
    onStartTest: (ExamTest) -> Unit,
    onViewResult: (TestResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChapter by remember { mutableStateOf("All") }
    val filteredTests = remember(tests, selectedChapter) {
        if (!showMathChapterFilter || selectedChapter == "All") tests
        else tests.filter { it.chapter.equals(selectedChapter, ignoreCase = true) }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("test_list_screen")
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleBackButton(onClick = onBack)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDeep
                    )
                    Text(
                        text = headerSubtitle,
                        fontFamily = DevanagariFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showMathChapterFilter) {
            item {
                val chips = listOf("All") + MathChapters.ALL
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chips) { chap ->
                        FilterChip(
                            selected = selectedChapter == chap,
                            onClick = { selectedChapter = chap },
                            label = { Text(chap, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyDeep,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        if (filteredTests.isEmpty()) {
            item {
                MbsEmptyStateCard(
                    title = "No Published Tests Here Yet",
                    subtitle = "Tests published by Mithlesh Sir for this section will appear automatically."
                )
            }
        } else {
            items(filteredTests, key = { it.id }) { test ->
                TestRowCard(
                    test = test,
                    result = resultsByTestId[test.id],
                    inProgress = attemptsByTestId[test.id]?.status == "in_progress",
                    onStart = { onStartTest(test) },
                    onViewResult = { resultsByTestId[test.id]?.let(onViewResult) }
                )
            }
        }
    }
}

@Composable
fun TestRowCard(
    test: ExamTest,
    result: TestResult?,
    inProgress: Boolean,
    onStart: () -> Unit,
    onViewResult: () -> Unit
) {
    val ext = LocalMbsColors.current
    val stripeColor = when {
        result != null -> MintAccent
        test.published -> GoldenYellow
        else -> ext.textMuted
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("test_card_${test.id}")
            .clickable {
                if (result != null) onViewResult()
                else if (test.published) onStart()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(stripeColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = test.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyDeep,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                val chapterPart = if (test.chapter.isNotBlank()) " • ${test.chapter}" else ""
                Text(
                    text = "${test.questionCount} सवाल • ${test.durationMinutes} मिनट$chapterPart",
                    fontFamily = DevanagariFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (test.negativeMarking > 0) {
                    Text(
                        text = "Negative marking: -${test.negativeMarking}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.coralAccent
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            when {
                result != null -> {
                    Surface(
                        onClick = onViewResult,
                        shape = RoundedCornerShape(14.dp),
                        color = MintTint
                    ) {
                        Text(
                            text = "${result.obtainedMarks.toInt()}/${result.totalMarks.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MintDark,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
                test.published -> {
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldenYellow,
                            contentColor = NavyDeep
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (inProgress) "जारी रखें" else "शुरू करें",
                            fontFamily = DevanagariFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked",
                            tint = ext.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("बंद", color = ext.textMuted, fontFamily = DevanagariFontFamily)
                    }
                }
            }
        }
    }
}

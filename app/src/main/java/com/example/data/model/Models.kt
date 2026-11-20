package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ExamCategory(
    val firestoreValue: String,
    val displayTitle: String,
    val shortBadge: String,
    val subtitleHi: String
) {
    NAVODAYA("Navodaya", "Navodaya (JNVST)", "JNV", "कक्षा 6 प्रवेश परीक्षा"),
    SAINIK_SCHOOL("Sainik School", "Sainik School", "SS", "AISSEE प्रवेश परीक्षा"),
    SIMULTALA("Simultala", "Simultala", "SV", "सिमुलतला आवासीय विद्यालय"),
    OLYMPIAD("Olympiad", "Olympiad", "OLY", "राष्ट्रीय गणित ओलंपियाड");

    companion object {
        fun fromValue(raw: String): ExamCategory =
            entries.firstOrNull { it.firestoreValue.equals(raw, ignoreCase = true) } ?: NAVODAYA
    }
}

enum class TestType(val firestoreValue: String, val label: String, val subtitleHi: String) {
    FULL("FULL", "Full Test", "सभी विषय एक साथ, असली परीक्षा जैसा"),
    SECTIONAL("SECTIONAL", "Sectional Test", "एक-एक विषय की अलग तैयारी");

    companion object {
        fun fromValue(raw: String): TestType =
            entries.firstOrNull { it.firestoreValue.equals(raw, ignoreCase = true) } ?: FULL
    }
}

enum class ExamSubject(
    val firestoreValue: String,
    val displayName: String,
    val shortBadge: String
) {
    MATHEMATICS("Mathematics", "Mathematics", "M"),
    REASONING("Reasoning", "Reasoning / Mental Ability", "R"),
    EVS("EVS", "EVS", "EV"),
    ENGLISH("English", "English", "En"),
    HINDI("Hindi", "Hindi", "हि");

    companion object {
        fun fromValue(raw: String): ExamSubject =
            entries.firstOrNull {
                it.firestoreValue.equals(raw, ignoreCase = true) ||
                    it.displayName.equals(raw, ignoreCase = true)
            } ?: MATHEMATICS
    }
}

object MathChapters {
    val ALL = listOf(
        "Number System",
        "Fractions",
        "Percentage",
        "Geometry",
        "Time and Work",
        "Mensuration",
        "Average",
        "Ratio and Proportion",
        "Profit and Loss"
    )
}

enum class NoteMaterialType(val firestoreValue: String, val label: String) {
    NOTES("NOTES", "Subject Notes"),
    FORMULA_SHEET("FORMULA_SHEET", "Formula Sheet"),
    WORKSHEET("WORKSHEET", "Worksheet"),
    PREVIOUS_YEAR("PREVIOUS_YEAR", "Previous Year Paper");

    companion object {
        fun fromValue(raw: String): NoteMaterialType =
            entries.firstOrNull { it.firestoreValue.equals(raw, ignoreCase = true) } ?: NOTES
    }
}

enum class AssignmentMode(val firestoreValue: String, val label: String) {
    ALL("ALL", "All Students"),
    BATCH("BATCH", "Specific Batch"),
    STUDENTS("STUDENTS", "Assigned Students");

    companion object {
        fun fromValue(raw: String): AssignmentMode =
            entries.firstOrNull { it.firestoreValue.equals(raw, ignoreCase = true) } ?: ALL
    }
}

enum class RankingMode(val firestoreValue: String, val label: String) {
    PERCENTAGE("PERCENTAGE", "Average Percentage"),
    TOTAL_MARKS("TOTAL_MARKS", "Total Marks");

    companion object {
        fun fromValue(raw: String): RankingMode =
            entries.firstOrNull { it.firestoreValue.equals(raw, ignoreCase = true) } ?: PERCENTAGE
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val studentId: String = "",
    val batch: String = "General",
    val role: String = "student",
    val active: Boolean = true,
    val streakDays: Long = 0L,
    val totalTestsAttempted: Long = 0L,
    val totalMarksObtained: Double = 0.0,
    val averagePercentage: Double = 0.0,
    val lastTestDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentRosterEntry(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val name: String = "",
    val batch: String = "Navodaya-A",
    val targetCategory: String = "Navodaya",
    val active: Boolean = true,
    val linkedUid: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExamTest(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Navodaya",
    val testType: String = "FULL",
    val subject: String = "All Subjects",
    val chapter: String = "",
    val durationMinutes: Long = 45L,
    val totalMarks: Double = 30.0,
    val negativeMarking: Double = 0.0,
    val questionCount: Long = 0L,
    val published: Boolean = false,
    val featuredToday: Boolean = false,
    val assignmentMode: String = "ALL",
    val assignedBatch: String = "",
    val assignedStudentUids: List<String> = emptyList(),
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExamQuestion(
    @DocumentId val id: String = "",
    val testId: String = "",
    val orderIndex: Long = 0L,
    val subject: String = "Mathematics",
    val chapter: String = "",
    val questionText: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val marks: Double = 1.0,
    val published: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuestionAnswerKey(
    @DocumentId val id: String = "",
    val testId: String = "",
    val orderIndex: Long = 0L,
    val correctOptionIndex: Long = 0L,
    val explanation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

/**
 * In-memory editor model used strictly by Admin before splitting question vs protected answer key.
 */
data class AdminQuestionDraft(
    val id: String = "",
    val subject: String = "Mathematics",
    val chapter: String = "",
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctOptionIndex: Int = 0,
    val marks: Double = 1.0,
    val explanation: String = ""
)

@Keep
@IgnoreExtraProperties
data class TestAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val status: String = "in_progress",
    val answers: Map<String, Long> = emptyMap(),
    val markedForReview: List<String> = emptyList(),
    val currentQuestionIndex: Long = 0L,
    val remainingSeconds: Long = 0L,
    val elapsedSeconds: Long = 0L,
    @ServerTimestamp val startedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val submittedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TestResult(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val studentRollNo: String = "",
    val testId: String = "",
    val testTitle: String = "",
    val category: String = "Navodaya",
    val subject: String = "All Subjects",
    val totalMarks: Double = 0.0,
    val obtainedMarks: Double = 0.0,
    val percentage: Double = 0.0,
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val unattemptedCount: Long = 0L,
    val accuracy: Double = 0.0,
    val timeTakenSeconds: Long = 0L,
    val subjectBreakdown: Map<String, Map<String, Any>> = emptyMap(),
    val answers: Map<String, Long> = emptyMap(),
    @ServerTimestamp val submittedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class SubjectPerformanceItem(
    val subject: String,
    val correct: Int,
    val wrong: Int,
    val total: Int,
    val obtainedMarks: Double,
    val maxMarks: Double
) {
    val ratio: Float get() = if (total > 0) (correct.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
}

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val batch: String = "",
    val totalMarks: Double = 0.0,
    val averagePercentage: Double = 0.0,
    val testsCompleted: Long = 0L,
    val categoryStats: Map<String, Map<String, Any>> = emptyMap(),
    val visible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyNotePdf(
    @DocumentId val id: String = "",
    val title: String = "",
    val subject: String = "Mathematics",
    val chapter: String = "Number System",
    val materialType: String = "NOTES",
    val category: String = "Navodaya",
    val description: String = "",
    val pdfUrl: String = "",
    val contentMarkdown: String = "",
    val fileSizeLabel: String = "1.0 MB",
    val published: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetCategory: String = "ALL",
    val targetBatch: String = "",
    val published: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "app_config",
    val leaderboardEnabled: Boolean = true,
    val rankingMode: String = "PERCENTAGE",
    val welcomeMessage: String = "सीखें गणित, बढ़ें सफलता की ओर",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// --- DOMAIN ENGINES (PURE & UNIT-TESTABLE) ---

data class ComputedScoreSummary(
    val totalMarks: Double,
    val obtainedMarks: Double,
    val percentage: Double,
    val correctCount: Int,
    val wrongCount: Int,
    val unattemptedCount: Int,
    val accuracy: Double,
    val subjectBreakdown: Map<String, Map<String, Any>>
)

object ExamScoringEngine {
    fun scoreExam(
        questions: List<ExamQuestion>,
        answerKeys: List<QuestionAnswerKey>,
        selectedAnswers: Map<String, Int>,
        negativeMarking: Double
    ): ComputedScoreSummary {
        val keyByQuestionId = answerKeys.associateBy { it.id }
        var correct = 0
        var wrong = 0
        var unattempted = 0
        var totalMarks = 0.0
        var rawObtained = 0.0

        val subjectTotals = linkedMapOf<String, MutableMap<String, Double>>()

        for (q in questions) {
            val marks = if (q.marks > 0) q.marks else 1.0
            totalMarks += marks
            val subj = q.subject.ifBlank { "Mathematics" }
            val bucket = subjectTotals.getOrPut(subj) {
                mutableMapOf(
                    "correct" to 0.0,
                    "wrong" to 0.0,
                    "total" to 0.0,
                    "obtained" to 0.0,
                    "maxMarks" to 0.0
                )
            }
            bucket["total"] = (bucket["total"] ?: 0.0) + 1.0
            bucket["maxMarks"] = (bucket["maxMarks"] ?: 0.0) + marks

            val chosen = selectedAnswers[q.id]
            val key = keyByQuestionId[q.id]
            when {
                chosen == null || chosen < 0 -> {
                    unattempted++
                }
                key != null && chosen == key.correctOptionIndex.toInt() -> {
                    correct++
                    rawObtained += marks
                    bucket["correct"] = (bucket["correct"] ?: 0.0) + 1.0
                    bucket["obtained"] = (bucket["obtained"] ?: 0.0) + marks
                }
                else -> {
                    wrong++
                    rawObtained -= negativeMarking
                    bucket["wrong"] = (bucket["wrong"] ?: 0.0) + 1.0
                    bucket["obtained"] = (bucket["obtained"] ?: 0.0) - negativeMarking
                }
            }
        }

        val safeTotal = if (totalMarks > 0.0) totalMarks else 1.0
        val obtained = ((max(0.0, rawObtained) * 100.0).roundToInt() / 100.0)
        val percentage = (((obtained / safeTotal) * 100.0).coerceIn(0.0, 100.0) * 10.0).roundToInt() / 10.0
        val attempted = correct + wrong
        val accuracy = if (attempted > 0) {
            (((correct.toDouble() / attempted.toDouble()) * 100.0) * 10.0).roundToInt() / 10.0
        } else {
            0.0
        }

        val breakdownForFirestore: Map<String, Map<String, Any>> = subjectTotals.mapValues { (_, v) ->
            mapOf(
                "correct" to (v["correct"]?.toLong() ?: 0L),
                "wrong" to (v["wrong"]?.toLong() ?: 0L),
                "total" to (v["total"]?.toLong() ?: 0L),
                "obtained" to (max(0.0, v["obtained"] ?: 0.0)),
                "maxMarks" to (v["maxMarks"] ?: 0.0)
            )
        }

        return ComputedScoreSummary(
            totalMarks = safeTotal,
            obtainedMarks = obtained,
            percentage = percentage,
            correctCount = correct,
            wrongCount = wrong,
            unattemptedCount = unattempted,
            accuracy = accuracy,
            subjectBreakdown = breakdownForFirestore
        )
    }

    fun parseSubjectBreakdown(raw: Map<String, Map<String, Any>>): List<SubjectPerformanceItem> {
        return raw.entries.map { (subject, data) ->
            SubjectPerformanceItem(
                subject = subject,
                correct = (data["correct"] as? Number)?.toInt() ?: 0,
                wrong = (data["wrong"] as? Number)?.toInt() ?: 0,
                total = (data["total"] as? Number)?.toInt() ?: 0,
                obtainedMarks = (data["obtained"] as? Number)?.toDouble() ?: 0.0,
                maxMarks = (data["maxMarks"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

object TestEligibilityHelper {
    fun isEligibleForStudent(test: ExamTest, profile: UserProfile): Boolean {
        if (!test.published) return false
        if (!profile.active) return false
        return when (AssignmentMode.fromValue(test.assignmentMode)) {
            AssignmentMode.ALL -> true
            AssignmentMode.BATCH -> {
                test.assignedBatch.isBlank() ||
                    test.assignedBatch.equals(profile.batch, ignoreCase = true)
            }
            AssignmentMode.STUDENTS -> {
                test.assignedStudentUids.any { assigned ->
                    assigned.equals(profile.userId, ignoreCase = true) ||
                        assigned.equals(profile.studentId, ignoreCase = true)
                }
            }
        }
    }
}

object StreakCalculator {
    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayString(nowMillis: Long = System.currentTimeMillis()): String =
        dateFormat().format(Date(nowMillis))

    fun nextStreak(currentStreak: Long, lastTestDate: String, todayDate: String): Long {
        if (lastTestDate.isBlank()) return max(1L, currentStreak)
        if (lastTestDate == todayDate) return max(1L, currentStreak)
        return try {
            val sdf = dateFormat()
            val prev = sdf.parse(lastTestDate) ?: return 1L
            val today = sdf.parse(todayDate) ?: return 1L
            val cal = Calendar.getInstance().apply {
                time = prev
                add(Calendar.DAY_OF_YEAR, 1)
            }
            if (sdf.format(cal.time) == sdf.format(today)) {
                currentStreak + 1L
            } else {
                1L
            }
        } catch (_: Exception) {
            1L
        }
    }
}

object QuestionBulkParser {
    /**
     * Parses lines formatted as:
     * Subject | Question Text | Option A | Option B | Option C | Option D | Correct(A/B/C/D or 0-3) | Explanation
     */
    fun parseLines(rawText: String, defaultSubject: String, defaultChapter: String = ""): List<AdminQuestionDraft> {
        return rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split("|").map { it.trim() }
                if (parts.size < 7) return@mapNotNull null
                val subject = parts[0].ifBlank { defaultSubject }
                val question = parts[1]
                val a = parts[2]
                val b = parts[3]
                val c = parts[4]
                val d = parts[5]
                val correctRaw = parts[6].uppercase(Locale.US)
                val correctIdx = when (correctRaw) {
                    "A", "0", "1" -> if (correctRaw == "1") 0 else 0
                    "B" -> 1
                    "C" -> 2
                    "D" -> 3
                    else -> correctRaw.toIntOrNull()?.coerceIn(0, 3) ?: 0
                }.let {
                    when (correctRaw) {
                        "0" -> 0
                        "1" -> 1
                        "2" -> 2
                        "3" -> 3
                        "A" -> 0
                        "B" -> 1
                        "C" -> 2
                        "D" -> 3
                        else -> it
                    }
                }
                val explanation = parts.getOrNull(7) ?: ""
                if (question.isBlank() || a.isBlank() || b.isBlank() || c.isBlank() || d.isBlank()) {
                    null
                } else {
                    AdminQuestionDraft(
                        subject = subject,
                        chapter = defaultChapter,
                        questionText = question,
                        optionA = a,
                        optionB = b,
                        optionC = c,
                        optionD = d,
                        correctOptionIndex = correctIdx,
                        marks = 1.0,
                        explanation = explanation
                    )
                }
            }
    }
}

object CsvResultExporter {
    private fun escape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun toCsv(results: List<TestResult>): String {
        val sb = StringBuilder()
        sb.appendLine("Student Name,Student ID,Test Title,Category,Subject,Obtained Marks,Total Marks,Percentage,Correct,Wrong,Unattempted,Accuracy %,Time Taken (sec)")
        for (r in results) {
            sb.appendLine(
                listOf(
                    escape(r.studentName),
                    escape(r.studentRollNo),
                    escape(r.testTitle),
                    escape(r.category),
                    escape(r.subject),
                    r.obtainedMarks.toString(),
                    r.totalMarks.toString(),
                    r.percentage.toString(),
                    r.correctCount.toString(),
                    r.wrongCount.toString(),
                    r.unattemptedCount.toString(),
                    r.accuracy.toString(),
                    r.timeTakenSeconds.toString()
                ).joinToString(",")
            )
        }
        return sb.toString()
    }
}

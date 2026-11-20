package com.example

import com.example.data.model.CsvResultExporter
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamScoringEngine
import com.example.data.model.ExamTest
import com.example.data.model.QuestionAnswerKey
import com.example.data.model.QuestionBulkParser
import com.example.data.model.StreakCalculator
import com.example.data.model.TestEligibilityHelper
import com.example.data.model.TestResult
import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun examScoringEngine_computesPositiveNegativeUnattemptedAndSubjectBreakdown() {
        val questions = listOf(
            ExamQuestion(id = "q_1", subject = "Mathematics", marks = 2.0),
            ExamQuestion(id = "q_2", subject = "Mathematics", marks = 2.0),
            ExamQuestion(id = "q_3", subject = "Reasoning", marks = 1.0),
            ExamQuestion(id = "q_4", subject = "Hindi", marks = 1.0)
        )
        val keys = listOf(
            QuestionAnswerKey(id = "q_1", correctOptionIndex = 1L),
            QuestionAnswerKey(id = "q_2", correctOptionIndex = 2L),
            QuestionAnswerKey(id = "q_3", correctOptionIndex = 0L),
            QuestionAnswerKey(id = "q_4", correctOptionIndex = 3L)
        )
        // q_1 correct (+2), q_2 wrong (-0.5), q_3 correct (+1), q_4 unattempted (0)
        val selected = mapOf("q_1" to 1, "q_2" to 0, "q_3" to 0)

        val summary = ExamScoringEngine.scoreExam(
            questions = questions,
            answerKeys = keys,
            selectedAnswers = selected,
            negativeMarking = 0.5
        )

        assertEquals(6.0, summary.totalMarks, 0.001)
        assertEquals(2.5, summary.obtainedMarks, 0.001)
        assertEquals(2, summary.correctCount)
        assertEquals(1, summary.wrongCount)
        assertEquals(1, summary.unattemptedCount)
        assertEquals(66.7, summary.accuracy, 0.1)
        assertEquals(41.7, summary.percentage, 0.1)

        val breakdown = ExamScoringEngine.parseSubjectBreakdown(summary.subjectBreakdown)
        val mathRow = breakdown.first { it.subject == "Mathematics" }
        assertEquals(1, mathRow.correct)
        assertEquals(1, mathRow.wrong)
        assertEquals(2, mathRow.total)
        assertEquals(1.5, mathRow.obtainedMarks, 0.001)
    }

    @Test
    fun testEligibilityHelper_enforcesPublishActiveBatchAndStudentTargeting() {
        val studentA = UserProfile(userId = "uid_1", studentId = "MBS101", batch = "Navodaya-A", active = true)
        val studentB = UserProfile(userId = "uid_2", studentId = "MBS102", batch = "Sainik-B", active = true)
        val inactiveStudent = studentA.copy(active = false)

        val draftTest = ExamTest(id = "t1", published = false, assignmentMode = "ALL")
        assertFalse(TestEligibilityHelper.isEligibleForStudent(draftTest, studentA))

        val allTest = ExamTest(id = "t2", published = true, assignmentMode = "ALL")
        assertTrue(TestEligibilityHelper.isEligibleForStudent(allTest, studentA))
        assertFalse(TestEligibilityHelper.isEligibleForStudent(allTest, inactiveStudent))

        val batchTest = ExamTest(id = "t3", published = true, assignmentMode = "BATCH", assignedBatch = "Navodaya-A")
        assertTrue(TestEligibilityHelper.isEligibleForStudent(batchTest, studentA))
        assertFalse(TestEligibilityHelper.isEligibleForStudent(batchTest, studentB))

        val targetedTest = ExamTest(
            id = "t4",
            published = true,
            assignmentMode = "STUDENTS",
            assignedStudentUids = listOf("MBS102")
        )
        assertFalse(TestEligibilityHelper.isEligibleForStudent(targetedTest, studentA))
        assertTrue(TestEligibilityHelper.isEligibleForStudent(targetedTest, studentB))
    }

    @Test
    fun streakCalculator_tracksConsecutiveSameDayAndResetDays() {
        assertEquals(1L, StreakCalculator.nextStreak(0L, "", "2026-11-20"))
        assertEquals(4L, StreakCalculator.nextStreak(4L, "2026-11-20", "2026-11-20"))
        assertEquals(5L, StreakCalculator.nextStreak(4L, "2026-11-19", "2026-11-20"))
        assertEquals(1L, StreakCalculator.nextStreak(4L, "2026-11-17", "2026-11-20"))
    }

    @Test
    fun questionBulkParser_parsesPipeSeparatedQuestionsAndSkipsInvalidLines() {
        val raw = """
            # Comment line ignored
            Mathematics | What is 12 x 8? | 86 | 96 | 104 | 92 | B | 12 multiplied by 8 equals 96.
            Reasoning | Next in series 2, 4, 8, 16, ? | 24 | 30 | 32 | 36 | 2 | Each term doubles.
            Malformed | Only three columns | Skip
        """.trimIndent()

        val parsed = QuestionBulkParser.parseLines(raw, defaultSubject = "Mathematics", defaultChapter = "Number System")
        assertEquals(2, parsed.size)
        assertEquals("Mathematics", parsed[0].subject)
        assertEquals(1, parsed[0].correctOptionIndex)
        assertEquals("96", parsed[0].optionB)
        assertEquals("Reasoning", parsed[1].subject)
        assertEquals(2, parsed[1].correctOptionIndex)
    }

    @Test
    fun csvResultExporter_escapesQuotesAndIncludesHeaders() {
        val results = listOf(
            TestResult(
                studentName = "Aarav \"Topper\" Kumar",
                studentRollNo = "MBS-001",
                testTitle = "Navodaya Mock 1",
                category = "Navodaya",
                subject = "All Subjects",
                obtainedMarks = 28.0,
                totalMarks = 30.0,
                percentage = 93.3,
                correctCount = 28L,
                wrongCount = 2L,
                unattemptedCount = 0L,
                accuracy = 93.3,
                timeTakenSeconds = 1800L
            )
        )
        val csv = CsvResultExporter.toCsv(results)
        assertTrue(csv.startsWith("Student Name,Student ID,Test Title"))
        assertTrue(csv.contains("\"Aarav \"\"Topper\"\" Kumar\""))
        assertTrue(csv.contains("28.0,30.0,93.3"))
    }
}

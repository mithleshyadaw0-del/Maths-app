package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AdminQuestionDraft
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamScoringEngine
import com.example.data.model.ExamTest
import com.example.data.model.LeaderboardEntry
import com.example.data.model.QuestionAnswerKey
import com.example.data.model.StreakCalculator
import com.example.data.model.StudentRosterEntry
import com.example.data.model.StudyNotePdf
import com.example.data.model.TestAttempt
import com.example.data.model.TestResult
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

class MbsRepository(
    val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.logAndRethrow(uid: String, operation: OperationType, path: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, operation, path)
            }
            throw error
        }

    // --- AUTH & PROFILE ---

    suspend fun checkIsAdmin(uid: String, email: String?, emailVerified: Boolean): Boolean {
        if (email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) && emailVerified) {
            return true
        }
        return try {
            val snap = db.collection("admins").document(uid).get().await()
            snap.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "admins/$uid")
            false
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logAndRethrow(uid, OperationType.GET, path)
        )
    }

    suspend fun ensureUserProfile(
        requestedRole: String,
        studentIdInput: String,
        batchInput: String
    ): Result<UserProfile> {
        var docPath = "users/unauthenticated"
        return try {
            val user = auth.currentUser ?: error("Not signed in with Google.")
            val uid = user.uid
            docPath = "users/$uid"
            val userRef = db.collection("users").document(uid)
            val existingSnap = userRef.get().await()

            val verifiedAdmin = checkIsAdmin(uid, user.email, user.isEmailVerified)
            if (requestedRole == "admin" && !verifiedAdmin) {
                return Result.failure(
                    SecurityException("Unauthorized: Only verified administrators can use Admin Login.")
                )
            }

            if (existingSnap.exists()) {
                val existing = existingSnap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(userId = uid)

                // If student provided a Student ID on login and wants to update/link it
                val trimmedId = studentIdInput.trim()
                if (trimmedId.isNotBlank() && trimmedId != existing.studentId && !verifiedAdmin) {
                    val roster = tryClaimRosterEntry(uid, trimmedId)
                    val updates = mutableMapOf<String, Any>(
                        "studentId" to trimmedId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (roster != null) {
                        updates["batch"] = roster.batch
                        if (roster.name.isNotBlank()) updates["name"] = roster.name
                    }
                    userRef.update(updates).await()
                }
                val refreshed = userRef.get().await().toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: existing
                return Result.success(refreshed)
            }

            // New user registration
            val cleanStudentId = studentIdInput.trim().ifBlank {
                if (verifiedAdmin) "ADMIN-MBS" else "MBS-${uid.take(6).uppercase()}"
            }
            val claimedRoster = if (!verifiedAdmin) tryClaimRosterEntry(uid, cleanStudentId) else null
            if (claimedRoster != null && !claimedRoster.active) {
                return Result.failure(
                    SecurityException("This Student ID is currently deactivated. Please contact Mithlesh Sir.")
                )
            }
            val resolvedName = (claimedRoster?.name?.takeIf { it.isNotBlank() }
                ?: user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Student").take(80)
            val resolvedBatch = (claimedRoster?.batch?.takeIf { it.isNotBlank() }
                ?: batchInput.trim().ifBlank { "Navodaya-A" }).take(60)
            val resolvedRole = if (verifiedAdmin && requestedRole == "admin") "admin" else "student"

            val payload = mapOf(
                "userId" to uid,
                "name" to resolvedName,
                "email" to (user.email ?: "").take(150),
                "studentId" to cleanStudentId.take(40),
                "batch" to resolvedBatch,
                "role" to resolvedRole,
                "active" to true,
                "streakDays" to 0L,
                "totalTestsAttempted" to 0L,
                "totalMarksObtained" to 0.0,
                "averagePercentage" to 0.0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userRef.set(payload).await()
            val created = userRef.get().await().toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserProfile(
                id = uid,
                userId = uid,
                name = resolvedName,
                email = user.email ?: "",
                studentId = cleanStudentId,
                batch = resolvedBatch,
                role = resolvedRole,
                active = claimedRoster?.active ?: true
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun tryClaimRosterEntry(uid: String, rawStudentId: String): StudentRosterEntry? {
        val rosterDocId = rawStudentId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        if (rosterDocId.isBlank()) return null
        return try {
            val docRef = db.collection("student_roster").document(rosterDocId)
            val snap = docRef.get().await()
            if (!snap.exists()) return null
            val entry = snap.toObject(StudentRosterEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return null
            if (entry.active && entry.linkedUid.isBlank()) {
                docRef.update(
                    mapOf(
                        "linkedUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            entry
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    // --- STUDENT STREAMS ---

    fun observePublishedTests(): Flow<List<ExamTest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("tests")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ExamTest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(uid, OperationType.LIST, "tests")
        )
    }

    fun observeStudentAttempts(userId: String): Flow<List<TestAttempt>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/attempts"
        emitAll(
            db.collection("users").document(userId).collection("attempts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TestAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logAndRethrow(uid, OperationType.LIST, path)
        )
    }

    fun observeStudentResults(userId: String): Flow<List<TestResult>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("results")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TestResult::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.submittedAt?.seconds ?: 0L }
                }
                .logAndRethrow(uid, OperationType.LIST, "results")
        )
    }

    fun observeVisibleLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("leaderboard")
                .whereEqualTo("visible", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logAndRethrow(uid, OperationType.LIST, "leaderboard")
        )
    }

    fun observePublishedNotes(): Flow<List<StudyNotePdf>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("notes")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudyNotePdf::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(uid, OperationType.LIST, "notes")
        )
    }

    fun observePublishedAnnouncements(): Flow<List<Announcement>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("announcements")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(uid, OperationType.LIST, "announcements")
        )
    }

    fun observeAppSettings(): Flow<AppSettings> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("settings").document("app_config")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: AppSettings()
                    } else AppSettings()
                }
                .logAndRethrow(uid, OperationType.GET, "settings/app_config")
        )
    }

    // --- EXAM EXECUTION (START, AUTO-SAVE, SUBMIT, SOLUTIONS) ---

    suspend fun loadPublishedQuestions(testId: String): Result<List<ExamQuestion>> {
        val path = "tests/$testId/questions"
        return try {
            requireUserId()
            val snap = db.collection("tests").document(testId)
                .collection("questions")
                .whereEqualTo("published", true)
                .get()
                .await()
            val list = snap.toObjects(ExamQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.orderIndex }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getOrCreateAttempt(test: ExamTest): Result<TestAttempt> {
        var path = "users/unauthenticated/attempts/${test.id}"
        return try {
            val uid = requireUserId()
            path = "users/$uid/attempts/${test.id}"
            val docRef = db.collection("users").document(uid)
                .collection("attempts").document(test.id)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(TestAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: error("Corrupted attempt data.")
                return Result.success(existing)
            }
            val totalSeconds = (test.durationMinutes.coerceAtLeast(1L) * 60L).coerceAtMost(18000L)
            val payload = mapOf(
                "userId" to uid,
                "testId" to test.id,
                "status" to "in_progress",
                "answers" to emptyMap<String, Long>(),
                "markedForReview" to emptyList<String>(),
                "currentQuestionIndex" to 0L,
                "remainingSeconds" to totalSeconds,
                "elapsedSeconds" to 0L,
                "startedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val created = docRef.get().await()
                .toObject(TestAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: TestAttempt(
                    id = test.id,
                    userId = uid,
                    testId = test.id,
                    status = "in_progress",
                    remainingSeconds = totalSeconds
                )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun autoSaveAttempt(
        testId: String,
        answers: Map<String, Int>,
        markedForReview: Set<String>,
        currentQuestionIndex: Int,
        remainingSeconds: Long,
        elapsedSeconds: Long
    ): Result<Unit> {
        var path = "users/unauthenticated/attempts/$testId"
        return try {
            val uid = requireUserId()
            path = "users/$uid/attempts/$testId"
            val docRef = db.collection("users").document(uid)
                .collection("attempts").document(testId)
            val longAnswers = answers.mapValues { it.value.toLong() }
            docRef.update(
                mapOf(
                    "answers" to longAnswers,
                    "markedForReview" to markedForReview.toList().take(200),
                    "currentQuestionIndex" to currentQuestionIndex.coerceIn(0, 200).toLong(),
                    "remainingSeconds" to remainingSeconds.coerceIn(0L, 18000L),
                    "elapsedSeconds" to elapsedSeconds.coerceIn(0L, 18000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Securely locks attempt as 'submitted' (preventing duplicate submissions),
     * unlocks protected answer_keys post-submission, scores via ExamScoringEngine,
     * saves immutable result at /results/{uid}_{testId}, and updates real student stats & leaderboard.
     */
    suspend fun submitExamAndComputeResult(
        profile: UserProfile,
        test: ExamTest,
        questions: List<ExamQuestion>,
        answers: Map<String, Int>,
        markedForReview: Set<String>,
        currentQuestionIndex: Int,
        remainingSeconds: Long,
        elapsedSeconds: Long
    ): Result<TestResult> {
        var path = "results"
        return try {
            val uid = requireUserId()
            val resultId = "${uid}_${test.id}"
            path = "results/$resultId"

            // Prevent duplicate submissions
            val existingResultSnap = db.collection("results").document(resultId).get().await()
            if (existingResultSnap.exists()) {
                val existingRes = existingResultSnap.toObject(
                    TestResult::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existingRes != null) return Result.success(existingRes)
            }

            // Step 1: Lock attempt as submitted (terminal state lock in firestore.rules)
            val attemptRef = db.collection("users").document(uid)
                .collection("attempts").document(test.id)
            val attemptSnap = attemptRef.get().await()
            val currentStatus = attemptSnap.getString("status")
            val longAnswers = answers.mapValues { it.value.toLong() }

            if (currentStatus != "submitted") {
                path = attemptRef.path
                attemptRef.update(
                    mapOf(
                        "answers" to longAnswers,
                        "markedForReview" to markedForReview.toList().take(200),
                        "currentQuestionIndex" to currentQuestionIndex.coerceIn(0, 200).toLong(),
                        "remainingSeconds" to remainingSeconds.coerceIn(0L, 18000L),
                        "elapsedSeconds" to elapsedSeconds.coerceIn(0L, 18000L),
                        "status" to "submitted",
                        "submittedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Step 2: Now that attempt is locked as 'submitted', Firestore rules unlock answer_keys
            path = "tests/${test.id}/answer_keys"
            val keysSnap = db.collection("tests").document(test.id)
                .collection("answer_keys")
                .get()
                .await()
            val answerKeys = keysSnap.toObjects(
                QuestionAnswerKey::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )

            // Step 3: Score using ExamScoringEngine
            val scored = ExamScoringEngine.scoreExam(
                questions = questions,
                answerKeys = answerKeys,
                selectedAnswers = answers,
                negativeMarking = test.negativeMarking
            )

            // Step 4: Write immutable deterministic TestResult
            val resultRef = db.collection("results").document(resultId)
            path = resultRef.path
            val resultPayload = mapOf(
                "userId" to uid,
                "studentName" to profile.name.ifBlank { "Student" }.take(80),
                "studentRollNo" to profile.studentId.take(40),
                "testId" to test.id,
                "testTitle" to test.title.take(120),
                "category" to test.category,
                "subject" to test.subject.take(60),
                "totalMarks" to scored.totalMarks,
                "obtainedMarks" to scored.obtainedMarks,
                "percentage" to scored.percentage,
                "correctCount" to scored.correctCount.toLong(),
                "wrongCount" to scored.wrongCount.toLong(),
                "unattemptedCount" to scored.unattemptedCount.toLong(),
                "accuracy" to scored.accuracy,
                "timeTakenSeconds" to elapsedSeconds.coerceIn(0L, 18000L),
                "subjectBreakdown" to scored.subjectBreakdown,
                "answers" to longAnswers,
                "submittedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            resultRef.set(resultPayload).await()

            // Step 5: Recompute real student stats & leaderboard from actual submitted results
            recomputeStudentStatsAndLeaderboard(uid, profile)

            val savedResult = resultRef.get().await().toObject(
                TestResult::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: TestResult(
                id = resultId,
                userId = uid,
                studentName = profile.name,
                studentRollNo = profile.studentId,
                testId = test.id,
                testTitle = test.title,
                category = test.category,
                subject = test.subject,
                totalMarks = scored.totalMarks,
                obtainedMarks = scored.obtainedMarks,
                percentage = scored.percentage,
                correctCount = scored.correctCount.toLong(),
                wrongCount = scored.wrongCount.toLong(),
                unattemptedCount = scored.unattemptedCount.toLong(),
                accuracy = scored.accuracy,
                timeTakenSeconds = elapsedSeconds,
                subjectBreakdown = scored.subjectBreakdown,
                answers = longAnswers
            )
            Result.success(savedResult)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun recomputeStudentStatsAndLeaderboard(uid: String, profile: UserProfile) {
        val allResultsSnap = db.collection("results")
            .whereEqualTo("userId", uid)
            .get()
            .await()
        val allResults = allResultsSnap.toObjects(
            TestResult::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        val count = allResults.size
        val sumMarks = allResults.sumOf { it.obtainedMarks }
        val avgPct = if (count > 0) {
            ((allResults.sumOf { it.percentage } / count) * 10.0).roundToInt() / 10.0
        } else 0.0

        val today = StreakCalculator.todayString()
        val updatedStreak = StreakCalculator.nextStreak(profile.streakDays, profile.lastTestDate, today)

        db.collection("users").document(uid).update(
            mapOf(
                "streakDays" to updatedStreak,
                "totalTestsAttempted" to count.toLong(),
                "totalMarksObtained" to ((sumMarks * 100.0).roundToInt() / 100.0),
                "averagePercentage" to avgPct,
                "lastTestDate" to today,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val catStats: Map<String, Map<String, Any>> = allResults
            .groupBy { it.category }
            .mapValues { (_, list) ->
                val cMarks = (list.sumOf { it.obtainedMarks } * 100.0).roundToInt() / 100.0
                val cAvg = ((list.sumOf { it.percentage } / list.size) * 10.0).roundToInt() / 10.0
                mapOf(
                    "totalMarks" to cMarks,
                    "averagePercentage" to cAvg,
                    "testsCompleted" to list.size.toLong()
                )
            }

        val lbRef = db.collection("leaderboard").document(uid)
        val existingLb = lbRef.get().await()
        if (existingLb.exists()) {
            lbRef.update(
                mapOf(
                    "studentName" to profile.name.ifBlank { "Student" }.take(80),
                    "batch" to profile.batch.take(60),
                    "totalMarks" to ((sumMarks * 100.0).roundToInt() / 100.0),
                    "averagePercentage" to avgPct,
                    "testsCompleted" to count.toLong(),
                    "categoryStats" to catStats,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            lbRef.set(
                mapOf(
                    "userId" to uid,
                    "studentName" to profile.name.ifBlank { "Student" }.take(80),
                    "batch" to profile.batch.take(60),
                    "totalMarks" to ((sumMarks * 100.0).roundToInt() / 100.0),
                    "averagePercentage" to avgPct,
                    "testsCompleted" to count.toLong(),
                    "categoryStats" to catStats,
                    "visible" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun loadAnswerKeysAfterSubmission(testId: String): Result<List<QuestionAnswerKey>> {
        val path = "tests/$testId/answer_keys"
        return try {
            requireUserId()
            val snap = db.collection("tests").document(testId)
                .collection("answer_keys")
                .get()
                .await()
            val keys = snap.toObjects(
                QuestionAnswerKey::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.orderIndex }
            Result.success(keys)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    // --- ADMIN STREAMS ---

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(uid, OperationType.LIST, "users")
        )
    }

    fun observeStudentRosterAdmin(): Flow<List<StudentRosterEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("student_roster")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudentRosterEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(uid, OperationType.LIST, "student_roster")
        )
    }

    fun observeAllTestsAdmin(): Flow<List<ExamTest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("tests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ExamTest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(uid, OperationType.LIST, "tests")
        )
    }

    fun observeAllResultsAdmin(): Flow<List<TestResult>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("results")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TestResult::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.submittedAt?.seconds ?: 0L }
                }
                .logAndRethrow(uid, OperationType.LIST, "results")
        )
    }

    fun observeAllNotesAdmin(): Flow<List<StudyNotePdf>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("notes")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudyNotePdf::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(uid, OperationType.LIST, "notes")
        )
    }

    fun observeAllAnnouncementsAdmin(): Flow<List<Announcement>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("announcements")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(uid, OperationType.LIST, "announcements")
        )
    }

    // --- ADMIN MUTATIONS ---

    suspend fun addStudentRosterEntry(
        studentId: String,
        name: String,
        batch: String,
        targetCategory: String
    ): Result<Unit> {
        val cleanId = studentId.trim().uppercase()
        val docId = cleanId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val path = "student_roster/$docId"
        return try {
            val adminUid = requireUserId()
            require(cleanId.isNotBlank() && docId.isNotBlank()) { "Valid Student ID is required." }
            require(name.trim().isNotBlank()) { "Student name is required." }
            db.collection("student_roster").document(docId).set(
                mapOf(
                    "studentId" to cleanId.take(40),
                    "name" to name.trim().take(80),
                    "batch" to batch.trim().ifBlank { "Navodaya-A" }.take(60),
                    "targetCategory" to targetCategory,
                    "active" to true,
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStudentProfileAdmin(
        targetUid: String,
        name: String,
        studentId: String,
        batch: String,
        active: Boolean
    ): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            requireUserId()
            require(name.trim().isNotBlank()) { "Name cannot be empty." }
            require(studentId.trim().isNotBlank()) { "Student ID cannot be empty." }
            db.collection("users").document(targetUid).update(
                mapOf(
                    "name" to name.trim().take(80),
                    "studentId" to studentId.trim().take(40),
                    "batch" to batch.trim().take(60),
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleRosterActiveAdmin(rosterDocId: String, active: Boolean): Result<Unit> {
        val path = "student_roster/$rosterDocId"
        return try {
            requireUserId()
            db.collection("student_roster").document(rosterDocId).update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Critical Test Publishing Workflow (Requirement 14):
     * 1. Validate test metadata & every question (4 non-empty options, valid answer index 0..3)
     * 2. Save test document cleanly
     * 3. Save questions (without answer keys) AND separate protected answer_keys
     * 4. Set published status & assignment targeting
     */
    suspend fun saveAndPublishTest(
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
    ): Result<String> {
        var path = "tests"
        return try {
            val adminUid = requireUserId()
            val cleanTitle = title.trim()
            require(cleanTitle.length >= 2) { "Test name must be at least 2 characters." }
            require(durationMinutes in 1..300) { "Duration must be between 1 and 300 minutes." }
            require(totalMarks in 1.0..1000.0) { "Total marks must be between 1 and 1000." }
            require(negativeMarking in 0.0..5.0) { "Negative marking must be between 0 and 5." }
            if (publishNow) {
                require(questions.isNotEmpty()) { "Cannot publish an empty test. Add at least 1 question." }
            }
            questions.forEachIndexed { idx, q ->
                require(q.questionText.trim().isNotBlank()) { "Question ${idx + 1} text is empty." }
                require(
                    q.optionA.trim().isNotBlank() &&
                        q.optionB.trim().isNotBlank() &&
                        q.optionC.trim().isNotBlank() &&
                        q.optionD.trim().isNotBlank()
                ) { "Question ${idx + 1} must have all 4 answer options filled." }
                require(q.correctOptionIndex in 0..3) { "Question ${idx + 1} has invalid correct option." }
            }

            val testRef = if (!existingTestId.isNullOrBlank()) {
                db.collection("tests").document(existingTestId)
            } else {
                db.collection("tests").document()
            }
            val testId = testRef.id
            path = testRef.path

            val computedTotalMarks = if (questions.isNotEmpty()) {
                questions.sumOf { if (it.marks > 0) it.marks else 1.0 }
            } else totalMarks

            val baseFields = mutableMapOf<String, Any?>(
                "title" to cleanTitle.take(120),
                "description" to description.trim().take(400).takeIf { it.isNotBlank() },
                "category" to category,
                "testType" to testType,
                "subject" to subject.trim().ifBlank { "All Subjects" }.take(60),
                "chapter" to chapter.trim().take(80).takeIf { it.isNotBlank() },
                "durationMinutes" to durationMinutes.toLong(),
                "totalMarks" to computedTotalMarks.coerceIn(1.0, 1000.0),
                "negativeMarking" to negativeMarking.coerceIn(0.0, 5.0),
                "questionCount" to questions.size.toLong(),
                "published" to publishNow,
                "featuredToday" to featuredToday,
                "assignmentMode" to assignmentMode,
                "assignedBatch" to assignedBatch.trim().take(60).takeIf { it.isNotBlank() },
                "assignedStudentUids" to assignedStudentIds.map { it.trim() }.filter { it.isNotBlank() }.take(200),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val existingSnap = testRef.get().await()
            if (existingSnap.exists()) {
                val updatePayload = baseFields.filterValues { it != null } as Map<String, Any>
                testRef.update(updatePayload).await()
            } else {
                baseFields["createdBy"] = adminUid
                baseFields["createdAt"] = FieldValue.serverTimestamp()
                val createPayload = baseFields.filterValues { it != null } as Map<String, Any>
                testRef.set(createPayload).await()
            }

            // Replace questions and protected answer_keys cleanly
            val oldQs = testRef.collection("questions").get().await()
            val oldKeys = testRef.collection("answer_keys").get().await()
            val deleteBatch = db.batch()
            oldQs.documents.forEach { deleteBatch.delete(it.reference) }
            oldKeys.documents.forEach { deleteBatch.delete(it.reference) }
            if (!oldQs.isEmpty || !oldKeys.isEmpty) {
                deleteBatch.commit().await()
            }

            // Commit new questions + protected answer_keys in batches of 150
            questions.chunked(150).forEachIndexed { chunkIdx, chunk ->
                val writeBatch = db.batch()
                chunk.forEachIndexed { localIdx, q ->
                    val globalIdx = chunkIdx * 150 + localIdx
                    val qId = "q_${globalIdx + 1}"
                    val qRef = testRef.collection("questions").document(qId)
                    val kRef = testRef.collection("answer_keys").document(qId)

                    val qMap = mutableMapOf<String, Any?>(
                        "testId" to testId,
                        "orderIndex" to globalIdx.toLong(),
                        "subject" to q.subject.trim().ifBlank { subject }.take(60),
                        "chapter" to q.chapter.trim().take(80).takeIf { it.isNotBlank() },
                        "questionText" to q.questionText.trim().take(1000),
                        "options" to listOf(
                            q.optionA.trim(),
                            q.optionB.trim(),
                            q.optionC.trim(),
                            q.optionD.trim()
                        ),
                        "marks" to (if (q.marks > 0) q.marks else 1.0).coerceAtMost(20.0),
                        "published" to publishNow,
                        "createdAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null } as Map<String, Any>

                    val kMap = mapOf(
                        "testId" to testId,
                        "orderIndex" to globalIdx.toLong(),
                        "correctOptionIndex" to q.correctOptionIndex.coerceIn(0, 3).toLong(),
                        "explanation" to q.explanation.trim().take(1000),
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    writeBatch.set(qRef, qMap)
                    writeBatch.set(kRef, kMap)
                }
                writeBatch.commit().await()
            }

            Result.success(testId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTestPublishAdmin(test: ExamTest, publish: Boolean): Result<Unit> {
        val path = "tests/${test.id}"
        return try {
            requireUserId()
            if (publish && test.questionCount <= 0L) {
                return Result.failure(IllegalStateException("Add at least 1 question before publishing."))
            }
            val testRef = db.collection("tests").document(test.id)
            val qSnap = testRef.collection("questions").get().await()
            if (publish && qSnap.isEmpty) {
                return Result.failure(IllegalStateException("No questions found in this test."))
            }
            val batch = db.batch()
            batch.update(
                testRef,
                mapOf(
                    "published" to publish,
                    "questionCount" to qSnap.size().toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            qSnap.documents.forEach { doc ->
                batch.update(doc.reference, mapOf("published" to publish))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun loadAdminTestQuestionsWithKeys(testId: String): Result<List<AdminQuestionDraft>> {
        val path = "tests/$testId/questions"
        return try {
            requireUserId()
            val qSnap = db.collection("tests").document(testId).collection("questions").get().await()
            val kSnap = db.collection("tests").document(testId).collection("answer_keys").get().await()
            val questions = qSnap.toObjects(ExamQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.orderIndex }
            val keys = kSnap.toObjects(QuestionAnswerKey::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .associateBy { it.id }
            val drafts = questions.map { q ->
                val k = keys[q.id]
                AdminQuestionDraft(
                    id = q.id,
                    subject = q.subject,
                    chapter = q.chapter,
                    questionText = q.questionText,
                    optionA = q.options.getOrElse(0) { "" },
                    optionB = q.options.getOrElse(1) { "" },
                    optionC = q.options.getOrElse(2) { "" },
                    optionD = q.options.getOrElse(3) { "" },
                    correctOptionIndex = k?.correctOptionIndex?.toInt() ?: 0,
                    marks = q.marks,
                    explanation = k?.explanation ?: ""
                )
            }
            Result.success(drafts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTestAdmin(testId: String): Result<Unit> {
        val path = "tests/$testId"
        return try {
            requireUserId()
            val testRef = db.collection("tests").document(testId)
            val qs = testRef.collection("questions").get().await()
            val ks = testRef.collection("answer_keys").get().await()
            val batch = db.batch()
            qs.documents.forEach { batch.delete(it.reference) }
            ks.documents.forEach { batch.delete(it.reference) }
            batch.delete(testRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveStudyNoteAdmin(
        title: String,
        subject: String,
        chapter: String,
        materialType: String,
        category: String,
        description: String,
        pdfUrl: String,
        contentMarkdown: String,
        fileSizeLabel: String
    ): Result<Unit> {
        var path = "notes"
        return try {
            val adminUid = requireUserId()
            require(title.trim().length >= 2) { "Note title must be at least 2 characters." }
            val docRef = db.collection("notes").document()
            path = docRef.path
            val payload = mutableMapOf<String, Any?>(
                "title" to title.trim().take(120),
                "subject" to subject.trim().take(60),
                "chapter" to chapter.trim().ifBlank { "General" }.take(80),
                "materialType" to materialType,
                "category" to category.trim().ifBlank { "Navodaya" }.take(40),
                "description" to description.trim().take(400).takeIf { it.isNotBlank() },
                "pdfUrl" to pdfUrl.trim().take(500).takeIf { it.isNotBlank() },
                "contentMarkdown" to contentMarkdown.trim().take(20000).takeIf { it.isNotBlank() },
                "fileSizeLabel" to fileSizeLabel.trim().ifBlank { "1.2 MB" }.take(30),
                "published" to true,
                "createdBy" to adminUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNoteAdmin(noteId: String): Result<Unit> {
        val path = "notes/$noteId"
        return try {
            requireUserId()
            db.collection("notes").document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun sendAnnouncementAdmin(
        title: String,
        message: String,
        targetCategory: String,
        targetBatch: String
    ): Result<Unit> {
        var path = "announcements"
        return try {
            val adminUid = requireUserId()
            require(title.trim().length >= 2) { "Announcement title is too short." }
            require(message.trim().length >= 2) { "Announcement message is too short." }
            val docRef = db.collection("announcements").document()
            path = docRef.path
            val payload = mutableMapOf<String, Any?>(
                "title" to title.trim().take(120),
                "message" to message.trim().take(1000),
                "targetCategory" to targetCategory.trim().ifBlank { "ALL" }.take(40),
                "targetBatch" to targetBatch.trim().take(60).takeIf { it.isNotBlank() },
                "published" to true,
                "createdBy" to adminUid,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncementAdmin(announcementId: String): Result<Unit> {
        val path = "announcements/$announcementId"
        return try {
            requireUserId()
            db.collection("announcements").document(announcementId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAppSettingsAdmin(
        leaderboardEnabled: Boolean,
        rankingMode: String,
        welcomeMessage: String
    ): Result<Unit> {
        val path = "settings/app_config"
        return try {
            val adminUid = requireUserId()
            val payload = mutableMapOf<String, Any?>(
                "leaderboardEnabled" to leaderboardEnabled,
                "rankingMode" to rankingMode,
                "welcomeMessage" to welcomeMessage.trim().take(200).takeIf { it.isNotBlank() },
                "updatedBy" to adminUid,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.collection("settings").document("app_config").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}

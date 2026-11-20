package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.LocalAttemptDraft
import com.example.data.firebase.LocalAttemptDraftStore
import com.example.data.firebase.MbsRepository
import com.example.data.firebase.MbsStarterSeeder
import com.example.data.firebase.extractFirestoreRootError
import com.example.data.model.AdminQuestionDraft
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTest
import com.example.data.model.LeaderboardEntry
import com.example.data.model.QuestionAnswerKey
import com.example.data.model.StudentRosterEntry
import com.example.data.model.StudyNotePdf
import com.example.data.model.TestAttempt
import com.example.data.model.TestEligibilityHelper
import com.example.data.model.TestResult
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ActiveExamUiState(
    val loading: Boolean = false,
    val test: ExamTest? = null,
    val questions: List<ExamQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<String, Int> = emptyMap(),
    val markedForReview: Set<String> = emptySet(),
    val remainingSeconds: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val submitting: Boolean = false,
    val submittedResult: TestResult? = null,
    val answerKeys: List<QuestionAnswerKey> = emptyList(),
    val errorMessage: String? = null
)

data class SessionBootstrapState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val verifiedAdmin: Boolean = false,
    val activePortal: String = "student", // "student" or "admin"
    val errorMessage: String? = null
)

class MbsViewModel(
    private val repository: MbsRepository,
    private val draftStore: LocalAttemptDraftStore,
    val currentUserId: String,
    private val userEmail: String?,
    private val emailVerified: Boolean,
    private val initialRequestedPortal: String,
    private val initialStudentId: String
) : ViewModel() {

    private companion object {
        const val TAG = "MbsViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }

    private val _session = MutableStateFlow(SessionBootstrapState(loading = true))
    val session: StateFlow<SessionBootstrapState> = _session.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _activeExam = MutableStateFlow(ActiveExamUiState())
    val activeExam: StateFlow<ActiveExamUiState> = _activeExam.asStateFlow()

    private val _viewingResult = MutableStateFlow<TestResult?>(null)
    val viewingResult: StateFlow<TestResult?> = _viewingResult.asStateFlow()

    private val _viewingResultQuestions = MutableStateFlow<List<ExamQuestion>>(emptyList())
    val viewingResultQuestions: StateFlow<List<ExamQuestion>> = _viewingResultQuestions.asStateFlow()

    private val _viewingResultKeys = MutableStateFlow<List<QuestionAnswerKey>>(emptyList())
    val viewingResultKeys: StateFlow<List<QuestionAnswerKey>> = _viewingResultKeys.asStateFlow()

    private var timerJob: Job? = null
    private var autoSaveTickCounter = 0

    // --- TWO-TIER FLOW UNWRAPPING HELPER ---
    private fun <T> Flow<T>.asUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = extractFirestoreRootError(error)
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    // --- STUDENT STREAMS ---
    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).asUiState("profile")

    val publishedTestsState: StateFlow<UiState<List<ExamTest>>> =
        repository.observePublishedTests().asUiState("tests")

    val studentAttemptsState: StateFlow<UiState<List<TestAttempt>>> =
        repository.observeStudentAttempts(currentUserId).asUiState("attempts")

    val studentResultsState: StateFlow<UiState<List<TestResult>>> =
        repository.observeStudentResults(currentUserId).asUiState("results")

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
        repository.observeVisibleLeaderboard().asUiState("leaderboard")

    val publishedNotesState: StateFlow<UiState<List<StudyNotePdf>>> =
        repository.observePublishedNotes().asUiState("notes")

    val publishedAnnouncementsState: StateFlow<UiState<List<Announcement>>> =
        repository.observePublishedAnnouncements().asUiState("announcements")

    val appSettingsState: StateFlow<UiState<AppSettings>> =
        repository.observeAppSettings().asUiState("settings")

    // --- ADMIN STREAMS (Collected ONLY inside AdminDashboardScreen when verifiedAdmin == true) ---
    val adminUsersState: StateFlow<UiState<List<UserProfile>>> by lazy {
        repository.observeAllUsersAdmin().asUiState("admin_users")
    }
    val adminRosterState: StateFlow<UiState<List<StudentRosterEntry>>> by lazy {
        repository.observeStudentRosterAdmin().asUiState("admin_roster")
    }
    val adminTestsState: StateFlow<UiState<List<ExamTest>>> by lazy {
        repository.observeAllTestsAdmin().asUiState("admin_tests")
    }
    val adminResultsState: StateFlow<UiState<List<TestResult>>> by lazy {
        repository.observeAllResultsAdmin().asUiState("admin_results")
    }
    val adminNotesState: StateFlow<UiState<List<StudyNotePdf>>> by lazy {
        repository.observeAllNotesAdmin().asUiState("admin_notes")
    }
    val adminAnnouncementsState: StateFlow<UiState<List<Announcement>>> by lazy {
        repository.observeAllAnnouncementsAdmin().asUiState("admin_announcements")
    }

    init {
        bootstrapSession()
    }

    private fun bootstrapSession() {
        viewModelScope.launch {
            _session.update { it.copy(loading = true, errorMessage = null) }
            val isAdmin = repository.checkIsAdmin(currentUserId, userEmail, emailVerified)
            val result = repository.ensureUserProfile(
                requestedRole = initialRequestedPortal,
                studentIdInput = initialStudentId,
                batchInput = "Navodaya-A"
            )
            result.fold(
                onSuccess = { profile ->
                    val portal = if (isAdmin && initialRequestedPortal == "admin") "admin" else "student"
                    _session.value = SessionBootstrapState(
                        loading = false,
                        profile = profile,
                        verifiedAdmin = isAdmin,
                        activePortal = portal
                    )
                },
                onFailure = { err ->
                    _session.value = SessionBootstrapState(
                        loading = false,
                        profile = null,
                        verifiedAdmin = isAdmin,
                        activePortal = "student",
                        errorMessage = err.localizedMessage ?: "Account verification failed."
                    )
                }
            )
        }
    }

    fun switchPortal(targetPortal: String) {
        if (targetPortal == "admin" && !_session.value.verifiedAdmin) {
            postMessage("Only authorized administrators can open Admin Dashboard.")
            return
        }
        _session.update { it.copy(activePortal = targetPortal) }
    }

    fun postMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    // --- EXAM LIFECYCLE (START, RESTORE, AUTO-SAVE, SUBMIT) ---

    fun startOrResumeTest(test: ExamTest, profile: UserProfile) {
        if (!TestEligibilityHelper.isEligibleForStudent(test, profile)) {
            postMessage("This test is not assigned to your batch or account.")
            return
        }
        stopTimer()
        viewModelScope.launch {
            _activeExam.value = ActiveExamUiState(loading = true, test = test)

            val qResult = repository.loadPublishedQuestions(test.id)
            val questions = qResult.getOrElse { err ->
                _activeExam.value = ActiveExamUiState(
                    loading = false,
                    errorMessage = err.localizedMessage ?: "Unable to load test questions."
                )
                return@launch
            }
            if (questions.isEmpty()) {
                _activeExam.value = ActiveExamUiState(
                    loading = false,
                    errorMessage = "No questions are available in this test yet."
                )
                return@launch
            }

            val attemptRes = repository.getOrCreateAttempt(test)
            val remoteAttempt = attemptRes.getOrElse { err ->
                _activeExam.value = ActiveExamUiState(
                    loading = false,
                    errorMessage = err.localizedMessage ?: "Unable to start exam attempt."
                )
                return@launch
            }

            if (remoteAttempt.status == "submitted") {
                postMessage("You have already submitted this test. Opening your result.")
                _activeExam.value = ActiveExamUiState(loading = false)
                val existingResults = (studentResultsState.value as? UiState.Success)?.data.orEmpty()
                existingResults.firstOrNull { it.testId == test.id }?.let { openSavedResult(it) }
                return@launch
            }

            // Merge remote attempt with local draft if local draft has newer progress after restart
            val localDraft = draftStore.loadDraft(currentUserId, test.id)
            val defaultSeconds = (test.durationMinutes.coerceAtLeast(1L) * 60L)
            val remoteAnswers = remoteAttempt.answers.mapValues { it.value.toInt() }
            val mergedAnswers = if (localDraft != null && localDraft.answers.size >= remoteAnswers.size) {
                localDraft.answers
            } else remoteAnswers
            val mergedReview = if (localDraft != null && localDraft.markedForReview.isNotEmpty()) {
                localDraft.markedForReview
            } else remoteAttempt.markedForReview.toSet()
            val mergedIndex = (localDraft?.currentQuestionIndex ?: remoteAttempt.currentQuestionIndex.toInt())
                .coerceIn(0, (questions.size - 1).coerceAtLeast(0))
            val mergedRemaining = listOfNotNull(
                localDraft?.remainingSeconds?.takeIf { it > 0 },
                remoteAttempt.remainingSeconds.takeIf { it > 0 }
            ).minOrNull() ?: defaultSeconds
            val mergedElapsed = maxOf(localDraft?.elapsedSeconds ?: 0L, remoteAttempt.elapsedSeconds)

            _activeExam.value = ActiveExamUiState(
                loading = false,
                test = test,
                questions = questions,
                currentIndex = mergedIndex,
                answers = mergedAnswers,
                markedForReview = mergedReview,
                remainingSeconds = mergedRemaining,
                elapsedSeconds = mergedElapsed
            )
            startExamTimer(profile)
        }
    }

    private fun startExamTimer(profile: UserProfile) {
        stopTimer()
        autoSaveTickCounter = 0
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _activeExam.value
                if (state.test == null || state.submitting || state.submittedResult != null) break
                val nextRemaining = (state.remainingSeconds - 1L).coerceAtLeast(0L)
                val nextElapsed = state.elapsedSeconds + 1L
                _activeExam.update {
                    it.copy(remainingSeconds = nextRemaining, elapsedSeconds = nextElapsed)
                }
                autoSaveTickCounter++
                if (autoSaveTickCounter % 10 == 0) {
                    persistDraftLocally()
                }
                if (autoSaveTickCounter % 25 == 0) {
                    syncAttemptToCloud()
                }
                if (nextRemaining <= 0L) {
                    postMessage("Time expired! Automatically submitting your test.")
                    submitActiveExam(profile)
                    break
                }
            }
        }
    }

    fun selectAnswer(questionId: String, optionIndex: Int) {
        _activeExam.update { state ->
            state.copy(answers = state.answers + (questionId to optionIndex))
        }
        persistDraftLocally()
        viewModelScope.launch { syncAttemptToCloud() }
    }

    fun clearResponse(questionId: String) {
        _activeExam.update { state ->
            state.copy(answers = state.answers - questionId)
        }
        persistDraftLocally()
        viewModelScope.launch { syncAttemptToCloud() }
    }

    fun toggleMarkForReview(questionId: String) {
        _activeExam.update { state ->
            val updated = if (questionId in state.markedForReview) {
                state.markedForReview - questionId
            } else {
                state.markedForReview + questionId
            }
            state.copy(markedForReview = updated)
        }
        persistDraftLocally()
    }

    fun jumpToQuestion(index: Int) {
        _activeExam.update { state ->
            val safe = index.coerceIn(0, (state.questions.size - 1).coerceAtLeast(0))
            state.copy(currentIndex = safe)
        }
        persistDraftLocally()
    }

    private fun persistDraftLocally() {
        val state = _activeExam.value
        val test = state.test ?: return
        draftStore.saveDraft(
            currentUserId,
            LocalAttemptDraft(
                testId = test.id,
                answers = state.answers,
                markedForReview = state.markedForReview,
                currentQuestionIndex = state.currentIndex,
                remainingSeconds = state.remainingSeconds,
                elapsedSeconds = state.elapsedSeconds,
                savedAtMillis = System.currentTimeMillis()
            )
        )
    }

    private suspend fun syncAttemptToCloud() {
        val state = _activeExam.value
        val test = state.test ?: return
        if (state.submitting || state.submittedResult != null) return
        repository.autoSaveAttempt(
            testId = test.id,
            answers = state.answers,
            markedForReview = state.markedForReview,
            currentQuestionIndex = state.currentIndex,
            remainingSeconds = state.remainingSeconds,
            elapsedSeconds = state.elapsedSeconds
        )
    }

    fun pauseAndExitExam() {
        persistDraftLocally()
        stopTimer()
        viewModelScope.launch {
            syncAttemptToCloud()
            _activeExam.value = ActiveExamUiState()
        }
    }

    fun submitActiveExam(profile: UserProfile) {
        val state = _activeExam.value
        val test = state.test ?: return
        if (state.submitting || state.submittedResult != null) return
        stopTimer()
        viewModelScope.launch {
            _activeExam.update { it.copy(submitting = true, errorMessage = null) }
            val submitRes = repository.submitExamAndComputeResult(
                profile = profile,
                test = test,
                questions = state.questions,
                answers = state.answers,
                markedForReview = state.markedForReview,
                currentQuestionIndex = state.currentIndex,
                remainingSeconds = state.remainingSeconds,
                elapsedSeconds = state.elapsedSeconds
            )
            submitRes.fold(
                onSuccess = { testResult ->
                    draftStore.clearDraft(currentUserId, test.id)
                    val keys = repository.loadAnswerKeysAfterSubmission(test.id).getOrDefault(emptyList())
                    _activeExam.update {
                        it.copy(
                            submitting = false,
                            submittedResult = testResult,
                            answerKeys = keys
                        )
                    }
                    _viewingResult.value = testResult
                    _viewingResultQuestions.value = state.questions
                    _viewingResultKeys.value = keys
                },
                onFailure = { err ->
                    _activeExam.update {
                        it.copy(
                            submitting = false,
                            errorMessage = err.localizedMessage ?: "Submission failed. Please retry."
                        )
                    }
                }
            )
        }
    }

    fun openSavedResult(result: TestResult) {
        viewModelScope.launch {
            _viewingResult.value = result
            val qs = repository.loadPublishedQuestions(result.testId).getOrDefault(emptyList())
            val keys = repository.loadAnswerKeysAfterSubmission(result.testId).getOrDefault(emptyList())
            _viewingResultQuestions.value = qs
            _viewingResultKeys.value = keys
        }
    }

    fun closeResultViewer() {
        _viewingResult.value = null
        _activeExam.value = ActiveExamUiState()
    }

    // --- ADMIN ACTIONS ---

    fun seedStarterCurriculum() {
        viewModelScope.launch {
            val res = MbsStarterSeeder.publishStarterCurriculum(repository)
            res.fold(
                onSuccess = { msg -> postMessage(msg) },
                onFailure = { err -> postMessage(err.localizedMessage ?: "Failed to publish starter pack.") }
            )
        }
    }

    fun addRosterStudent(studentId: String, name: String, batch: String, category: String) {
        viewModelScope.launch {
            repository.addStudentRosterEntry(studentId, name, batch, category).fold(
                onSuccess = { postMessage("Student ID $studentId created successfully.") },
                onFailure = { postMessage(it.localizedMessage ?: "Failed to create Student ID.") }
            )
        }
    }

    fun updateStudentAdmin(targetUid: String, name: String, studentId: String, batch: String, active: Boolean) {
        viewModelScope.launch {
            repository.updateStudentProfileAdmin(targetUid, name, studentId, batch, active).fold(
                onSuccess = { postMessage("Student profile updated.") },
                onFailure = { postMessage(it.localizedMessage ?: "Failed to update student.") }
            )
        }
    }

    fun toggleRosterActive(rosterId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleRosterActiveAdmin(rosterId, active).fold(
                onSuccess = { postMessage(if (active) "Student ID activated." else "Student ID deactivated.") },
                onFailure = { postMessage(it.localizedMessage ?: "Update failed.") }
            )
        }
    }

    fun saveTestAdmin(
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
        questions: List<AdminQuestionDraft>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveAndPublishTest(
                existingTestId = existingTestId,
                title = title,
                description = description,
                category = category,
                testType = testType,
                subject = subject,
                chapter = chapter,
                durationMinutes = durationMinutes,
                totalMarks = totalMarks,
                negativeMarking = negativeMarking,
                publishNow = publishNow,
                featuredToday = featuredToday,
                assignmentMode = assignmentMode,
                assignedBatch = assignedBatch,
                assignedStudentIds = assignedStudentIds,
                questions = questions
            ).fold(
                onSuccess = {
                    postMessage(
                        if (publishNow) "Test validated & published to eligible students!"
                        else "Test saved as draft."
                    )
                    onSuccess()
                },
                onFailure = { err ->
                    postMessage(err.localizedMessage ?: "Failed to save test.")
                }
            )
        }
    }

    fun loadAdminQuestions(testId: String, onLoaded: (List<AdminQuestionDraft>) -> Unit) {
        viewModelScope.launch {
            repository.loadAdminTestQuestionsWithKeys(testId).fold(
                onSuccess = { onLoaded(it) },
                onFailure = { postMessage(it.localizedMessage ?: "Failed to load questions.") }
            )
        }
    }

    fun togglePublishTest(test: ExamTest, publish: Boolean) {
        viewModelScope.launch {
            repository.toggleTestPublishAdmin(test, publish).fold(
                onSuccess = {
                    postMessage(if (publish) "Test published live to students!" else "Test unpublished.")
                },
                onFailure = { postMessage(it.localizedMessage ?: "Could not update publish status.") }
            )
        }
    }

    fun deleteTest(testId: String) {
        viewModelScope.launch {
            repository.deleteTestAdmin(testId).fold(
                onSuccess = { postMessage("Test deleted.") },
                onFailure = { postMessage(it.localizedMessage ?: "Failed to delete test.") }
            )
        }
    }

    fun saveNoteAdmin(
        title: String,
        subject: String,
        chapter: String,
        materialType: String,
        category: String,
        description: String,
        pdfUrl: String,
        contentMarkdown: String,
        fileSizeLabel: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveStudyNoteAdmin(
                title = title,
                subject = subject,
                chapter = chapter,
                materialType = materialType,
                category = category,
                description = description,
                pdfUrl = pdfUrl,
                contentMarkdown = contentMarkdown,
                fileSizeLabel = fileSizeLabel
            ).fold(
                onSuccess = {
                    postMessage("Study note / PDF published!")
                    onSuccess()
                },
                onFailure = { postMessage(it.localizedMessage ?: "Failed to publish note.") }
            )
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNoteAdmin(noteId).fold(
                onSuccess = { postMessage("Note deleted.") },
                onFailure = { postMessage(it.localizedMessage ?: "Failed to delete note.") }
            )
        }
    }

    fun sendAnnouncement(title: String, message: String, targetCategory: String, targetBatch: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.sendAnnouncementAdmin(title, message, targetCategory, targetBatch).fold(
                onSuccess = {
                    postMessage("Announcement sent to students!")
                    onSuccess()
                },
                onFailure = { postMessage(it.localizedMessage ?: "Failed to send announcement.") }
            )
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        viewModelScope.launch {
            repository.deleteAnnouncementAdmin(announcementId).fold(
                onSuccess = { postMessage("Announcement removed.") },
                onFailure = { postMessage(it.localizedMessage ?: "Delete failed.") }
            )
        }
    }

    fun updateSettings(leaderboardEnabled: Boolean, rankingMode: String, welcomeMessage: String) {
        viewModelScope.launch {
            repository.updateAppSettingsAdmin(leaderboardEnabled, rankingMode, welcomeMessage).fold(
                onSuccess = { postMessage("Leaderboard & app settings updated.") },
                onFailure = { postMessage(it.localizedMessage ?: "Failed to save settings.") }
            )
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

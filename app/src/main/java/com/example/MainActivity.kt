package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.LocalAttemptDraftStore
import com.example.data.firebase.MbsRepository
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.signOutSafely
import com.example.data.model.AppSettings
import com.example.data.model.ExamCategory
import com.example.data.model.TestEligibilityHelper
import com.example.ui.components.MbsEmptyStateCard
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ExamScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.NotesPdfScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StudentHomeScreen
import com.example.ui.screens.StudentProfileScreen
import com.example.ui.screens.StudentTestsFlowScreen
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.MbsTheme
import com.example.ui.theme.NavyDeep
import com.example.ui.viewmodel.MbsViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MbsTheme {
                MbsAppRoot()
            }
        }
    }
}

private enum class StudentTab(
    val labelHi: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("होम", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    TESTS("टेस्ट", Icons.Filled.Assignment, Icons.Outlined.Assignment, "nav_tab_tests"),
    NOTES("नोट्स", Icons.Filled.Description, Icons.Outlined.Description, "nav_tab_notes"),
    LEADERBOARD("रैंक", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_tab_leaderboard"),
    PROFILE("प्रोफ़ाइल", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile")
}

@Composable
fun MbsAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var requestedPortal by rememberSaveable { mutableStateOf("student") }
    var pendingStudentId by rememberSaveable { mutableStateOf("") }
    var authGateError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onUnauthenticated = {},
            scope = scope
        )
    }

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
        return
    }

    val user = currentUser
    if (user == null) {
        LoginScreen(
            externalError = authGateError,
            onAuthCompleted = { portal, studentId ->
                authGateError = null
                requestedPortal = portal
                pendingStudentId = studentId
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        // Key composition and ViewModel by UID so account switch tears down listeners cleanly
        key(user.uid) {
            AuthenticatedAppShell(
                uid = user.uid,
                email = user.email,
                emailVerified = user.isEmailVerified,
                requestedPortal = requestedPortal,
                pendingStudentId = pendingStudentId,
                onForceBackToLoginWithError = { msg ->
                    authGateError = msg
                    signOutSafely(credentialManager, scope) {
                        currentUser = null
                    }
                },
                onSignOut = {
                    authGateError = null
                    signOutSafely(credentialManager, scope) {
                        currentUser = null
                    }
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    uid: String,
    email: String?,
    emailVerified: Boolean,
    requestedPortal: String,
    pendingStudentId: String,
    onForceBackToLoginWithError: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel: MbsViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MbsViewModel(
                    repository = MbsRepository(db),
                    draftStore = LocalAttemptDraftStore(app),
                    currentUserId = uid,
                    userEmail = email,
                    emailVerified = emailVerified,
                    initialRequestedPortal = requestedPortal,
                    initialStudentId = pendingStudentId
                )
            }
        }
    )

    val session by viewModel.session.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val publishedTestsState by viewModel.publishedTestsState.collectAsStateWithLifecycle()
    val attemptsState by viewModel.studentAttemptsState.collectAsStateWithLifecycle()
    val resultsState by viewModel.studentResultsState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val notesState by viewModel.publishedNotesState.collectAsStateWithLifecycle()
    val announcementsState by viewModel.publishedAnnouncementsState.collectAsStateWithLifecycle()
    val settingsState by viewModel.appSettingsState.collectAsStateWithLifecycle()
    val activeExam by viewModel.activeExam.collectAsStateWithLifecycle()
    val viewingResult by viewModel.viewingResult.collectAsStateWithLifecycle()
    val viewingResultQuestions by viewModel.viewingResultQuestions.collectAsStateWithLifecycle()
    val viewingResultKeys by viewModel.viewingResultKeys.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // If unauthorized user tried Admin Login, sign out cleanly and report on LoginScreen
    LaunchedEffect(session.errorMessage) {
        val err = session.errorMessage
        if (err != null && session.profile == null) {
            onForceBackToLoginWithError(err)
        }
    }

    if (session.loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = NavyDeep)
                Spacer(Modifier.height(12.dp))
                Text("Verifying your Maths By Mithlesh Sir account...", fontWeight = FontWeight.Medium)
            }
        }
        return
    }

    val liveProfile = (profileState as? UiState.Success)?.data ?: session.profile
    if (liveProfile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            MbsEmptyStateCard(
                title = "Profile Unavailable",
                subtitle = session.errorMessage ?: "Unable to verify account profile.",
                actionLabel = "Back to Login",
                onAction = onSignOut
            )
        }
        return
    }

    // Account Deactivated Guard
    if (!liveProfile.active && !session.verifiedAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            MbsEmptyStateCard(
                title = "Account Deactivated",
                subtitle = "Your Student ID (${liveProfile.studentId}) is currently deactivated by the administrator. Please contact Mithlesh Sir.",
                actionLabel = "Sign Out",
                onAction = onSignOut
            )
        }
        return
    }

    // 1. Admin Portal
    if (session.activePortal == "admin" && session.verifiedAdmin) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onSwitchToStudentPreview = { viewModel.switchPortal("student") },
                    onSignOut = onSignOut
                )
            }
        }
        return
    }

    // 2. Active Full-Screen Exam Interface
    if (activeExam.loading || (activeExam.test != null && viewingResult == null)) {
        ExamScreen(
            state = activeExam,
            onSelectOption = { qId, opt -> viewModel.selectAnswer(qId, opt) },
            onClearResponse = { qId -> viewModel.clearResponse(qId) },
            onToggleReview = { qId -> viewModel.toggleMarkForReview(qId) },
            onJumpToQuestion = { idx -> viewModel.jumpToQuestion(idx) },
            onSubmitConfirm = { viewModel.submitActiveExam(liveProfile) },
            onPauseAndExit = { viewModel.pauseAndExitExam() }
        )
        return
    }

    // 3. Full-Screen Result & Detailed Solutions Screen
    val currentResult = viewingResult
    if (currentResult != null) {
        val leaderboard = (leaderboardState as? UiState.Success)?.data.orEmpty()
        val sortedLb = leaderboard.sortedByDescending { it.averagePercentage }
        val rank = sortedLb.indexOfFirst { it.userId == uid }.takeIf { it >= 0 }?.plus(1)
        ResultScreen(
            result = currentResult,
            questions = viewingResultQuestions,
            answerKeys = viewingResultKeys,
            rankPosition = rank,
            onBackToHome = { viewModel.closeResultViewer() }
        )
        return
    }

    // 4. Student Main 5-Tab Dashboard
    var selectedTab by rememberSaveable { mutableStateOf(StudentTab.HOME) }
    var selectedCategoryForTests by remember { mutableStateOf<ExamCategory?>(null) }

    val allPublishedTests = (publishedTestsState as? UiState.Success)?.data.orEmpty()
    val eligibleTests = remember(allPublishedTests, liveProfile) {
        allPublishedTests.filter { TestEligibilityHelper.isEligibleForStudent(it, liveProfile) }
    }
    val attempts = (attemptsState as? UiState.Success)?.data.orEmpty()
    val results = (resultsState as? UiState.Success)?.data.orEmpty()
    val leaderboard = (leaderboardState as? UiState.Success)?.data.orEmpty()
    val notes = (notesState as? UiState.Success)?.data.orEmpty()
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
    val settings = (settingsState as? UiState.Success)?.data ?: AppSettings()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                StudentTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (tab != StudentTab.TESTS) selectedCategoryForTests = null
                            selectedTab = tab
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.labelHi
                            )
                        },
                        label = {
                            Text(
                                text = tab.labelHi,
                                fontFamily = DevanagariFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = NavyDeep,
                            indicatorColor = NavyDeep,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                StudentTab.HOME -> StudentHomeScreen(
                    profile = liveProfile,
                    eligibleTests = eligibleTests,
                    results = results,
                    attempts = attempts,
                    announcements = announcements,
                    welcomeTagline = settings.welcomeMessage,
                    onCategoryClick = { cat ->
                        selectedCategoryForTests = cat
                        selectedTab = StudentTab.TESTS
                    },
                    onStartTest = { test -> viewModel.startOrResumeTest(test, liveProfile) },
                    onViewResult = { res -> viewModel.openSavedResult(res) }
                )

                StudentTab.TESTS -> StudentTestsFlowScreen(
                    initialCategory = selectedCategoryForTests,
                    eligibleTests = eligibleTests,
                    results = results,
                    attempts = attempts,
                    onStartTest = { test -> viewModel.startOrResumeTest(test, liveProfile) },
                    onViewResult = { res -> viewModel.openSavedResult(res) },
                    onClearInitialCategory = { selectedCategoryForTests = null }
                )

                StudentTab.NOTES -> NotesPdfScreen(
                    notes = notes,
                    onNotify = { msg -> viewModel.postMessage(msg) }
                )

                StudentTab.LEADERBOARD -> LeaderboardScreen(
                    currentUserId = uid,
                    entries = leaderboard,
                    settings = settings
                )

                StudentTab.PROFILE -> StudentProfileScreen(
                    profile = liveProfile,
                    verifiedAdmin = session.verifiedAdmin,
                    onSwitchToAdmin = { viewModel.switchPortal("admin") },
                    onSignOut = onSignOut
                )
            }
        }
    }
}

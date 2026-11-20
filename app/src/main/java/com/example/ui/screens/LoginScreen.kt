package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.onGoogleSignInClicked
import com.example.ui.components.MbsErrorBanner
import com.example.ui.components.MbsLogoBadge
import com.example.ui.components.TeacherPortraitCircle
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.LocalMbsColors
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MintTint
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep

@Composable
fun LoginScreen(
    externalError: String?,
    onAuthCompleted: (requestedPortal: String, studentId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val ext = LocalMbsColors.current

    var selectedMode by rememberSaveable { mutableStateOf("student") } // "student" or "admin"
    var studentIdInput by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val displayedError = errorMessage ?: externalError

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("login_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 540.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(6.dp))

                // Header Identity with MBS Crest & Corrected Teacher Portrait
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    MbsLogoBadge(size = 66.dp)
                    Spacer(Modifier.width(12.dp))
                    TeacherPortraitCircle(size = 70.dp, ringWidth = 3.dp)
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyDeep,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.app_tagline_hi),
                    fontFamily = DevanagariFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.welcome_back),
                    style = MaterialTheme.typography.titleMedium,
                    color = NavyDeep,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                // Signature Dual Login Role Cards (matching attached UI reference)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Admin Login Card (Deep Navy)
                    val adminSelected = selectedMode == "admin"
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = NavyDeep),
                        border = if (adminSelected) {
                            BorderStroke(3.dp, GoldenYellow)
                        } else {
                            BorderStroke(1.dp, NavyCard)
                        },
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (adminSelected) 8.dp else 2.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(176.dp)
                            .testTag("admin_login_card")
                            .clickable {
                                selectedMode = "admin"
                                errorMessage = null
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(GoldenYellow.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = "Admin Login",
                                    tint = GoldenYellow,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Admin\nLogin",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Authorized Only",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Student Login Card (Golden Yellow)
                    val studentSelected = selectedMode == "student"
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = GoldenYellow),
                        border = if (studentSelected) {
                            BorderStroke(3.dp, NavyDeep)
                        } else {
                            BorderStroke(1.dp, GoldenYellow)
                        },
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (studentSelected) 8.dp else 2.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(176.dp)
                            .testTag("student_login_card")
                            .clickable {
                                selectedMode = "student"
                                errorMessage = null
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(NavyDeep.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = "Student Login",
                                    tint = NavyDeep,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Student\nLogin",
                                style = MaterialTheme.typography.titleMedium,
                                color = NavyDeep,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Student ID & Auth",
                                style = MaterialTheme.typography.bodySmall,
                                color = NavyDeep.copy(alpha = 0.78f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Active Login Panel
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ext.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (selectedMode == "admin") Icons.Filled.Lock else Icons.Filled.Badge,
                                contentDescription = null,
                                tint = if (selectedMode == "admin") NavyDeep else ext.indigoAccent
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (selectedMode == "admin") {
                                    "Administrator Verification"
                                } else {
                                    "Student Portal Sign-In"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NavyDeep
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = if (selectedMode == "admin") {
                                "Sign in with your verified administrator Google account. Public admin registration is disabled."
                            } else {
                                "Enter your Roll Number / Student ID issued by Mithlesh Sir (optional for first-time students) and sign in securely with Google."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AnimatedVisibility(visible = selectedMode == "student") {
                            Column {
                                Spacer(Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = studentIdInput,
                                    onValueChange = { studentIdInput = it.uppercase() },
                                    label = { Text("Student ID / Roll Number (e.g. MBS-2026-001)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Badge,
                                            contentDescription = null,
                                            tint = NavyDeep
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NavyDeep,
                                        unfocusedBorderColor = ext.cardBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("student_id_input")
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (displayedError != null) {
                            MbsErrorBanner(message = displayedError)
                            Spacer(Modifier.height(14.dp))
                        }

                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthCompleted(selectedMode, studentIdInput.trim())
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                        errorMessage = "Sign-in was cancelled. Please choose an account to continue."
                                    }
                                )
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMode == "admin") NavyDeep else GoldenYellow,
                                contentColor = if (selectedMode == "admin") Color.White else NavyDeep
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = if (selectedMode == "admin") GoldenYellow else NavyDeep,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Signing in...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (selectedMode == "admin") {
                                        "Admin Sign in with Google"
                                    } else {
                                        "Student Sign in with Google"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Free Education & Security Trust Strip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MintTint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MintAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "100% Free for All Students • Secured by Firebase Auth",
                            style = MaterialTheme.typography.labelMedium,
                            color = NavyDeep,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

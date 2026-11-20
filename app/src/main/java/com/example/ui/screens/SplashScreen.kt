package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.FloatingMathSymbolsBackdrop
import com.example.ui.components.MbsLogoBadge
import com.example.ui.components.TeacherPortraitCircle
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val scaleAnim = remember { Animatable(0.82f) }
    val alphaAnim = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650)
            )
        }
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1900, easing = FastOutSlowInEasing)
        )
        delay(220L)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F122D), NavyDeep, NavyCard)
                )
            )
            .testTag("splash_screen")
    ) {
        FloatingMathSymbolsBackdrop(
            modifier = Modifier.fillMaxSize(),
            symbolColor = GoldenYellow.copy(alpha = 0.14f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(0.5f))

            // Official color-corrected Mithlesh Sir portrait with golden ring
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.scale(scaleAnim.value)
            ) {
                TeacherPortraitCircle(
                    size = 186.dp,
                    ringWidth = 4.dp,
                    modifier = Modifier.padding(bottom = 26.dp)
                )
                MbsLogoBadge(size = 64.dp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Maths By",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Mithlesh Sir",
                style = MaterialTheme.typography.displayLarge,
                color = GoldenYellow,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.10f)
            ) {
                Text(
                    text = stringResource(R.string.app_tagline_hi),
                    fontFamily = DevanagariFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Navodaya • Sainik School • Simultala • Olympiad",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            LinearProgressIndicator(
                progress = { progressAnim.value },
                color = GoldenYellow,
                trackColor = Color.White.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Loading your classroom...",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}

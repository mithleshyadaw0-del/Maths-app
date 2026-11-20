package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ExamCategory
import com.example.data.model.ExamSubject
import com.example.data.model.StudyNotePdf
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberTint
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoTint
import com.example.ui.theme.LocalMbsColors
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MintTint
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkTint
import kotlin.math.sin

data class CategoryVisualStyle(
    val cardBg: Color,
    val badgeColor: Color,
    val badgeTextColor: Color = Color.White,
    val shortCode: String
)

fun categoryStyle(category: ExamCategory): CategoryVisualStyle = when (category) {
    ExamCategory.NAVODAYA -> CategoryVisualStyle(MintTint, MintAccent, Color.White, "JNV")
    ExamCategory.SAINIK_SCHOOL -> CategoryVisualStyle(IndigoTint, IndigoAccent, Color.White, "SS")
    ExamCategory.SIMULTALA -> CategoryVisualStyle(AmberTint, AmberAccent, NavyDeep, "SV")
    ExamCategory.OLYMPIAD -> CategoryVisualStyle(PinkTint, PinkAccent, Color.White, "OLY")
}

fun subjectBadgeColor(subject: ExamSubject): Color = when (subject) {
    ExamSubject.MATHEMATICS -> MintAccent
    ExamSubject.REASONING -> IndigoAccent
    ExamSubject.EVS -> AmberAccent
    ExamSubject.ENGLISH -> PinkAccent
    ExamSubject.HINDI -> NavyDeep
}

@Composable
fun MbsLogoBadge(
    size: Dp = 76.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(NavyDeep)
            .border(2.5.dp, GoldenYellow, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_mbs_launcher_icon_1795174307622),
            contentDescription = "Maths By Mithlesh Sir MBS Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun TeacherPortraitCircle(
    size: Dp = 152.dp,
    ringWidth: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(GoldenYellow.copy(alpha = 0.35f), NavyDeep)
                )
            )
            .border(
                width = ringWidth,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        GoldenYellow,
                        Color(0xFFFFD873),
                        GoldenYellow.copy(alpha = 0.65f),
                        GoldenYellow
                    )
                ),
                shape = CircleShape
            )
            .padding(ringWidth + 2.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_teacher_mithlesh_1795174310335),
            contentDescription = "Mithlesh Sir Official Portrait",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun FloatingMathSymbolsBackdrop(
    modifier: Modifier = Modifier,
    symbolColor: Color = GoldenYellow.copy(alpha = 0.14f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "math_symbols_float")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "float_phase"
    )

    val symbols = listOf("π", "√x", "+", "×", "÷", "∑", "%", "△", "∞")
    Box(modifier = modifier) {
        symbols.forEachIndexed { idx, sym ->
            val xFraction = when (idx % 3) {
                0 -> 0.08f + (idx * 0.03f)
                1 -> 0.45f + ((idx % 2) * 0.14f)
                else -> 0.80f - (idx * 0.02f)
            }
            val yFraction = 0.08f + (idx * 0.095f)
            val dy = sin(phase + idx * 0.9f) * 10f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = (xFraction * 320).dp,
                        top = ((yFraction * 620) + dy).coerceAtLeast(8f).dp
                    )
            ) {
                Text(
                    text = sym,
                    color = symbolColor,
                    fontSize = (24 + (idx % 3) * 8).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CircleBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "back_button"
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
            .size(48.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CircularScoreRing(
    obtainedText: String,
    percentage: Float,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (percentage.coerceIn(0f, 100f) / 100f) * 360f,
        animationSpec = tween(durationMillis = 900),
        label = "score_ring_sweep"
    )
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 12.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = Color.White.copy(alpha = 0.16f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = GoldenYellow,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = obtainedText,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = GoldenYellow
            )
        }
    }
}

@Composable
fun MbsEmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ext = LocalMbsColors.current
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ext.indigoTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inbox,
                    contentDescription = null,
                    tint = ext.indigoAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldenYellow,
                        contentColor = NavyDeep
                    )
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MbsErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalMbsColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ext.coralTint,
        border = BorderStroke(1.dp, ext.coralAccent.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = "Error",
                tint = ext.coralAccent
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ext.coralAccent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PdfPreviewDialog(
    note: StudyNotePdf,
    loading: Boolean,
    pages: List<Bitmap>,
    error: String?,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
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
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_pdf_preview")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close preview",
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${note.subject} • ${note.chapter}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldenYellow
                            )
                        }
                        Button(
                            onClick = onDownloadClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldenYellow,
                                contentColor = NavyDeep
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("download_pdf_from_preview")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Save PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                when {
                    loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NavyDeep)
                                Spacer(Modifier.height(12.dp))
                                Text("Preparing PDF preview...")
                            }
                        }
                    }
                    error != null -> {
                        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                            MbsErrorBanner(error)
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(pages) { index, bmp ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "PDF Page ${index + 1}",
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                                        )
                                        Text(
                                            text = "Page ${index + 1} of ${pages.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatCountdown(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hrs = safe / 3600
    val mins = (safe % 3600) / 60
    val secs = safe % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

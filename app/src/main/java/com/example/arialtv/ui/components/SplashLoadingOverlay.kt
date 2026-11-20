package com.example.arialtv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.arialtv.R
import com.example.arialtv.data.model.AppMetadata
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.DotMatrixFont
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.SignalRed
import kotlin.math.PI
import kotlin.math.cos

/**
 * Minimalist Nothing OS launch splash screen:
 * - Displays the `arial tv.` brand mark with a rhythmic traveling dot-matrix pulse.
 * - Includes a small, refined footer showing app version and developer credit.
 * - Crossfades smoothly into the loaded catalog after ~2.2 seconds.
 */
@Composable
fun SplashLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.92f) }

    LaunchedEffect(visible) {
        if (visible) {
            logoAlpha.animateTo(1f, animationSpec = tween(520, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(visible) {
        if (visible) {
            logoScale.animateTo(1f, animationSpec = tween(580, easing = FastOutSlowInEasing))
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(420, easing = FastOutSlowInEasing)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasBlack),
        ) {
            // Subtle centered monochrome/red ambient depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x16E11D25),
                                0.45f to Color(0x08FFFFFF),
                                1.0f to Color.Transparent,
                            ),
                        ),
                    ),
            )

            // Center: Brand logo + animated Nothing OS dot wave
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.arial_tv_logo_dark),
                    contentDescription = "arial tv.",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(42.dp),
                )

                NothingDotsLoader()
            }

            // Small Footer: Version + Developer Credit
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 26.dp)
                    .alpha(logoAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "v${AppMetadata.VERSION_NAME}  ·  DEVELOPED BY ${AppMetadata.DEVELOPER_NAME.uppercase()}",
                    fontFamily = DotMatrixFont,
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                    color = InkSecondary,
                )
                Text(
                    text = AppMetadata.GITHUB_HANDLE,
                    fontFamily = DotMatrixFont,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    color = InkMuted,
                )
            }
        }
    }
}

@Composable
private fun NothingDotsLoader(
    dotCount: Int = 7,
) {
    val infinite = rememberInfiniteTransition(label = "nothingDots")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dotWavePhase",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until dotCount) {
                val dotPos = i.toFloat() / (dotCount - 1).coerceAtLeast(1)
                // Circular wrapped distance from the moving wave crest
                var diff = (phase - dotPos)
                if (diff > 0.5f) diff -= 1f
                if (diff < -0.5f) diff += 1f
                val intensity = ((cos(diff * 2.0 * PI) + 1.0) / 2.0).toFloat()
                val sharpPeak = intensity * intensity * intensity

                val scale = 0.58f + 0.68f * sharpPeak
                val alpha = 0.22f + 0.78f * intensity
                val dotColor = if (sharpPeak > 0.62f) {
                    lerp(PureWhite, SignalRed, ((sharpPeak - 0.62f) / 0.38f).coerceIn(0f, 1f))
                } else {
                    PureWhite
                }

                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .scale(scale)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
        }

        Spacer(Modifier.height(2.dp))
    }
}

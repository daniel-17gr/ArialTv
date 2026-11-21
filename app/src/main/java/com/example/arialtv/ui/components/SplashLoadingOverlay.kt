package com.example.arialtv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.arialtv.R
import com.example.arialtv.data.model.AppMetadata
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.PureWhite

/**
 * Simple, lightweight YouTube TV–style launch screen:
 * - Solid dark canvas (zero heavy glows/gradients).
 * - Crisp `arial tv.` logo + 3 subtle minimal pulsing dots.
 * - Quiet small footer with version and developer credit.
 */
@Composable
fun SplashLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(200)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasBlack),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.arial_tv_logo_dark),
                    contentDescription = "arial tv.",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(38.dp),
                )

                SimpleDotsIndicator()
            }

            Text(
                text = "v${AppMetadata.VERSION_NAME}  ·  ${AppMetadata.DEVELOPER_NAME}",
                fontSize = 11.sp,
                color = InkMuted,
                letterSpacing = 0.6.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun SimpleDotsIndicator() {
    val infinite = rememberInfiniteTransition(label = "dots")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dotStep",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until 3) {
            val dist = kotlin.math.abs(progress - (i + 0.5f)).coerceAtMost(1.5f)
            val dotAlpha = (1f - (dist / 1.5f) * 0.72f).coerceIn(0.25f, 1f)
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(dotAlpha)
                    .clip(CircleShape)
                    .background(PureWhite),
            )
        }
    }
}

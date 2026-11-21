package com.example.arialtv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.arialtv.R
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.SignalRed
import com.example.arialtv.ui.theme.SurfaceElevated
import com.example.arialtv.ui.theme.SurfaceTonal
import kotlinx.coroutines.delay

@Composable
fun ExitConfirmationDialog(
    onStay: () -> Unit,
    onConfirmExit: () -> Unit,
) {
    val stayFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(60)
        runCatching { stayFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 340.dp, max = 440.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceTonal)
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.arial_tv_logo_dark),
                contentDescription = "arial tv.",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(22.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "EXIT ARIAL TV?",
                style = MaterialTheme.typography.labelMedium,
                color = InkSecondary,
            )
            Text(
                text = "Are you sure you want to close the app?",
                style = MaterialTheme.typography.bodyMedium,
                color = InkPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val pillShape = RoundedCornerShape(50)
                Surface(
                    onClick = onStay,
                    modifier = Modifier.focusRequester(stayFocus),
                    shape = ClickableSurfaceDefaults.shape(pillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = SurfaceElevated,
                        contentColor = InkPrimary,
                        focusedContainerColor = PureWhite,
                        focusedContentColor = InkOnWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                ) {
                    Text(
                        text = "Stay",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 11.dp),
                    )
                }

                Surface(
                    onClick = onConfirmExit,
                    shape = ClickableSurfaceDefaults.shape(pillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = SurfaceElevated,
                        contentColor = SignalRed,
                        focusedContainerColor = SignalRed,
                        focusedContentColor = PureWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                ) {
                    Text(
                        text = "Exit",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 11.dp),
                    )
                }
            }
        }
    }
}

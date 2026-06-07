package net.focustation.myapplication.ui.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.focustation.myapplication.R
import net.focustation.myapplication.ui.theme.FocusInk

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onTimeout()
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_thumbnail),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxWidth(0.62f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(40.dp)),
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Focustation",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = FocusInk,
            )
        }
    }
}

private const val SPLASH_DURATION_MS = 1400L

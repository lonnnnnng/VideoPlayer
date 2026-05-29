package com.zy.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.zy.player.ui.theme.AppColors

@Composable
fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(modifier = modifier) {
        if (url.isBlank()) {
            ImageFallback(title = contentDescription)
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                loading = { ImageFallback(title = contentDescription) },
                error = { ImageFallback(title = contentDescription) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ImageFallback(title: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        AppColors.SurfaceAlt,
                        AppColors.SurfaceRaised,
                        AppColors.Primary.copy(alpha = 0.10f)
                    )
                )
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title?.takeIf { it.isNotBlank() } ?: "ZYPlayer",
            color = AppColors.TextPrimary.copy(alpha = 0.78f),
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

package com.example.galleryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.galleryapp.data.BitmapLoader
import kotlin.math.max
import kotlin.math.min

class ViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onCreate(savedInstanceState)

        val id = intent.getLongExtra("id", -1L)
        val orientation = intent.getIntExtra("orientation", 0)

        setContent {
            MaterialTheme {
                ViewerScreen(
                    photoId = id,
                    orientation = orientation,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

@Composable
private fun ViewerScreen(
    photoId: Long,
    orientation: Int,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var bmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Load a higher res bitmap for the viewer
    LaunchedEffect(photoId) {
        if (photoId != -1L) {
            bmp = BitmapLoader.loadFull(
                cr = ctx.contentResolver,
                id = photoId,
                orientation = orientation,
                maxDim = 2048
            )
        }
    }

    // Container size in px
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // User controlled zoom & pan
    var userScale by remember { mutableStateOf(1f) }
    var userOffset by remember { mutableStateOf(Offset.Zero) }

    // Pinch and drag handler
    val tState = rememberTransformableState { zoomDelta, panDelta, _ ->
        userScale = (userScale * zoomDelta).coerceIn(1f, 6f)
        userOffset += panDelta
    }

    // Clamp offset so image never exposes blank space
    val clampedOffset by remember(bmp, containerSize, userScale, userOffset) {
        derivedStateOf {
            if (bmp == null || containerSize == IntSize.Zero) {
                Offset.Zero
            } else {
                val iw = bmp!!.width.toFloat()
                val ih = bmp!!.height.toFloat()
                val cw = containerSize.width.toFloat()
                val ch = containerSize.height.toFloat()

                // Base scale for base fit
                val baseScale = min(cw / iw, ch / ih)

                // Drawn dimensions after user scaling
                val drawnW = iw * baseScale * userScale
                val drawnH = ih * baseScale * userScale

                // Max translation before blank space would appear
                val maxX = max(0f, (drawnW - cw) / 2f)
                val maxY = max(0f, (drawnH - ch) / 2f)

                Offset(
                    x = userOffset.x.coerceIn(-maxX, maxX),
                    y = userOffset.y.coerceIn(-maxY, maxY)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        if (bmp == null) {
            CircularProgressIndicator()
        } else {
            Image(
                bitmap = bmp!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = userScale,
                        scaleY = userScale,
                        translationX = clampedOffset.x,
                        translationY = clampedOffset.y
                    )
                    .transformable(tState)
                    // Double-tap to toggle zoom/reset
                    .pointerInput(userScale) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (userScale > 1f) {
                                    userScale = 1f
                                    userOffset = Offset.Zero
                                } else {
                                    userScale = 2.5f
                                    userOffset = Offset.Zero
                                }
                            }
                        )
                    }
            )
        }

        // Back overlay positioned below the status bar
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
        BackHandler(onBack = onBack)
    }
}

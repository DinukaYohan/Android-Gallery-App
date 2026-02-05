package com.example.galleryapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.galleryapp.data.MediaStoreRepo
import com.example.galleryapp.data.PhotoRow
import com.example.galleryapp.ui.GalleryGrid
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GalleryApp() }
    }
}


//request the permissions, load photos from MediaStore when allowed and show the gallery grid
@Composable
private fun GalleryApp() {
    MaterialTheme {
        val ctx = LocalContext.current

        val permission = remember {
            if (Build.VERSION.SDK_INT >= 33)
                Manifest.permission.READ_MEDIA_IMAGES
            else
                Manifest.permission.READ_EXTERNAL_STORAGE
        }

        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(ctx, permission) ==
                        PackageManager.PERMISSION_GRANTED
            )
        }
        val requestPermission =
            rememberLauncherForActivityResult(RequestPermission()) { granted ->
                hasPermission = granted
            }

        var photos by remember { mutableStateOf<List<PhotoRow>>(emptyList()) }

        // Initial load when permission is granted (runs on a coroutine)
        LaunchedEffect(hasPermission) {
            if (hasPermission) {
                photos = MediaStoreRepo.queryAllPhotos(ctx)
            }
        }

        if (!hasPermission) {
            PermissionScreen { requestPermission.launch(permission) }
        } else {
            GalleryHome(
                photos = photos,
                onRefresh = {
                    // suspend: runs on IO inside repo
                    photos = MediaStoreRepo.queryAllPhotos(ctx)
                },
                onOpen = { row ->
                    ctx.startActivity(
                        Intent(ctx, ViewerActivity::class.java)
                            .putExtra("id", row.id)
                            .putExtra("orientation", row.orientation)
                    )
                }
            )
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Photos access required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "To show your gallery, the app needs read access to images on this device.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onRequest) { Text("Allow photos access") }
            }
        }
    }
}

//show the gallery with a top bar, reloads on resume, and lets you pinch anywhere
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryHome(
    photos: List<PhotoRow>,
    onRefresh: suspend () -> Unit,
    onOpen: (PhotoRow) -> Unit
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Reload when returning to foreground
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { onRefresh() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Pinch
    var columns by rememberSaveable { mutableStateOf(2) }
    var scaleAccum by remember { mutableStateOf(1f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        // accumulate scale and map to discrete column counts
        scaleAccum = (scaleAccum * zoomChange).coerceIn(0.6f, 3f)
        val mapped = (2f / scaleAccum).roundToInt().coerceIn(1, 6)
        if (mapped != columns) columns = mapped
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(transformState)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gallery") },
                    actions = {
                        IconButton(onClick = { scope.launch { onRefresh() } }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { padding ->
            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No photos found.")
                }
            } else {
                GalleryGrid(
                    photos = photos,
                    onOpen = onOpen,
                    columns = columns,
                    aspect = 4f / 3f,
                    spacing = 8.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}
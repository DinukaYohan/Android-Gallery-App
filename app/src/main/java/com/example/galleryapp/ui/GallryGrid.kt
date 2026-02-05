package com.example.galleryapp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.example.galleryapp.data.BitmapLoader
import com.example.galleryapp.data.PhotoRow


//Displays a scrollable grid of photo thumbnails
@Composable
fun GalleryGrid(
    photos: List<PhotoRow>,
    onOpen: (PhotoRow) -> Unit,
    columns: Int,
    aspect: Float,
    spacing: Dp,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items(photos, key = { it.id }) { row ->
            var bmp by remember(row.id) { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(row.id) {
                // Slightly larger target so rectangular tiles look sharp
                bmp = BitmapLoader.loadThumb(ctx.contentResolver, row, targetPx = 600)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .clickable { onOpen(row) },
                contentAlignment = Alignment.Center
            ) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

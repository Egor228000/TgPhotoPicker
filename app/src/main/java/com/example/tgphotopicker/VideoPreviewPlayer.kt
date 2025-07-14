package com.example.tgphotopicker

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

@Composable
fun VideoPreview(
    uri: Uri,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    context: Context
) {

    val mime = remember(uri) { context.contentResolver.getType(uri) }
    val isVideo = mime?.startsWith("video") == true

    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uri) {
        if (!isVideo) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(480, 480), null)
                } else {
                    val id = ContentUris.parseId(uri)
                    MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        id,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                }
            } catch (e: FileNotFoundException) {
                Log.w("VideoPreview", "Thumbnail not found for $uri", e)
                null
            } catch (e: SecurityException) {
                Log.e("VideoPreview", "No permission to read $uri", e)
                null
            } catch (e: Exception) {
                Log.e("VideoPreview", "Error loading thumbnail for $uri", e)
                null
            }
        }
    }

    Box(modifier = modifier) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Video preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (placeholder != null) {
            Image(
                painter = placeholder,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (bitmap != null || placeholder != null) {
            Icon(
                painter = painterResource(R.drawable.baseline_play_arrow_24),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }
    }
}


@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            this.playWhenReady = playWhenReady
            prepare()
        }
    }
    DisposableEffect(uri) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setShutterBackgroundColor(Color.Transparent.toArgb())
            }
        },
        modifier = modifier
    )
}

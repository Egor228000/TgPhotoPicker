package com.example.tgphotopicker

import android.R.attr.y
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ContentMain(
    context: Context,
    hasPermission: Boolean,
    scaffoldState: BottomSheetScaffoldState,
    selectedVisible: SnapshotStateList<Uri>,
    images: SnapshotStateList<Uri>,
    mediaPickerLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>>,
    iconVideoCircle: MutableState<Boolean>


) {
    var textField by remember { mutableStateOf("") }

    var iconToogle by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val progress = remember { Animatable(0f) }
    var isRecording by remember { mutableStateOf(false) }
    val cameraSelectorDefalt by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }

    var isPlaying by remember { mutableStateOf(false) }

    suspend fun startRecordAnimation() {

        if (iconVideoCircle.value) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 60000)
            )
            isRecording = false
        } else {
            isRecording = false
            progress.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
    ) {

        Image(
            painter = painterResource(R.drawable.d2bfd3ea45910c01255ae022181148c4),
            null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .blur(
                    if (iconVideoCircle.value) 10.dp else 0.dp
                )

                .fillMaxSize()

        )

        val brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF8A7EE3),
                Color(0xFFC953BD)
            ),
            start = Offset(2f, 2f),
            end = Offset(2f, y + 200f)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .blur(
                    if (iconVideoCircle.value) 10.dp else 0.dp
                )
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 64.dp)

        ) {
            items(selectedVisible) { img ->

                val aspectRatio by remember(img) {
                    mutableFloatStateOf(calculateAspectRatio(context, img) ?: 1f)
                }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        if (img.toString().endsWith(".mp4")) {
                            val exoPlayer = remember {
                                ExoPlayer.Builder(context)
                                    .build()
                                    .apply {
                                        setMediaItem(MediaItem.fromUri(img))
                                        prepare()
                                        this.playWhenReady = isPlaying
                                    }
                            }
                            val currentProgress = remember { mutableStateOf(0f) }

                            LaunchedEffect(exoPlayer) {
                                while (true) {
                                    val pos = exoPlayer.currentPosition.toFloat()
                                    val dur = (exoPlayer.duration.takeIf { it > 0 } ?: 1L).toFloat()
                                    currentProgress.value = pos / dur
                                    delay(1L)
                                }
                            }



                            Box(
                                modifier = Modifier
                                    .clickable(
                                        onClick = {
                                            isPlaying = !isPlaying
                                        }
                                    )
                                    .size(300.dp)
                                    .clip(CircleShape)

                            ) {
                                Column(
                                    modifier = Modifier
                                    .clickable {
                                    isPlaying = !isPlaying
                                }
                                    .zIndex(1f)
                                ) {


                                    LaunchedEffect(isPlaying) {
                                        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
                                    }

                                    // listen for end of playback
                                    DisposableEffect(exoPlayer) {
                                        val listener = object : Player.Listener {
                                            override fun onPlaybackStateChanged(state: Int) {
                                                if (state == Player.STATE_ENDED) {
                                                    // rewind and update UI
                                                    exoPlayer.seekTo(0)
                                                    isPlaying = false
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
                                                useController = false
                                                setShutterBackgroundColor(Color.Transparent.toArgb())
                                            }
                                        },
                                        modifier = Modifier
                                            .size(600.dp)
                                            .aspectRatio(0.1f),
                                    )

                                }
                                TouchControlledCircularProgressIndicator(
                                    modifier = Modifier
                                        .zIndex(2f)

                                        .matchParentSize(),
                                    progress = currentProgress.value,
                                    onProgressChange = { newProgress ->
                                        val duration = exoPlayer.duration
                                        if (duration > 0) {
                                            val seekPosition = (newProgress * duration).toLong()
                                            exoPlayer.seekTo(seekPosition)
                                        }
                                    }
                                )


                            }


                        } else if (isVideo(context, img)) {
                            VideoPlayer(
                                uri = img,
                                modifier = Modifier
                                    .fillMaxSize(0.7f)
                                    .aspectRatio(aspectRatio)
                                    .clip(RoundedCornerShape(10, 3, 3, 10))
                                    .background(brush)
                                    .padding(4.dp),
                                playWhenReady = true
                            )
                        } else {

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Card(
                                    shape = RoundedCornerShape(10, 3, 3, 10),
                                    modifier = Modifier
                                        .fillMaxSize(0.7f)
                                        .aspectRatio(aspectRatio)
                                        .clip(RoundedCornerShape(10, 3, 3, 10))
                                        .background(brush)
                                        .padding(4.dp)
                                ) {
                                    CoilImage(
                                        imageModel = { img },
                                        imageOptions = ImageOptions(
                                            contentScale = ContentScale.Crop,
                                            alignment = Alignment.Center
                                        ),
                                        modifier = Modifier


                                    )
                                }
                            }
                        }
                    }
            }
        }

    }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .blur(
                if (iconVideoCircle.value) 10.dp else 0.dp
            )
            .fillMaxHeight()
    ) {
        TextField(
            value = textField,
            onValueChange = { textField = it },
            modifier = Modifier

                .height(60.dp)
                .fillMaxWidth(1f),
            maxLines = 1,
            shape = RoundedCornerShape(0.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF202F41),
                unfocusedContainerColor = Color(0xFF202F41),
                unfocusedIndicatorColor = Color(0xFF202F41),
                focusedIndicatorColor = Color(0xFF202F41),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White

            ),
            enabled = false,
            textStyle = TextStyle(fontSize = 25.sp),
            placeholder = { Text("Сообщение", color = Color(0xFF707F92), fontSize = 20.sp) },
            leadingIcon = {

                IconButton(
                    onClick = {

                    },
                    modifier = Modifier
                        .graphicsLayer(
                            translationY = -35f
                        )

                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_emoji_language_24),
                        null,
                        modifier = Modifier

                            .size(30.dp),
                        tint = Color(0xFF707F92)
                    )
                }

            },
            trailingIcon = {
                AnimatedContent(
                    targetState = iconToogle,
                    transitionSpec = {
                        (
                                fadeIn(tween(300))).togetherWith(fadeOut(tween(300)))
                    },
                    label = "IconToggleAnimation"
                ) { state ->

                    Icon(
                        painter = painterResource(
                            if (state) R.drawable.outline_camera_24
                            else R.drawable.outline_mic_24
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        iconToogle = !iconToogle
                                    },
                                    onPress = {
                                        iconVideoCircle.value = true
                                        isRecording = true

                                        coroutineScope.launch {
                                            startRecordAnimation()
                                        }

                                        val released = try {
                                            tryAwaitRelease()
                                        } catch (e: CancellationException) {
                                            false
                                        }
                                        isRecording = false


                                    }
                                )
                            }
                            .graphicsLayer(translationY = -35f)
                            .size(30.dp),
                        tint = Color(0xFF707F92)
                    )
                }

            },
            suffix = {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (hasPermission) {
                                when {
                                    scaffoldState.bottomSheetState.isVisible ->
                                        scaffoldState.bottomSheetState.hide()

                                    scaffoldState.bottomSheetState.isVisible ->
                                        scaffoldState.bottomSheetState.expand()

                                    else ->
                                        scaffoldState.bottomSheetState.show()
                                }
                            } else {

                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(PickVisualMedia.ImageAndVideo)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .graphicsLayer(
                            translationY = -35f
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_attach_file_24),
                        null,
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer(rotationZ = -150f),
                        tint = Color(0xFF707F92)
                    )
                }
            }
        )
    }
    if (iconVideoCircle.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {

            CircularProgressIndicator(
                progress = { progress.value },
                color = Color.White,
                strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth,
                trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.Center)
            )

            CameraXCaptureScreen(
                onImageCaptured = { uri ->
                },
                onVideoCaptured = { uri ->
                    Log.d("CameraX", "Captured video URI: $uri")
                    if (!selectedVisible.contains(uri)) {
                        selectedVisible.add(uri)
                    }
                    iconVideoCircle.value = false
                },
                modifier = Modifier
                    .size(340.dp)
                    .padding(5.dp)

                    .clip(CircleShape)
                    .align(Alignment.Center),
                images,
                false,
                isRecording,
                false,
                cameraSelectorDefalt,
                context
            )

            Card(
                colors = CardDefaults.cardColors(Color(0xFF202F41)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        "0:18,8",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                    )
                    TextButton(
                        onClick = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    ) {
                        Text(
                            "ОТМЕНА",
                            color = Color.White,

                            )
                    }

                }

            }
            FloatingActionButton(
                onClick = {

                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .graphicsLayer(
                        translationY = 10f,
                        translationX = 30f
                    )
                    .size(100.dp),
                shape = CircleShape,
                containerColor = Color(0xFF199AF8)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_send_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(32.dp),
                )
            }
        }

    }
}

@Composable
fun TouchControlledCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    onProgressChange: (Float) -> Unit
) {
    var localProgress by remember { mutableStateOf(progress) }
    LaunchedEffect(progress) { localProgress = progress }

    val strokeWidth = 7.dp
    val paddingDp   = 16.dp
    val radiusDp    = 300.dp
    val diameterDp  = radiusDp * 2 + paddingDp * 2
    val angleOffset = Math.toRadians(90.0).toFloat()

    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    val padPx    = with(LocalDensity.current) { paddingDp.toPx() }

    var boxSizePx by remember { mutableStateOf(0f) }
    val effectiveRadiusPx = (boxSizePx / 2f) - padPx
    val innerRadiusPx     = effectiveRadiusPx - strokePx
    val outerRadiusPx     = effectiveRadiusPx

    Box(
        modifier
            .size(diameterDp)
            .onSizeChanged { boxSizePx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset((size.width/2).toFloat(), (size.height/2).toFloat())
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    val dist = sqrt(dx*dx + dy*dy)

                    if (dist in innerRadiusPx..outerRadiusPx) {
                        val angle      = atan2(dy, dx)
                        val normalized = ((angle + angleOffset + 2*PI) % (2*PI)).toFloat()
                        localProgress = normalized / (2*PI.toFloat())
                        onProgressChange(localProgress)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val center = Offset((size.width/2).toFloat(), (size.height/2).toFloat())
                    val dx = tap.x - center.x
                    val dy = tap.y - center.y
                    val dist = sqrt(dx*dx + dy*dy)

                    if (dist in innerRadiusPx..outerRadiusPx) {
                        val angle      = atan2(dy, dx)
                        val normalized = ((angle + angleOffset + 2*PI) % (2*PI)).toFloat()
                        localProgress = normalized / (2*PI.toFloat())
                        onProgressChange(localProgress)
                    }
                }
            }
            .padding(paddingDp)
    ) {
        CircularProgressIndicator(
            progress    = { localProgress },
            strokeWidth = strokeWidth,
            trackColor  = Color.Gray,
            color       = Color.White,
            modifier    = Modifier.fillMaxSize()
        )
    }
}

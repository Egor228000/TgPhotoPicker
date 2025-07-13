package com.example.tgphotopicker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.memory.MemoryCache.Builder
import coil.request.ImageRequest
import com.example.tgphotopicker.view.MainViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SheetContent(
    context: Context,
    mainViewModel: MainViewModel,
    bottomSheetState: SheetState,
    stateLazyVerticalGrid: LazyGridState,
    innerPadding: PaddingValues,
    cameraLauncher: ActivityResultLauncher<Intent>

) {
    val listMediaSheet by mainViewModel.listMediaSheet.collectAsStateWithLifecycle()
    val listMediaSheetSelected by mainViewModel.listMediaSheetSelected.collectAsStateWithLifecycle()
    val hasPermission by mainViewModel.hasPermission.collectAsStateWithLifecycle()
    val watchMedia by mainViewModel.watchMedia.collectAsStateWithLifecycle()
    val openCamera by mainViewModel.openCamera.collectAsStateWithLifecycle()
    var photoClick = remember { mutableStateOf(false) }


    val expanded by remember {
        derivedStateOf {
            bottomSheetState.currentValue == SheetValue.PartiallyExpanded || bottomSheetState.currentValue == SheetValue.Expanded


        }
    }
    LaunchedEffect(hasPermission, bottomSheetState) {
        mainViewModel.loadMedia(context)
    }


    val videoCount = listMediaSheetSelected.count { mainViewModel.isVideo(context, it) }
    val photoCount = listMediaSheetSelected.size - videoCount

    val isOnlyVideo = videoCount > 0 && photoCount == 0
    val isOnlyPhoto = photoCount > 0 && videoCount == 0
    val isMixed = videoCount > 0 && photoCount > 0


    Column {
        AnimatedVisibility(
            visible = listMediaSheetSelected.isNotEmpty(), enter = slideInVertically(
                initialOffsetY = { it }, animationSpec = tween(durationMillis = 300)
            ), exit = slideOutVertically(
                targetOffsetY = { it }, animationSpec = tween(durationMillis = 300)
            ), modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            val text = when {
                isMixed -> "Выбрано ${listMediaSheetSelected.size} медиафайл${
                    mainViewModel.pluralEnding(
                        listMediaSheetSelected.size
                    )
                }"

                isOnlyVideo -> "Выбрано ${listMediaSheetSelected.size} видео"
                isOnlyPhoto -> {
                    val verb = if (listMediaSheetSelected.size == 1) "Выбрана" else "Выбрано"
                    val noun = when {
                        listMediaSheetSelected.size % 10 == 1 && listMediaSheetSelected.size % 100 != 11 -> "фотография"
                        listMediaSheetSelected.size % 10 in 2..4 && listMediaSheetSelected.size % 100 !in 12..14 -> "фотографии"
                        else -> "фотографий"
                    }
                    "$verb ${listMediaSheetSelected.size} $noun"
                }

                else -> {
                    ""
                }
            }
            Text(
                text = text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        LazyVerticalGrid(
            state = stateLazyVerticalGrid,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
        ) {
            item {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp))
                        .background(Color.LightGray)
                        .clickable {},
                    contentAlignment = Alignment.Center,

                    ) {
                    if (expanded) {
                        CameraXCaptureScreen(
                            onImageCaptured = { uri ->
                            },
                            onVideoCaptured = {

                            },
                            modifier = Modifier,
                            mainViewModel,
                            true,
                            false,
                            photoClick,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            context,
                            onClick = {
                                cameraLauncher.launch(Intent(context, CameraActivity::class.java))

                            }
                        )
                    } else {

                    }

                }
            }
            itemsIndexed(listMediaSheet) { index, uri ->
                val cornerShape = when (index) {
                    1 -> RoundedCornerShape(topEnd = 10.dp)
                    else -> RoundedCornerShape(0.dp)
                }

                val isSelected = uri in listMediaSheetSelected
                val selectionIndex = if (isSelected) listMediaSheetSelected.indexOf(uri) + 1 else 0
                val isVideo = mainViewModel.isVideo(context, uri)

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f)
                )

                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(120.dp)
                ) {
                    if (isVideo) {
                        VideoPreview(
                            uri, modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clickable {
                                    mainViewModel.addWatchMedia(uri)
                                }, context = context
                        )
                    } else {
                        CoilImage(
                            imageOptions = ImageOptions(
                                contentScale = ContentScale.Crop, alignment = Alignment.Center
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clickable {
                                    mainViewModel.addWatchMedia(uri)
                                }
                                .clip(cornerShape),
                            imageRequest = {
                                ImageRequest.Builder(context).data(uri).crossfade(true).size(180)
                                    .bitmapConfig(Bitmap.Config.RGB_565).build()
                            },
                            imageLoader = {
                                ImageLoader.Builder(LocalContext.current).memoryCache {
                                    Builder(context).maxSizePercent(0.25).build()
                                } // ограничить доступную память
                                    .crossfade(true).build()
                            },
                        )

                    }

                    CircleCheckBox(
                        checked = isSelected,
                        onCheckedChange = { newValue ->
                            if (newValue) mainViewModel.addMediaSheetSelected(uri)
                            else mainViewModel.removeMediaSheetSelected(uri)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        countFiles = selectionIndex
                    )
                }
            }
        }
    }


    watchMedia?.let {
        Dialog(
            onDismissRequest = {
                mainViewModel.clearWatchMedia()
            }, properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler {
                mainViewModel.clearWatchMedia()
            }

            Box(
                modifier = Modifier.fillMaxSize()

            ) {
                watchMedia?.let { uri ->
                    val isVideo = mainViewModel.isVideo(context, uri)
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color.Black)
                            .fillMaxSize()
                    ) {
                        if (isVideo) {
                            VideoPlayer(
                                uri,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        } else {
                            ZoomableImage(
                                painter = uri,
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        mainViewModel.clearWatchMedia()
                    }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_close_24),
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OpenCamera(
    mainViewModel: MainViewModel,
    context: Context,
    cameraLauncher: ActivityResultLauncher<Intent>

) {
    val context = LocalContext.current

    var photoClick = remember { mutableStateOf(false) }
    Box {

        CameraXCaptureScreen(
            onImageCaptured = { uri ->
                mainViewModel.addWatchMedia(uri)
                Log.d("CameraX", "📸 Photo captured: $uri")

            },
            onVideoCaptured = {

            },
            modifier = Modifier.fillMaxSize(),
            mainViewModel,
            false,
            false,
            photoClick,
            CameraSelector.DEFAULT_BACK_CAMERA,
            context,
            onClick = {}
        )
        Button(
            onClick = {





            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            Text("asd")
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun ZoomableImage(
    painter: Uri,
    backgroundColor: Color = Color.Transparent,
    imageAlign: Alignment = Alignment.Center,
    maxScale: Float = 1f,
    minScale: Float = 3f,
    isRotation: Boolean = false,
    isZoomable: Boolean = true,
    scrollState: ScrollableState? = null,
) {
    val coroutineScope = rememberCoroutineScope()

    val scale = remember { mutableStateOf(1f) }
    val rotationState = remember { mutableStateOf(1f) }
    val offsetX = remember { mutableStateOf(1f) }
    val offsetY = remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .background(backgroundColor)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { },
                onDoubleClick = {
                    if (scale.value >= 2f) {
                        scale.value = 1f
                        offsetX.value = 1f
                        offsetY.value = 1f
                    } else scale.value = 3f
                },
            )
            .pointerInput(Unit) {
                if (isZoomable) {
                    forEachGesture {
                        awaitPointerEventScope {
                            awaitFirstDown()
                            do {
                                val event = awaitPointerEvent()
                                scale.value *= event.calculateZoom()
                                if (scale.value > 1) {
                                    scrollState?.run {
                                        coroutineScope.launch {
                                            setScrolling(false)
                                        }
                                    }
                                    val offset = event.calculatePan()
                                    offsetX.value += offset.x
                                    offsetY.value += offset.y
                                    rotationState.value += event.calculateRotation()
                                    scrollState?.run {
                                        coroutineScope.launch {
                                            setScrolling(true)
                                        }
                                    }
                                } else {
                                    scale.value = 1f
                                    offsetX.value = 1f
                                    offsetY.value = 1f
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                }
            }

    ) {

        CoilImage(
            imageModel = { painter }, imageOptions = ImageOptions(
                contentScale = ContentScale.Crop, alignment = Alignment.Center
            ), modifier = Modifier
                .align(imageAlign)
                .graphicsLayer {
                    if (isZoomable) {
                        scaleX = maxOf(maxScale, minOf(minScale, scale.value))
                        scaleY = maxOf(maxScale, minOf(minScale, scale.value))
                        if (isRotation) {
                            rotationZ = rotationState.value
                        }
                        translationX = offsetX.value
                        translationY = offsetY.value
                    }
                }
                .fillMaxWidth()
        )
    }


}

suspend fun ScrollableState.setScrolling(value: Boolean) {
    scroll(scrollPriority = MutatePriority.PreventUserInput) {
        when (value) {
            true -> Unit
            else -> awaitCancellation()
        }
    }
}
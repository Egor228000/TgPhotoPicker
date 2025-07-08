package com.example.tgphotopicker

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetContent(
    context: Context,
    images: SnapshotStateList<Uri>,
    selected: SnapshotStateList<Uri>,
    bottomSheetState: SheetState,
    stateLazyVerticalGrid: LazyGridState,
    innerPadding: PaddingValues,
    hasPermission: Boolean
) {
    var capturedUri by remember { mutableStateOf<Uri?>(null) }


    val expanded by remember {
        derivedStateOf {
            bottomSheetState.currentValue == SheetValue.PartiallyExpanded
                    ||
                    bottomSheetState.currentValue == SheetValue.Expanded


        }
    }
    LaunchedEffect(hasPermission, bottomSheetState) {
        loadMedia(context, images)
    }


    var openUri = remember { mutableStateListOf<Uri>() }
    val videoCount = selected.count { isVideo(context, it) }
    val photoCount = selected.size - videoCount

    val isOnlyVideo = videoCount > 0 && photoCount == 0
    val isOnlyPhoto = photoCount > 0 && videoCount == 0
    val isMixed = videoCount > 0 && photoCount > 0


    Column {
        AnimatedVisibility(
            visible = selected.isNotEmpty(),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            val text = when {
                isMixed -> "Выбрано ${selected.size} медиафайл${pluralEnding(selected.size)}"
                isOnlyVideo -> "Выбрано ${selected.size} видео"
                isOnlyPhoto -> {
                    val verb = if (selected.size == 1) "Выбрана" else "Выбрано"
                    val noun = when {
                        selected.size % 10 == 1 && selected.size % 100 != 11 -> "фотография"
                        selected.size % 10 in 2..4 && selected.size % 100 !in 12..14 -> "фотографии"
                        else -> "фотографий"
                    }
                    "$verb ${selected.size} $noun"
                }

                else -> {
                    ""
                }
            }
            Text(
                text = text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
                        .clickable { /* как-то обработать */ },
                    contentAlignment = Alignment.Center,

                    ) {
                    if (expanded) {
                        CameraXCaptureScreen(
                            onImageCaptured = { uri ->
                                capturedUri = uri
                            },
                            onVideoCaptured = {

                            },
                            modifier = Modifier,
                            images,
                            true,
                            false,
                            false,
                            CameraSelector.DEFAULT_BACK_CAMERA
                        )
                    } else {

                    }

                }
            }
            itemsIndexed(images) { index, uri ->
                val cornerShape = when (index) {
                    1 -> RoundedCornerShape(topEnd = 10.dp)
                    else -> RoundedCornerShape(0.dp)
                }

                val isSelected = uri in selected
                val selectionIndex = if (isSelected) selected.indexOf(uri) + 1 else 0
                val isVideo = isVideo(context, uri)

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
                            uri,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clickable { openUri.add(uri) }
                        )
                    } else {
                        CoilImage(
                            imageModel = { uri },
                            imageOptions = ImageOptions(
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clickable { openUri.add(uri) }
                                .clip(cornerShape)
                        )
                    }

                    CircleCheckBox(
                        checked = isSelected,
                        onCheckedChange = { newValue ->
                            if (newValue) selected.add(uri)
                            else selected.remove(uri)
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

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var constraints by remember { mutableStateOf(Constraints()) }

    val maxOffsetX = remember(scale, constraints.maxWidth) {
        if (scale <= 1f) 0f else (constraints.maxWidth * (scale - 1) / 2f)
    }
    val maxOffsetY = remember(scale, constraints.maxHeight) {
        if (scale <= 1f) 0f else (constraints.maxHeight * (scale - 1) / 2f)
    }

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        offset = Offset(
            x = (offset.x + panChange.x * scale).coerceIn(-maxOffsetX, maxOffsetX),
            y = (offset.y + panChange.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
        )
    }

    if (openUri.isNotEmpty()) {

        Dialog(
            onDismissRequest = { openUri.clear() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler {
                openUri.clear()
            }

            Box(
                modifier = Modifier
                    .onSizeChanged {
                        constraints = Constraints(
                            maxWidth = (it.width / density).toInt(),
                            maxHeight = (it.height / density).toInt()
                        )
                    }
                    .fillMaxSize()

            ) {
                openUri.forEach { uri ->
                    val isVideo = isVideo(context, uri)
                    Box {
                        if (isVideo) {
                            VideoPlayer(
                                uri,
                                modifier = Modifier.fillMaxSize()

                            )
                        } else {
                            CoilImage(
                                imageModel = { uri },
                                imageOptions = ImageOptions(
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (scale > 1f) {
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                } else {
                                                    scale = 2f
                                                }
                                            }
                                        )
                                    }
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                    }
                                    .transformable(state)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        openUri.clear()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_close_24),
                        null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(30.dp)
                    )
                }
            }
        }
    }
}
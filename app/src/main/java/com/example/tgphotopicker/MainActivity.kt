package com.example.tgphotopicker

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.tgphotopicker.ui.theme.TgPhotoPickerTheme
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TgPhotoPickerTheme {
                Main()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main() {
    val images = remember { mutableStateListOf<Uri>() }
    val selected = remember { mutableStateListOf<Uri>() }

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                    Manifest.permission.READ_MEDIA_VIDEO

                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE

                }
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                    Manifest.permission.READ_MEDIA_VIDEO

                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            )
        }
    }

    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )
    val coroutineScope = rememberCoroutineScope()


    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            SheetContent(
                context,
                images,
                selected,
                bottomSheetState
            )
        },
        sheetContainerColor = Color(0xFF212D3B),
        modifier = Modifier,
        sheetPeekHeight = 570.dp,
        sheetShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        sheetSwipeEnabled = true,
        topBar = {
            TopAppBar(
                title = { Text("Photo Picker", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(Color(0xFF202F41))

            )
        }
    ) {
        ContentMain(
            selected,
            context,
            coroutineScope,
            hasPermission,
            scaffoldState,
            permissionLauncher
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentMain(
    selected: SnapshotStateList<Uri>,
    context: Context,
    coroutineScope: CoroutineScope,
    hasPermission: Boolean,
    scaffoldState: BottomSheetScaffoldState,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    var textField by remember { mutableStateOf("") }

    Box {
        Image(
            painter = painterResource(R.drawable.d2bfd3ea45910c01255ae022181148c4),
            null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()

        )
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)

        ) {
            item {

                selected.forEach { img ->
                    if (isVideo(context, img)) {
                        VideoPlayer(
                            uri = img,
                            modifier = Modifier
                                .height(300.dp)
                                .fillMaxWidth(1f)
                        )
                    } else {
                        CoilImage(
                            imageModel = { img }, // loading a network image or local resource using an URL.
                            imageOptions = ImageOptions(
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center
                            ),
                            modifier = Modifier
                                .clickable(onClick = {

                                })
                                .height(300.dp)

                                .fillMaxWidth(1f)

                        )
                    }

                }
            }

        }

    }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxHeight()
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
                IconButton(
                    onClick = {

                    },
                    modifier = Modifier
                        .graphicsLayer(
                            translationY = -35f
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_mic_24),
                        null,
                        modifier = Modifier
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
                                permissionLauncher.launch(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        Manifest.permission.READ_MEDIA_IMAGES
                                        Manifest.permission.READ_MEDIA_VIDEO

                                    } else {
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    }
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
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetContent(
    context: Context,
    images: SnapshotStateList<Uri>,
    selected: SnapshotStateList<Uri>,
    bottomSheetState: SheetState,
) {
    var height by remember { mutableStateOf(0.dp) }
    var offsets by remember { mutableStateOf(0f) }
    LaunchedEffect(bottomSheetState) {
        loadMedia(context, images)
    }


    var textMessage by remember { mutableStateOf("") }


    when (selected.size) {
        0 -> {


        }

        1 -> {
            height = 0.dp
            textMessage = "Выбрана ${selected.size} фотография"
        }

        2 -> {
            height = 0.dp
            textMessage = "Выбраны  ${selected.size} фотографии"
        }

        else -> {

            textMessage = "Выбраны  ${selected.size} медиафайла"
        }
    }
    Column {
        Text(offsets.toString())
        if (selected.size > 0) {
            Text(
                textMessage,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.W900
            )

        } else {

        }

        Box(
        ) {

            LazyVerticalGrid(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                items(images) { uri ->
                    val isSelected = uri in selected
                    val isVideo = isVideo(context, uri)

                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(120.dp)
                    ) {
                        if (isVideo) {
                            VideoPreview(
                                uri
                            )
                        } else {


                            CoilImage(
                                imageModel = { uri },
                                imageOptions = ImageOptions(
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center
                                ),
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { newValue ->
                                if (newValue) selected.add(uri)
                                else selected.remove(uri)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        )
                    }
                }

            }


        }
    }
}

@Composable
fun VideoPreview(
    uri: Uri,
) {
    val context = LocalContext.current

    // Загружаем кадр из видео в фоне
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // API 29+: есть удобный метод loadThumbnail
                    context.contentResolver.loadThumbnail(uri, android.util.Size(480, 480), null)
                } else {
                    // Для старых устройств — через MediaStore Thumbnails
                    val id = uri.lastPathSegment?.toLongOrNull()
                    if (id != null) {
                        MediaStore.Video.Thumbnails.getThumbnail(
                            context.contentResolver,
                            id,
                            MediaStore.Video.Thumbnails.MINI_KIND,
                            null
                        )
                    } else null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Просто рендерим картинку — без контролов и без плеера
    bitmap?.let {
        Box() {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Video preview",
                modifier = Modifier
                    .size(200.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_play_arrow_24),
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

        }

    }
}

@Composable
fun VideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = false
) {
    val context = LocalContext.current

    // 1) Создаём ExoPlayer и настраиваем медиа
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                this.playWhenReady = playWhenReady
            }
    }

    // 2) Освобождаем плеер, когда Composable уходит из композиции
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // 3) Рендерим стандартный PlayerView
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true       // показывать контролы паузы/прокрутки
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
            }
        },
        modifier = modifier
    )
}

fun isVideo(context: Context, uri: Uri): Boolean {
    val type = context.contentResolver.getType(uri)
    return type?.startsWith("video") == true
}

private fun loadMedia(context: Context, mediaList: MutableList<Uri>) {
    val imageProjection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED)
    val videoProjection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DATE_ADDED)

    val imageUriList = mutableListOf<Pair<Uri, Long>>()
    val videoUriList = mutableListOf<Pair<Uri, Long>>()

    // Загрузка изображений
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        imageProjection,
        null,
        null,
        null
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val date = cursor.getLong(dateCol)
            val contentUri =
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            imageUriList.add(contentUri to date)
        }
    }

    // Загрузка видео
    context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        videoProjection,
        null,
        null,
        null
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val date = cursor.getLong(dateCol)
            val contentUri =
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            videoUriList.add(contentUri to date)
        }
    }

    // Объединяем, сортируем по дате и сохраняем в основной список
    mediaList.clear()
    (imageUriList + videoUriList)
        .sortedByDescending { it.second } // сортировка по дате (DESC)
        .mapTo(mediaList) { it.first }
}
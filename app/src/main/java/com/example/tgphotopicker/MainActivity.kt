package com.example.tgphotopicker

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            Main()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main() {
    var iconVideoCircle = remember { mutableStateOf(false) }

    val images = remember { mutableStateListOf<Uri>() }
    val selected = remember { mutableStateListOf<Uri>() }
    var selectedVisible = remember { mutableStateListOf<Uri>() }

    var stateLazyVerticalGrid = rememberLazyGridState()

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(false)
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            images.clear()
            images.addAll(uris)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.any { it }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val imagesGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED

            val videosGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED

            hasPermission = imagesGranted || videosGranted

            if (!imagesGranted || !videosGranted) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                )
            }
        } else {
            hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                )

            }
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

    val expanded by remember {
        derivedStateOf {
            scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded
                    || selected.isNotEmpty()

        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = expanded && selected.isNotEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                )
            ) {
                PanelSend(selected, selectedVisible, bottomSheetState)
            }

            AnimatedVisibility(
                visible = expanded && selected.isEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                )
            ) {
                PanelRow()
            }
        },
        modifier = Modifier
            .imePadding()
    ) { innerPadding ->
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
                Box {
                    SheetContent(
                        context,
                        images,
                        selected,
                        bottomSheetState,
                        stateLazyVerticalGrid,
                        innerPadding,
                        hasPermission
                    )
                }
            },
            sheetContainerColor = Color(0xFF212D3B),
            sheetPeekHeight = 500.dp,
            sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            sheetSwipeEnabled = true,
            topBar = {
                TopAppBar(
                    title = { Text("Photo Picker", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(Color(0xFF202F41)),
                    modifier = Modifier
                        .blur(
                            if (iconVideoCircle.value) 10.dp else 0.dp
                        )
                )
            }
        ) {
            ContentMain(
                context,
                coroutineScope,
                hasPermission,
                scaffoldState,
                selectedVisible,
                images,
                mediaPickerLauncher,
                iconVideoCircle
            )
        }
    }
}

@Composable
fun CameraXCaptureScreen(
    onImageCaptured: (Uri) -> Unit,
    onVideoCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    images: SnapshotStateList<Uri>,
    iconVisible: Boolean,
    isVideoRecording: Boolean,
    isPhotoCapture: Boolean,
    cameraSelector: CameraSelector
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermission = Manifest.permission.CAMERA
    val audioPermission = Manifest.permission.RECORD_AUDIO

    val hasCameraPermission = ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED
    val hasAudioPermission = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    val imageCapture = remember { ImageCapture.Builder().build() }

    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var isRecordingStarted by remember { mutableStateOf(isVideoRecording) }

    // CameraX binding
    LaunchedEffect(cameraProviderFuture, cameraSelector) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
        } catch (e: Exception) {
            Log.e("CameraX", "❌ Failed to bind camera use cases", e)
        }
    }

    // Photo capture
    LaunchedEffect(isPhotoCapture) {
        if (isPhotoCapture && hasCameraPermission) {
            val photoFile = File.createTempFile("IMG_", ".jpg", context.cacheDir)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val uri = output.savedUri ?: Uri.fromFile(photoFile)
                        images.add(uri)
                        onImageCaptured(uri)
                        Log.d("CameraX", "📸 Photo captured: $uri")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraX", "❌ Photo capture failed: ${exception.message}", exception)
                    }
                }
            )
        }
    }

    // Video recording
    LaunchedEffect(isVideoRecording) {
        if (isVideoRecording && activeRecording == null) {

            val timestamp = System.currentTimeMillis()
            val videoFile = File(context.cacheDir, "VID_$timestamp.mp4")

            val outputOptions = FileOutputOptions
                .Builder(videoFile)
                .build()
            activeRecording = videoCapture.output
                .prepareRecording(context, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            Log.d("CameraX", "▶️ Recording started")
                            isRecordingStarted = true
                        }
                        is VideoRecordEvent.Finalize -> {
                            isRecordingStarted = false
                            activeRecording = null

                            if (!event.hasError()) {
                                val fileUri = Uri.fromFile(videoFile)
                                onVideoCaptured(fileUri)
                                Log.d("CameraX", "✅ Video saved to: $fileUri")
                            } else {
                                Log.e("CameraX", "❌ Video error: ${event.error}", event.cause)
                            }
                        }
                    }
                }
        } else if (!isVideoRecording && activeRecording != null) {
            if (isRecordingStarted) {
                delay(100)
                activeRecording?.stop()
                Log.d("CameraX", "⏹️ Stopping video recording")
            } else {
                Log.w("CameraX", "⚠️ Tried to stop before recording started")
            }
            activeRecording = null
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
                if (isRecordingStarted) {
                    activeRecording?.stop()
                }
                activeRecording = null
                isRecordingStarted = false
                Log.d("CameraX", "📴 Camera released")
            } catch (e: Exception) {
                Log.e("CameraX", "❌ Error during camera release", e)
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    if (iconVisible) {
        Icon(
            painter = painterResource(
                if (isVideoRecording) R.drawable.baseline_camera_alt_24
                else R.drawable.outline_video_camera_front_off_24
            ),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp)
                .clickable {
                    if (!hasCameraPermission || !hasAudioPermission) {
                        permissionLauncher.launch(arrayOf(cameraPermission, audioPermission))
                    }
                }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelSend(
    selected: SnapshotStateList<Uri>,
    selectedVisible: SnapshotStateList<Uri>,
    bottomSheetState: SheetState
) {
    var textField by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        TextField(
            value = textField,
            onValueChange = { textField = it },
            modifier = Modifier
                .height(65.dp)
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
            placeholder = {
                Text(
                    "Добавить подпись...",
                    color = Color(0xFF707F92),
                    fontSize = 20.sp
                )
            },
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
        )
        FloatingActionButton(
            onClick = {
                scope.launch {
                    bottomSheetState.hide()
                    selectedVisible.addAll(selected)
                    selected.clear()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer(
                    translationY = -80f,
                    translationX = -30f
                )
                .size(64.dp),
            shape = CircleShape,
            containerColor = Color(0xFF199AF8)
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_send_24),
                contentDescription = null,
                tint = Color.White
            )
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-4).dp, y = 8.dp)
                .background(Color(0xFF199AF8), CircleShape)
                .border(2.dp, Color(0xFF202F41), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = selected.size.toString(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun PanelRow() {
    Card(
        modifier = Modifier
            .height(110.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(Color(0xFF202C3A)),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
        ) {
            val listIcon = listOf(
                R.drawable.outline_image_24,
                R.drawable.outline_file_copy_24,
                R.drawable.outline_location_on_24,
                R.drawable.baseline_person_24
            )
            val listColor = listOf(
                Color(0xFF4C94F4),
                Color(0xFF58BBF3),
                Color(0xFF60C256),
                Color(0xFFDAAD44)

            )
            val listName = listOf(
                "Галерея",
                "Файлы",
                "Геолокация",
                "Контакты"
            )

            listIcon.indices.forEach { index ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(listColor[index])
                            .size(50.dp)
                            .clickable {
                            }
                    ) {
                        Icon(
                            painter = painterResource(id = listIcon[index]),
                            contentDescription = listName[index],
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )

                    }
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = listName[index],
                        fontSize = 14.sp,
                        color = Color(0xFF75818F)
                    )
                }
            }
        }

    }
}


fun calculateAspectRatio(context: Context, imageUri: Uri): Float? {
    return try {
        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)

            val width = options.outWidth
            val height = options.outHeight

            if (width > 0 && height > 0) {
                width.toFloat() / height.toFloat()
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


@Composable
fun CircleCheckBox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    checkedColor: Color = Color(0xFF0A9FFA),
    uncheckedColor: Color = Color.White,
    borderWidth: Dp = 2.dp,
    countFiles: Int
) {

    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .border(
                width = borderWidth,
                color = if (checked) checkedColor else uncheckedColor,
                shape = CircleShape
            )
            .clickable { onCheckedChange?.invoke(!checked) }
            .background(
                if (checked) checkedColor
                else Color.Transparent
            )
    ) {
        if (checked) {
            Text(
                countFiles.toString(), color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

    }
}


fun isVideo(context: Context, uri: Uri): Boolean {
    val type = context.contentResolver.getType(uri)
    return type?.startsWith("video") == true
}

fun loadMedia(context: Context, mediaList: MutableList<Uri>) {
    val imageProjection =
        arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED)
    val videoProjection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DATE_ADDED)

    val imageUriList = mutableListOf<Pair<Uri, Long>>()
    val videoUriList = mutableListOf<Pair<Uri, Long>>()

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

    mediaList.clear()
    (imageUriList + videoUriList)
        .sortedByDescending { it.second }
        .mapTo(mediaList) { it.first }
}

fun pluralEnding(count: Int, one: String = "", few: String = "а", many: String = "ов"): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> one
        count % 10 in 2..4 && count % 100 !in 12..14 -> few
        else -> many
    }
}



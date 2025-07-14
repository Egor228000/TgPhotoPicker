package com.example.tgphotopicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tgphotopicker.view.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val mainViewModel: MainViewModel by viewModels()
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>("photoUri")
            uri?.let {
                mainViewModel.addWatchMedia(it)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val mediaPickerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickMultipleVisualMedia()
                ) { uris ->
                    if (!uris.isNullOrEmpty()) {
                        mainViewModel.clearMediaSheet()
                        mainViewModel.addMediaSheetFirst(uris)
                    }
                }
                Main(
                    context, mediaPickerLauncher, mainViewModel, cameraLauncher
                )
            }
        }
    }
}

enum class FlashMode {
    OFF, AUTO, ON
}

class CameraActivity : ComponentActivity() {
    val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MyApp_FullScreen)
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var photoClick = remember { mutableStateOf(false) }
            var videoClick = remember { mutableStateOf(false) }
            var iconFront by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            var currentIconIndex by remember { mutableStateOf(0) }
            val listFlashIcon = listOf(
                R.drawable.baseline_flash_off_24,
                R.drawable.baseline_flash_auto_24,
                R.drawable.baseline_flash_on_24


            )
            var currentFlashMode by remember { mutableStateOf(FlashMode.OFF) }

            val cameraSelector by mainViewModel.cameraSelector.collectAsStateWithLifecycle()

            val flashIcon = when (mainViewModel.flashMode.value) {
                MainViewModel.FlashMode.OFF -> R.drawable.baseline_flash_off_24
                MainViewModel.FlashMode.ON -> R.drawable.baseline_flash_on_24
                MainViewModel.FlashMode.AUTO -> R.drawable.baseline_flash_auto_24
            }
            var elapsedMillis by remember { mutableLongStateOf(0L) }
            val alphaAnim = remember { Animatable(0.3f) }

            LaunchedEffect(videoClick.value) {
                while (videoClick.value) {
                    delay(10L)
                    elapsedMillis += 10L
                }
            }
            LaunchedEffect(videoClick.value) {
                while (videoClick.value) {
                    alphaAnim.animateTo(1f, animationSpec = tween(600))
                    alphaAnim.animateTo(0.3f, animationSpec = tween(600))
                }
            }
            val totalSeconds = elapsedMillis / 1000
            val seconds = totalSeconds % 60
            val hundredths = (elapsedMillis % 1000) / 10

            val text = "%02d:%02d".format(seconds, hundredths)


            Box(
                modifier = Modifier
            ) {

                CameraXCaptureScreen(
                    onImageCaptured = { uri ->
                        val resultIntent = Intent().apply {
                            putExtra("photoUri", uri)
                        }
                        if (uri.toString().isNotEmpty()) {
                            scope.launch {
                                setResult(RESULT_OK, resultIntent)
                                finish()
                            }
                        }

                    },
                    onVideoCaptured = { uri ->

                        val resultIntent = Intent().apply {
                            putExtra("photoUri", uri)
                        }
                        Log.d("CameraX", "✅ Video saved to: $uri")

                        if (uri.toString().isNotEmpty()) {
                            scope.launch {
                                setResult(RESULT_OK, resultIntent)
                                delay(500)
                                finish()

                            }

                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    mainViewModel = mainViewModel,
                    iconVisible = false,
                    isVideoRecording = videoClick,
                    isPhotoCapture = photoClick,
                    cameraSelector = cameraSelector,
                    context = context,
                    onClick = {})

               if(videoClick.value) {

                   Box(
                       modifier = Modifier
                           .statusBarsPadding()
                           .padding(top = 64.dp)
                           .fillMaxWidth()
                           .align(Alignment.TopCenter),
                       contentAlignment = Alignment.Center
                   ) {

                       Box(
                           modifier = Modifier
                               .background(
                                   color = Color.Black.copy(alpha = 0.5f),
                                   shape = RoundedCornerShape(8.dp)
                               )
                               .padding(horizontal = 16.dp, vertical = 8.dp)
                       ) {
                           Row(
                               verticalAlignment = Alignment.CenterVertically
                           ) {
                               Box(
                                   modifier = Modifier
                                       .background(
                                           color = Color.Red.copy(alphaAnim.value),
                                           shape = RoundedCornerShape(50.dp)
                                       )
                                       .size(8.dp)
                               )
                               Spacer(Modifier.padding(horizontal = 4.dp))
                               Text(
                                   text = text,
                                   color = Color.White,
                                   fontSize = 17.sp
                               )
                           }

                       }
                   }
               }



                Box(
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .navigationBarsPadding()
                        .size(70.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Transparent)
                        .border(2.dp, Color.White, shape = RoundedCornerShape(50.dp))

                        .pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
                                    val down = awaitFirstDown()

                                    val longPressJob = scope.launch {
                                        delay(200)
                                        videoClick.value = true
                                    }

                                    val up = waitForUpOrCancellation()

                                    longPressJob.cancel()

                                    if (up != null) {
                                        val duration = up.uptimeMillis - down.uptimeMillis
                                        if (duration < 150) {
                                            photoClick.value = true
                                        } else {
                                            scope.launch {
                                                videoClick.value = false
                                            }
                                        }
                                    } else {
                                        longPressJob.cancel()
                                        videoClick.value = false
                                    }
                                }
                            }
                        }
                ) {
                    if (photoClick.value || videoClick.value) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(CircleShape)
                                .size(70.dp)
                                .background(if (videoClick.value) Color(0xFFCA4645) else Color.White)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        currentIconIndex = (currentIconIndex + 1) % listFlashIcon.size
                        currentFlashMode = when (currentFlashMode) {
                            FlashMode.OFF -> FlashMode.AUTO
                            FlashMode.AUTO -> FlashMode.ON
                            FlashMode.ON -> FlashMode.OFF
                        }
                        mainViewModel.toggleFlashMode()


                    },
                    modifier = Modifier
                        .padding(bottom = 32.dp, start = 32.dp)

                        .navigationBarsPadding()

                        .size(70.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Icon(
                        painter = painterResource(flashIcon), null, modifier = Modifier
                            .size(35.dp),
                        tint = Color.White
                    )

                }
                IconButton(
                    onClick = {
                        iconFront = !iconFront
                        mainViewModel.addCameraSelector(
                            if (iconFront) CameraSelector.DEFAULT_FRONT_CAMERA
                            else CameraSelector.DEFAULT_BACK_CAMERA
                        )

                    },
                    modifier = Modifier
                        .padding(bottom = 32.dp, end = 32.dp)

                        .navigationBarsPadding()

                        .size(70.dp)
                        .align(Alignment.BottomEnd)
                ) {

                    if (!iconFront) {
                        Icon(
                            painter = painterResource(R.drawable.outline_flip_camera_ios_24),
                            null,
                            modifier = Modifier
                                .size(35.dp),
                            tint = Color.White

                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.outline_photo_camera_front_24),
                            null,
                            modifier = Modifier
                                .size(40.dp),
                            tint = Color.White
                        )
                    }
                }
                Text(
                    "Нажмите для фото и удерживайте для видео",
                    color = Color.White,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                        .align(Alignment.BottomCenter),
                    fontSize = 17.sp
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(
    context: Context,
    mediaPickerLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>>,
    mainViewModel: MainViewModel,
    cameraLauncher: ActivityResultLauncher<Intent>
) {

    val listMediaSheetSelected by mainViewModel.listMediaSheetSelected.collectAsStateWithLifecycle()
    val recordingVideoCircle by mainViewModel.recordingVideoCircle.collectAsStateWithLifecycle()
    val watchMedia by mainViewModel.watchMedia.collectAsStateWithLifecycle()

    var stateLazyVerticalGrid = rememberLazyGridState()

    PermissionLaunch(
        context,
        mainViewModel,
    )
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden, skipHiddenState = false
    )

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )
    val expanded by remember {
        derivedStateOf {
            scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded || listMediaSheetSelected.isNotEmpty()

        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = expanded && listMediaSheetSelected.isNotEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it }, animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it }, animationSpec = tween(300)
                )
            ) {
                PanelSend(mainViewModel, bottomSheetState)
            }

            AnimatedVisibility(
                visible = expanded && listMediaSheetSelected.isEmpty(), enter = slideInVertically(
                    initialOffsetY = { it }, animationSpec = tween(300)
                ), exit = slideOutVertically(
                    targetOffsetY = { it }, animationSpec = tween(300)
                )
            ) {
                PanelRow()
            }
        }, modifier = Modifier.imePadding()
    ) { innerPadding ->
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
                Box {
                    SheetContent(
                        context,
                        mainViewModel,
                        bottomSheetState,
                        stateLazyVerticalGrid,
                        innerPadding,
                        cameraLauncher
                    )
                }
            },
            sheetContainerColor = if (watchMedia?.toString()
                    ?.isNotEmpty() == true
            ) Color.Black else Color(0xFF212D3B),
            sheetPeekHeight = 500.dp,
            sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            sheetSwipeEnabled = true,
            topBar = {
                TopAppBar(
                    title = { Text("Photo Picker", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        if (watchMedia?.toString()?.isNotEmpty() == true) Color.Black else Color(
                            0xFF212D3B
                        )
                    ),
                    modifier = Modifier.blur(
                        if (recordingVideoCircle) 10.dp else 0.dp
                    )
                )
            }) {
            ContentMain(
                context,
                scaffoldState,
                mainViewModel,
                mediaPickerLauncher,
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelSend(
    mainViewModel: MainViewModel, bottomSheetState: SheetState
) {
    val listMediaSheetSelected by mainViewModel.listMediaSheetSelected.collectAsStateWithLifecycle()


    var textField by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxWidth()
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
                    "Добавить подпись...", color = Color(0xFF707F92), fontSize = 20.sp
                )
            },
            leadingIcon = {
                IconButton(
                    onClick = {}, modifier = Modifier.graphicsLayer(
                        translationY = -35f
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_emoji_language_24),
                        null,
                        modifier = Modifier.size(30.dp),
                        tint = Color(0xFF707F92)
                    )
                }

            },
        )
        FloatingActionButton(
            onClick = {
                scope.launch {
                    bottomSheetState.hide()
                    mainViewModel.addMediaChat(listMediaSheetSelected)
                    mainViewModel.clearMediaSheetSelected()
                }
            }, modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer(
                    translationY = -80f, translationX = -30f
                )
                .size(64.dp), shape = CircleShape, containerColor = Color(0xFF199AF8)
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
                .border(2.dp, Color(0xFF202F41), CircleShape), contentAlignment = Alignment.Center
        ) {
            Text(
                text = listMediaSheetSelected.size.toString(),
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
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            val listIcon = listOf(
                R.drawable.outline_image_24,
                R.drawable.outline_file_copy_24,
                R.drawable.outline_location_on_24,
                R.drawable.baseline_person_24
            )
            val listColor = listOf(
                Color(0xFF4C94F4), Color(0xFF58BBF3), Color(0xFF60C256), Color(0xFFDAAD44)

            )
            val listName = listOf(
                "Галерея", "Файлы", "Геолокация", "Контакты"
            )

            listIcon.indices.forEach { index ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(listColor[index])
                            .size(50.dp)
                            .clickable {}) {
                        Icon(
                            painter = painterResource(id = listIcon[index]),
                            contentDescription = listName[index],
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )

                    }
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = listName[index], fontSize = 14.sp, color = Color(0xFF75818F)
                    )
                }
            }
        }

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
            )) {
        if (checked) {
            Text(
                countFiles.toString(),
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

    }
}




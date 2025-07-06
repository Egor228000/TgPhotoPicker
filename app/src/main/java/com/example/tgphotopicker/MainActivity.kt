package com.example.tgphotopicker

import android.Manifest
import android.R.attr.x
import android.R.attr.y
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            Main()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main() {
    val images = remember { mutableStateListOf<Uri>() }
    val selected = remember { mutableStateListOf<Uri>() }
    var selectedVisible = remember { mutableStateListOf<Uri>() }

    var stateLazyVerticalGrid = rememberLazyGridState()

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

    val expanded by remember {
        derivedStateOf {
            scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded
                    || selected.isNotEmpty()

        }
    }

    Scaffold(
        bottomBar = {
            // 1) Панель отправки — только когда лист развернут И есть selection
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
                Box(

                ) {
                    SheetContent(
                        context,
                        images,
                        selected,
                        bottomSheetState,
                        stateLazyVerticalGrid,
                        innerPadding
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
                    colors = TopAppBarDefaults.topAppBarColors(Color(0xFF202F41))

                )
            }
        ) {
            ContentMain(
                context,
                coroutineScope,
                hasPermission,
                scaffoldState,
                permissionLauncher,
                selectedVisible
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentMain(
    context: Context,
    coroutineScope: CoroutineScope,
    hasPermission: Boolean,
    scaffoldState: BottomSheetScaffoldState,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    selectedVisible: SnapshotStateList<Uri>
) {
    var textField by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
    ) {
        Image(
            painter = painterResource(R.drawable.d2bfd3ea45910c01255ae022181148c4),
            null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()

        )
        val brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF8A7EE3),
                Color(0xFFC953BD)
            ),
            start = Offset(2f, 2f),
            end = Offset(2f, y + 200f)  // например, градиент по вертикали на 200px
        )

        LazyColumn(
            modifier = Modifier
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 64.dp)

        ) {
            items(selectedVisible) { img ->

                val aspectRatio by remember(img) {
                    mutableFloatStateOf(calculateAspectRatio(context, img) ?: 1f)
                }
                if (isVideo(context, img)) {
                    VideoPlayer(
                        uri = img,
                        modifier = Modifier
                            .height(400.dp)
                            .width(180.dp)
                            .aspectRatio(aspectRatio)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetContent(
    context: Context,
    images: SnapshotStateList<Uri>,
    selected: SnapshotStateList<Uri>,
    bottomSheetState: SheetState,
    stateLazyVerticalGrid: LazyGridState,
    innerPadding: PaddingValues
) {
    var openUri = remember { mutableStateListOf<Uri>() }


    val videoCount = selected.count { isVideo(context, it) }
    val photoCount = selected.size - videoCount

    val isOnlyVideo = videoCount > 0 && photoCount == 0
    val isOnlyPhoto = photoCount > 0 && videoCount == 0
    val isMixed = videoCount > 0 && photoCount > 0
    LaunchedEffect(bottomSheetState) {
        loadMedia(context, images)
    }

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
            itemsIndexed(images) { index, uri ->
                val cornerShape = when (index) {
                    0 -> RoundedCornerShape(topStart = 10.dp)
                    2 -> RoundedCornerShape(topEnd = 10.dp)
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

@Composable
fun VideoPreview(
    uri: Uri,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null
) {
    val context = LocalContext.current

    // Проверяем, что это точно видео
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
                    // Правильный способ получить ID
                    val id = ContentUris.parseId(uri)
                    MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        id,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                }
            } catch (e: FileNotFoundException) {
                // эскиз не найден — пропускаем
                Log.w("VideoPreview", "Thumbnail not found for $uri", e)
                null
            } catch (e: SecurityException) {
                // нет разрешения
                Log.e("VideoPreview", "No permission to read $uri", e)
                null
            } catch (e: Exception) {
                // что‑то ещё пошло не так
                Log.e("VideoPreview", "Error loading thumbnail for $uri", e)
                null
            }
        }
    }

    Box(modifier = modifier) {
        if (bitmap != null) {
            // показываем эскиз
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Video preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (placeholder != null) {
            // или плейсхолдер, если передали
            Image(
                painter = placeholder,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (bitmap != null || placeholder != null) {
            // кнопка Play поверх картинки/плейсхолдера
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


@Composable
fun VideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = false
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                this.playWhenReady = playWhenReady
            }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
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



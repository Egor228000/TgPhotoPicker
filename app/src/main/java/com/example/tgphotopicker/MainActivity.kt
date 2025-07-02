package com.example.tgphotopicker

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.tgphotopicker.ui.theme.TgPhotoPickerTheme
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.launch

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

    val imagesOnClick = remember { mutableStateListOf<Uri>() }


    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
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


    // Автоматически запрашиваем разрешение при старте
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            )
        }
    }



    var textField by remember { mutableStateOf("") }
    // Configure the bottom sheet to support hiding completely
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false // Allow hiding completely
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )
    val coroutineScope = rememberCoroutineScope()

    var checkBox by remember { mutableStateOf(false) }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {

            LaunchedEffect(Unit) {
                loadImages(context, images)

            }

            Box {

                LazyVerticalGrid(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(images) { uri ->
                        Box {
                            CoilImage(
                                imageModel = { uri }, // loading a network image or local resource using an URL.
                                imageOptions = ImageOptions(
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center
                                ),
                                modifier = Modifier
                                    .clickable(onClick = {
                                        imagesOnClick.add(uri)
                                        coroutineScope.launch {
                                            bottomSheetState.hide()

                                        }
                                    })
                                    .width(150.dp).height(120.dp),
                            )
                            Checkbox(
                                checkBox,
                                onCheckedChange = {checkBox = it},
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                            )
                        }

                    }
                }


            }
            /*Column {
                Card(
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth(1f),
                    colors = CardDefaults.cardColors(Color(0xFF212D3B))
                ) {

                }
            }*/
        },
        sheetContainerColor = Color(0xFF212D3B),
        modifier = Modifier,
        sheetPeekHeight = 400.dp,
        sheetShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        sheetSwipeEnabled = true,
        topBar = {
            TopAppBar(
                title = { Text("Photo Picker", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(Color(0xFF202F41))

            )
        }
    ) {
        Box {
            Image(
                painter = painterResource(R.drawable.d2bfd3ea45910c01255ae022181148c4),
                null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()

            )
            Column(
                modifier = Modifier
                    .padding(16.dp)

            ) {
                imagesOnClick.forEach { img ->
                    CoilImage(
                        imageModel = {img}, // loading a network image or local resource using an URL.
                        imageOptions = ImageOptions(
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center
                        ),
                        modifier = Modifier
                            .clickable(onClick = {

                            })
                            .width(150.dp).height(120.dp),
                    )
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
}




private fun loadImages(context: Context, images: MutableList<Uri>) {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection, null, null, sortOrder
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        images.clear()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
            )
            Log.d("Gallery", "Found image URI: $contentUri")
            images.add(contentUri)
        }
    } ?: Log.e("Gallery", "Query returned null cursor")
}
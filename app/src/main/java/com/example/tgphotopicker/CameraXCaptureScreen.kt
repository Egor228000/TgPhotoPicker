package com.example.tgphotopicker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun CameraXCaptureScreen(
    onImageCaptured: (Uri) -> Unit,
    onVideoCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    images: SnapshotStateList<Uri>,
    iconVisible: Boolean,
    isVideoRecording: Boolean,
    isPhotoCapture: Boolean,
    cameraSelector: CameraSelector,
    context: Context
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermission = Manifest.permission.CAMERA
    val audioPermission = Manifest.permission.RECORD_AUDIO

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        cameraPermission
    ) == PackageManager.PERMISSION_GRANTED
    val hasAudioPermission = ContextCompat.checkSelfPermission(
        context,
        audioPermission
    ) == PackageManager.PERMISSION_GRANTED

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
                if (isVideoRecording || hasAudioPermission) R.drawable.baseline_camera_alt_24
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
package com.example.tgphotopicker.view

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel() : ViewModel() {

    private val _listMediaSheet = MutableStateFlow<List<Uri>>(emptyList())
    val listMediaSheet: StateFlow<List<Uri>>  = _listMediaSheet
    fun addMediaSheet(uri: Uri) {
        _listMediaSheet.value += uri
    }
    fun addMediaSheetFirst(uri: List<Uri>) {
        _listMediaSheet.value += uri
    }
    fun clearMediaSheet() {
        _listMediaSheet.value = emptyList()
    }

    private val _listMediaSheetSelected = MutableStateFlow<List<Uri>>(emptyList())
    val listMediaSheetSelected: StateFlow<List<Uri>>  = _listMediaSheetSelected
    fun addMediaSheetSelected(uri: Uri) {
        _listMediaSheetSelected.value += uri
    }
    fun clearMediaSheetSelected() {
        _listMediaSheetSelected.value = emptyList()
    }
    fun removeMediaSheetSelected(uri: Uri) {
        _listMediaSheetSelected.value -= uri
    }


    private val _listMediaChat = MutableStateFlow<List<Uri>>(emptyList())
    val listMediaChat: StateFlow<List<Uri>>  = _listMediaChat
    fun addMediaChat(uri: List<Uri>) {
        _listMediaChat.value += uri
    }
    fun addMediaChatFirst(uri: Uri) {
        _listMediaChat.value += uri
    }
    fun clearMediaChat() {
        _listMediaChat.value = emptyList()
    }

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission
    fun addHasPermission(bool: Boolean) {
        _hasPermission.value = bool
    }

    private val _recordingVideoCircle = MutableStateFlow(false)
    val recordingVideoCircle: StateFlow<Boolean> = _recordingVideoCircle
    fun addRecordingVideoCircle(bool: Boolean) {
        _recordingVideoCircle.value = bool
    }

    private val _watchMedia = MutableStateFlow<Uri?>(null)
    val watchMedia: StateFlow<Uri?> = _watchMedia
    fun addWatchMedia(uri: Uri) {
        _watchMedia.value = uri
    }
    fun clearWatchMedia() {
        _watchMedia.value = null
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

    fun isVideo(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri)
        return type?.startsWith("video") == true
    }



    fun pluralEnding(count: Int, one: String = "", few: String = "а", many: String = "ов"): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> one
            count % 10 in 2..4 && count % 100 !in 12..14 -> few
            else -> many
        }
    }






    fun loadMedia(context: Context) {
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


        _listMediaSheet.value = emptyList()

        _listMediaSheet.value = (imageUriList + videoUriList)
            .sortedByDescending { it.second }
            .map { it.first }

    }
}
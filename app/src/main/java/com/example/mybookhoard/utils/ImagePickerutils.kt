package com.example.mybookhoard.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilities for handling image selection from camera and gallery
 */
object ImagePickerUtils {

    /**
     * Get required permissions based on Android version
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasPermissions(context: Context): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Create a temporary file for camera capture
     */
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.cacheDir
        return File.createTempFile(
            "BOOK_COVER_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    /**
     * Get URI for camera capture using FileProvider
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Compress and prepare image for upload
     */
    fun prepareImageForUpload(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.readBytes()
        } catch (e: Exception) {
            android.util.Log.e("ImagePickerUtils", "Error reading image", e)
            null
        }
    }
}

/**
 * Composable state holder for image picker
 */
@Composable
fun rememberImagePickerState(
    context: Context,
    onImageSelected: (Uri) -> Unit
): ImagePickerState {
    val state = remember { ImagePickerState(context, onImageSelected) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        state.onPermissionsResult(permissions)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { state.onImageSelected(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            state.currentCameraUri?.let { state.onImageSelected(it) }
        }
    }

    DisposableEffect(Unit) {
        state.permissionLauncher = permissionLauncher
        state.galleryLauncher = galleryLauncher
        state.cameraLauncher = cameraLauncher
        onDispose { }
    }

    return state
}

/**
 * State class for managing image picker operations
 */
class ImagePickerState(
    private val context: Context,
    private val onImageSelectedCallback: (Uri) -> Unit
) {
    var permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>? = null
    var galleryLauncher: ManagedActivityResultLauncher<String, Uri?>? = null
    var cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>? = null

    var currentCameraUri: Uri? by mutableStateOf(null)
        private set

    var showPermissionDialog by mutableStateOf(false)
        private set

    fun onImageSelected(uri: Uri) {
        onImageSelectedCallback(uri)
    }

    fun launchGallery() {
        if (ImagePickerUtils.hasPermissions(context)) {
            galleryLauncher?.launch("image/*")
        } else {
            requestPermissions()
        }
    }

    fun launchCamera() {
        if (ImagePickerUtils.hasPermissions(context)) {
            try {
                val photoFile = ImagePickerUtils.createImageFile(context)
                currentCameraUri = ImagePickerUtils.getUriForFile(context, photoFile)
                cameraLauncher?.launch(currentCameraUri!!)
            } catch (e: Exception) {
                android.util.Log.e("ImagePickerState", "Error launching camera", e)
            }
        } else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        permissionLauncher?.launch(ImagePickerUtils.getRequiredPermissions())
    }

    fun onPermissionsResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            showPermissionDialog = true
        }
    }

    fun dismissPermissionDialog() {
        showPermissionDialog = false
    }
}
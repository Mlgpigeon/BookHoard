package com.example.mybookhoard.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
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
     * Create destination URI for cropped image
     */
    fun createCroppedImageUri(context: Context): Uri {
        val file = createImageFile(context)
        return getUriForFile(context, file)
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
 * Composable state holder for image picker with cropper
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
        uri?.let { state.onImagePickedFromSource(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            state.currentCameraUri?.let { state.onImagePickedFromSource(it) }
        }
    }

    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        state.handleCropResult(result)
    }

    LaunchedEffect(permissionLauncher, galleryLauncher, cameraLauncher, cropLauncher) {
        state.permissionLauncher = permissionLauncher
        state.galleryLauncher = galleryLauncher
        state.cameraLauncher = cameraLauncher
        state.cropLauncher = cropLauncher
    }

    return state
}

/**
 * State class for managing image picker operations with cropping
 */
class ImagePickerState(
    private val context: Context,
    private val onImageSelectedCallback: (Uri) -> Unit
) {
    var permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>? = null
    var galleryLauncher: ManagedActivityResultLauncher<String, Uri?>? = null
    var cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>? = null
    var cropLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null

    var currentCameraUri: Uri? by mutableStateOf(null)
        private set

    var showPermissionDialog by mutableStateOf(false)
        private set

    /**
     * Called when image is picked from camera or gallery
     */
    fun onImagePickedFromSource(sourceUri: Uri) {
        try {
            val destinationUri = ImagePickerUtils.createCroppedImageUri(context)

            val options = UCrop.Options().apply {
                // Quality and compression
                setCompressionQuality(90)
                setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG)

                // UI customization
                setHideBottomControls(false)
                setFreeStyleCropEnabled(true)
                setShowCropFrame(true)
                setShowCropGrid(true)

                // Color customization - Dark theme to avoid status bar issues
                setToolbarColor(android.graphics.Color.parseColor("#000000"))
                setStatusBarColor(android.graphics.Color.parseColor("#000000"))
                setToolbarWidgetColor(android.graphics.Color.WHITE)
                setActiveControlsWidgetColor(android.graphics.Color.parseColor("#6200EE"))
                setRootViewBackgroundColor(android.graphics.Color.parseColor("#000000"))

                // Allow gestures
                setAllowedGestures(
                    UCropActivity.SCALE, // Scale on first tab
                    UCropActivity.ROTATE, // Rotate on second tab
                    UCropActivity.ALL // All gestures on third tab
                )
            }

            val uCropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(2f, 3f) // Book cover ratio
                .withMaxResultSize(2000, 3000) // Max resolution
                .withOptions(options)
                .getIntent(context)

            cropLauncher?.launch(uCropIntent)
        } catch (e: Exception) {
            android.util.Log.e("ImagePickerState", "Error launching crop", e)
        }
    }
    /**
     * Handle crop activity result
     */
    fun handleCropResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { data ->
                    val resultUri = UCrop.getOutput(data)
                    resultUri?.let { onImageSelectedCallback(it) }
                }
            }
            UCrop.RESULT_ERROR -> {
                result.data?.let { data ->
                    val cropError = UCrop.getError(data)
                    android.util.Log.e("ImagePickerState", "Crop error", cropError)
                }
            }
        }
    }

    fun launchGallery() {
        if (ImagePickerUtils.hasPermissions(context)) {
            try {
                galleryLauncher?.launch("image/*")
            } catch (e: Exception) {
                android.util.Log.e("ImagePickerState", "Error launching gallery", e)
            }
        } else {
            requestPermissions()
        }
    }

    fun launchCamera() {
        if (ImagePickerUtils.hasPermissions(context)) {
            try {
                val photoFile = ImagePickerUtils.createImageFile(context)
                currentCameraUri = ImagePickerUtils.getUriForFile(context, photoFile)

                if (cameraLauncher != null) {
                    cameraLauncher?.launch(currentCameraUri!!)
                } else {
                    android.util.Log.e("ImagePickerState", "Camera launcher not initialized")
                }
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
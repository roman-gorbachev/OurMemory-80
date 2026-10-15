package com.gorman.ourmemoryapp.ui.scan.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorman.ourmemoryapp.R
import com.gorman.ourmemoryapp.ui.common.models.VeteranLink
import com.gorman.ourmemoryapp.ui.common.ui.CircleIconButton
import com.gorman.ourmemoryapp.ui.common.ui.VeteranItem
import com.gorman.ourmemoryapp.ui.scan.models.InscriptionStatus
import com.gorman.ourmemoryapp.ui.scan.models.ScannerUiIntent
import com.gorman.ourmemoryapp.ui.scan.models.ScannerUiState
import com.gorman.ourmemoryapp.ui.scan.viewmodels.ScannerViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    onBackClick: () -> Unit,
    onVeteranOpen: (String) -> Unit,
    scannerViewModel: ScannerViewModel = hiltViewModel()
) {
    val state by scannerViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    var lastQrValue by remember { mutableStateOf<String?>(null) }

    ScannerContent(
        state = state,
        hasCameraPermission = hasCameraPermission,
        onBackClick = onBackClick,
        onQrScanned = { rawValue ->
            if (rawValue != lastQrValue) {
                lastQrValue = rawValue
                VeteranLink.parseVeteranId(rawValue)?.let(onVeteranOpen)
                    ?: Toast.makeText(context, R.string.not_our_qr_code_msg, Toast.LENGTH_LONG).show()
            }
        },
        onPhotoTaken = { scannerViewModel.onUiIntent(ScannerUiIntent.OnPhotoTaken(it)) },
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onVeteranOpen = onVeteranOpen
    )
}

@Composable
private fun ScannerContent(
    state: ScannerUiState,
    hasCameraPermission: Boolean,
    onBackClick: () -> Unit,
    onQrScanned: (String) -> Unit,
    onPhotoTaken: (ByteArray) -> Unit,
    onRequestPermission: () -> Unit,
    onVeteranOpen: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            val controller = rememberScannerCamera(onQrScanned)
            val captureExecutor = rememberCaptureExecutor()
            AndroidView(
                factory = { PreviewView(it).apply { this.controller = controller } },
                modifier = Modifier.fillMaxSize()
            )
            ScannerPanel(
                state = state,
                onPhotoClick = { controller.capturePhoto(captureExecutor, onPhotoTaken) },
                onVeteranOpen = onVeteranOpen,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            CameraPermissionContent(
                onRequestPermission = onRequestPermission,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        CircleIconButton(
            painter = painterResource(R.drawable.chevron_left),
            contentDescription = stringResource(R.string.back),
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
        )
    }
}

@Composable
private fun ScannerPanel(
    state: ScannerUiState,
    onPhotoClick: () -> Unit,
    onVeteranOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                when (state.status) {
                    InscriptionStatus.IDLE -> PanelMessage(stringResource(R.string.point_camera_at_qr_or_name_msg))
                    InscriptionStatus.RECOGNIZING -> RecognizingMessage()
                    InscriptionStatus.FOUND -> {
                        Text(
                            text = stringResource(R.string.matching_veterans),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.candidates.forEach { veteran ->
                            VeteranItem(
                                veteran = veteran,
                                onClick = { onVeteranOpen(veteran.id) },
                                baseInfoMaxLines = CANDIDATE_INFO_MAX_LINES
                            )
                        }
                    }
                    InscriptionStatus.NOT_FOUND -> PanelMessage(stringResource(R.string.no_matching_veterans_msg))
                    InscriptionStatus.FAILED -> PanelMessage(
                        text = stringResource(R.string.failed_to_recognize_name_msg),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        Button(
            onClick = onPhotoClick,
            enabled = !state.isRecognizing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.photograph_headstone))
        }
    }
}

@Composable
private fun PanelMessage(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RecognizingMessage() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.recognizing_name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CameraPermissionContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(32.dp)
    ) {
        Text(
            text = stringResource(R.string.camera_permission_needed_msg),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.allow_camera_access))
        }
    }
}

@Composable
private fun rememberScannerCamera(onQrScanned: (String) -> Unit): LifecycleCameraController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnQrScanned by rememberUpdatedState(onQrScanned)
    val controller = remember { LifecycleCameraController(context) }
    DisposableEffect(controller, lifecycleOwner) {
        val analyzer = QrCodeAnalyzer { currentOnQrScanned(it) }
        controller.setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
        controller.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            analyzer.close()
        }
    }
    return controller
}

@Composable
private fun rememberCaptureExecutor(): ExecutorService {
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(executor) {
        onDispose { executor.shutdown() }
    }
    return executor
}

private fun LifecycleCameraController.capturePhoto(executor: ExecutorService, onPhotoTaken: (ByteArray) -> Unit) {
    takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                onPhotoTaken(image.use { it.toScaledJpeg() })
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(LOG_TAG, "Failed to take a photo", exception)
            }
        }
    )
}

private fun Context.hasCameraPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private const val CANDIDATE_INFO_MAX_LINES = 2
private const val LOG_TAG = "ScannerScreen"

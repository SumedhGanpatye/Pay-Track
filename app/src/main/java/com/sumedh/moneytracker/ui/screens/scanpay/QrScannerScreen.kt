package com.sumedh.moneytracker.ui.screens.scanpay

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sumedh.moneytracker.ui.screens.scanpay.components.BottomControls
import com.sumedh.moneytracker.ui.screens.scanpay.components.CameraPreview
import com.sumedh.moneytracker.ui.screens.scanpay.components.ReadingQrOverlay
import com.sumedh.moneytracker.ui.screens.scanpay.components.ScannerAppBar
import com.sumedh.moneytracker.ui.screens.scanpay.components.ScannerOverlay
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onNavigateToPaymentDetails: () -> Unit,
    viewModel: QrScannerViewModel = viewModel(factory = QrScannerViewModel.factory())
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val raw = decodeQrFromGallery(context, uri)
            if (raw.isNullOrBlank()) {
                snackbarHostState.showSnackbar(
                    "Invalid UPI QR\nPlease scan a valid payment QR code."
                )
            } else {
                viewModel.onRawQrDetected(raw)
            }
        }
    }

    // Reset scan gates whenever this destination becomes visible again.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.onScreenVisible()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.onScreenVisible()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(granted)
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                QrScannerEvent.NavigateToPaymentDetails -> onNavigateToPaymentDetails()
                is QrScannerEvent.ShowInvalidQr -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.cameraPermissionGranted) {
                key(uiState.cameraSession) {
                    CameraPreview(
                        enabled = uiState.scanningEnabled,
                        torchEnabled = uiState.torchEnabled,
                        cameraSession = uiState.cameraSession,
                        onQrDetected = viewModel::onRawQrDetected,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ScannerOverlay(
                    animateLaser = uiState.phase == ScanPhase.Idle && uiState.scanningEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                PermissionDeniedContent(
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ScannerAppBar(onBack = onBack)
            }

            if (uiState.cameraPermissionGranted) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Align the QR code inside the frame",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "It will be detected automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    BottomControls(
                        torchEnabled = uiState.torchEnabled,
                        enabled = uiState.phase == ScanPhase.Idle && uiState.scanningEnabled,
                        onFlashClick = viewModel::toggleTorch,
                        onGalleryClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    )
                }
            }

            ReadingQrOverlay(visible = uiState.phase == ScanPhase.Reading)
        }
    }
}

@Composable
private fun PermissionDeniedContent(onRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Camera access needed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "Allow camera permission to scan UPI QR codes securely inside Pay&Track.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonTeal,
                    contentColor = Color.Black
                )
            ) {
                Text("Enable camera", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private suspend fun decodeQrFromGallery(
    context: android.content.Context,
    uri: Uri
): String? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = false
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        try {
            scanner.process(image).awaitBarcode()
        } finally {
            scanner.close()
        }
    } catch (_: Exception) {
        null
    }
}

private suspend fun com.google.android.gms.tasks.Task<List<Barcode>>.awaitBarcode(): String? =
    suspendCoroutine { cont ->
        addOnSuccessListener { barcodes ->
            cont.resume(barcodes.firstOrNull()?.rawValue)
        }
        addOnFailureListener {
            cont.resume(null)
        }
    }

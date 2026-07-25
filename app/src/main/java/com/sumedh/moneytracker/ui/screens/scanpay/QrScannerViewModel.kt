package com.sumedh.moneytracker.ui.screens.scanpay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sumedh.moneytracker.domain.upi.PendingUpiScan
import com.sumedh.moneytracker.domain.upi.UpiDebugLog
import com.sumedh.moneytracker.domain.upi.UpiQrParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanPhase {
    Idle,
    Reading,
    ReadyToNavigate
}

data class QrScannerUiState(
    val torchEnabled: Boolean = false,
    val scanningEnabled: Boolean = true,
    val phase: ScanPhase = ScanPhase.Idle,
    val cameraPermissionGranted: Boolean = false,
    /** Bumped whenever the screen becomes visible so CameraPreview can remount cleanly. */
    val cameraSession: Int = 0
)

sealed interface QrScannerEvent {
    data object NavigateToPaymentDetails : QrScannerEvent
    data class ShowInvalidQr(val message: String) : QrScannerEvent
}

class QrScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QrScannerUiState())
    val uiState: StateFlow<QrScannerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QrScannerEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<QrScannerEvent> = _events.asSharedFlow()

    private var readingJob: Job? = null
    private var lastInvalidAtMs: Long = 0L

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { state ->
            state.copy(
                cameraPermissionGranted = granted,
                // Never leave scanning stuck off solely due to a stale phase —
                // visibility reset owns re-enable. Only force-disable when denied.
                scanningEnabled = if (!granted) {
                    false
                } else if (state.phase == ScanPhase.Idle) {
                    true
                } else {
                    state.scanningEnabled
                }
            )
        }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(torchEnabled = !it.torchEnabled) }
    }

    /**
     * Must run whenever the scanner destination becomes visible again
     * (first open, back navigation, or process recreation).
     *
     * Root cause of the "can't scan after back" bug: ViewModel survived on the
     * back stack with scanningEnabled=false / phase=ReadyToNavigate, while
     * CameraPreview kept receiving frames but dropped them all.
     */
    fun onScreenVisible() {
        readingJob?.cancel()
        readingJob = null
        _uiState.update { state ->
            state.copy(
                phase = ScanPhase.Idle,
                scanningEnabled = state.cameraPermissionGranted,
                torchEnabled = false,
                cameraSession = state.cameraSession + 1
            )
        }
    }

    fun onRawQrDetected(raw: String) {
        if (!_uiState.value.scanningEnabled) return
        if (_uiState.value.phase != ScanPhase.Idle) return

        UpiDebugLog.banner("SCAN DETECTED")
        UpiDebugLog.section("RAW_QR")
        UpiDebugLog.line(raw)
        UpiDebugLog.field("raw_length", raw.length.toString())

        val parsed = UpiQrParser.parse(raw)
        if (parsed == null) {
            UpiDebugLog.line("scan_outcome = INVALID (parser returned null)")
            val now = System.currentTimeMillis()
            if (now - lastInvalidAtMs > INVALID_THROTTLE_MS) {
                lastInvalidAtMs = now
                _events.tryEmit(
                    QrScannerEvent.ShowInvalidQr(
                        "Invalid UPI QR\nPlease scan a valid payment QR code."
                    )
                )
            }
            return
        }

        UpiDebugLog.line("scan_outcome = VALID — storing PendingUpiScan")
        _uiState.update {
            it.copy(
                scanningEnabled = false,
                phase = ScanPhase.Reading,
                torchEnabled = false
            )
        }

        PendingUpiScan.set(parsed.rawValue)

        readingJob?.cancel()
        readingJob = viewModelScope.launch {
            delay(READING_DELAY_MS)
            _uiState.update { it.copy(phase = ScanPhase.ReadyToNavigate) }
            _events.emit(QrScannerEvent.NavigateToPaymentDetails)
        }
    }

    override fun onCleared() {
        readingJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val READING_DELAY_MS = 600L
        private const val INVALID_THROTTLE_MS = 2_200L

        fun factory(): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return QrScannerViewModel() as T
                }
            }
    }
}

package com.gorman.ourmemoryapp.ui.scan.models

sealed interface ScannerUiIntent {
    class OnPhotoTaken(val jpeg: ByteArray) : ScannerUiIntent
}

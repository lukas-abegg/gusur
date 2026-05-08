package com.gusur.app.util

actual fun pickJsonFile(onFilePicked: (String?) -> Unit) {
    // For mobile, we would use UIDocumentPickerViewController
    // Placeholder for now
    onFilePicked(null)
}

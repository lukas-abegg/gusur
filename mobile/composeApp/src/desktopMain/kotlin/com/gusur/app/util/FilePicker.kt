package com.gusur.app.util

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual fun pickJsonFile(onFilePicked: (String?) -> Unit) {
    val frame = Frame()
    val dialog = FileDialog(frame, "Select Scraper JSON", FileDialog.LOAD)
    dialog.file = "*.json"
    dialog.isVisible = true
    
    val file = dialog.file
    val directory = dialog.directory
    
    if (file != null && directory != null) {
        val content = File(directory, file).readText()
        onFilePicked(content)
    } else {
        onFilePicked(null)
    }
    
    frame.dispose()
}

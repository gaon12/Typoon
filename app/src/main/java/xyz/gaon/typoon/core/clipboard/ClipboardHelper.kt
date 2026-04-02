package xyz.gaon.typoon.core.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import xyz.gaon.typoon.core.text.TextPayloadSanitizer

class ClipboardHelper(
    private val context: Context,
) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun readText(): String? {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(context)
            return TextPayloadSanitizer.sanitize(text).ifBlank { null }
        }
        return null
    }

    fun writeText(text: String) {
        val clip = ClipData.newPlainText("typoon", text)
        clipboardManager.setPrimaryClip(clip)
    }
}

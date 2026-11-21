package com.fsck.k9.helper

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.os.Build
import android.os.PersistableBundle

/**
 * Access the system clipboard
 */
class ClipboardManager(private val context: Context) {

    /**
     * Copy a text string to the system clipboard
     *
     * @param label User-visible label for the content.
     * @param text The actual text to be copied to the clipboard.
     * @param isSensitive Whether the text should be hidden from clipboard previews shown by the system.
     */
    @JvmOverloads
    fun setText(label: String, text: String, isSensitive: Boolean = false) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        if (isSensitive) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboardManager.setPrimaryClip(clip)
    }

    private companion object {
        val EXTRA_IS_SENSITIVE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ClipDescription.EXTRA_IS_SENSITIVE
        } else {
            "android.content.extra.IS_SENSITIVE"
        }
    }
}

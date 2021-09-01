package com.rainyseason.cj.common

import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.view.View
import android.widget.EditText
import androidx.annotation.IdRes
import com.rainyseason.cj.R

fun isTextDifferent(str1: CharSequence?, str2: CharSequence?): Boolean {
    if (str1 === str2) {
        return false
    }
    if (str1 == null || str2 == null) {
        return true
    }
    val length = str1.length
    if (length != str2.length) {
        return true
    }

    if (str1 is Spanned) {
        return str1 != str2
    }

    for (i in 0 until length) {
        if (str1[i] != str2[i]) {
            return true
        }
    }
    return false
}

/**
 * Set the given text on the textview.
 *
 * @return True if the given text is different from the previous text on the textview.
 */
fun EditText.setTextIfDifferent(newText: CharSequence?): Boolean {
    if (!isTextDifferent(newText, text)) {
        // Previous text is the same. No op
        return false
    }

    setText(newText)
    // Since the text changed we move the cursor to the end of the new text.
    // This allows us to fill in text programmatically with a different value,
    // but if the user is typing and the view is rebound we won't lose their cursor position.
    setSelection(newText?.length ?: 0)
    return true
}


/**
 * Debounce the given block so that it is is delayed by the duration.
 * The duration timer is reset every time this is called, so that the block is only executed if this is not called for [durationMillis].
 * This is particularly intended for updating EditText views from mvrx view model state changes to avoid loops.
 * @param key The key corresponding to the handler. You can use the default if you only call this in one place per view.
 * @param durationMillis Amount of time to wait before calling the block again.
 * @param block lambda function that should only be run every durationMillis ms. The callback happens on the main thread
 */
fun <T : View> T.debounced(
    @IdRes key: Int = R.id.default_debounce_key,
    durationMillis: Long = 1000,
    block: T.() -> Unit
) {
    val handler: Handler = getTag(key) as Handler? ?: run {
        Handler(Looper.getMainLooper()).also {
            setTag(key, it)
        }
    }

    handler.removeCallbacksAndMessages(null)
    handler.postDelayed({ block() }, durationMillis)
}
package com.example.myapplication

import android.text.InputFilter
import android.text.Spanned

class DecimalDigitsInputFilter : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val existingText = dest.toString()
        val proposedText = existingText.substring(0, dstart) + source + existingText.substring(dend, existingText.length)
        if (proposedText.count { it == '.' } > 1) {
            return ""
        }
        return null
    }
}
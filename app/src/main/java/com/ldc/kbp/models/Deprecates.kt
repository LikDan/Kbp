package com.ldc.kbp.models

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.TextView
import androidx.core.app.ShareCompat
import com.itextpdf.layout.element.Table
import java.io.File
import java.util.*

object Deprecates {
    fun getScreenSize(activity: Activity): Point =
        Point().apply { activity.windowManager.defaultDisplay.getSize(this) }

    fun getStorageDir(type: String): File = Environment.getExternalStoragePublicDirectory(type)

    fun pdfTable(cols: Int) = Table(cols)

    fun shareIntentBuilder(activity: Activity) = ShareCompat.IntentBuilder.from(activity)

    fun setTextAppearance(textView: TextView, themeId: Int) = textView.setTextAppearance(themeId)
}
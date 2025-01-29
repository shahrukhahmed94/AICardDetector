package com.shahrukh.aicarddetector.utils

import android.util.DisplayMetrics
import androidx.compose.ui.unit.Dp

fun Dp.toPx(displayMetrics: DisplayMetrics): Float = this.value * displayMetrics.density

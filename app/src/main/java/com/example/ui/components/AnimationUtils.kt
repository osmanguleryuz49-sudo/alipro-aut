package com.example.ui.components

import android.content.Context
import android.provider.Settings

object AnimationUtils {
    /**
     * Returns true if system animations are explicitly disabled by the user in Developer Options.
     */
    fun areAnimationsDisabled(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0.0f
        } catch (_: Exception) {
            false
        }
    }
}

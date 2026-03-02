package com.example.al_aalim.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility class for haptic feedback following Material Design guidelines
 * Provides consistent, contextual haptic patterns throughout the app
 */
object HapticUtils {
    
    /**
     * Haptic feedback types with appropriate intensities
     */
    enum class HapticType {
        LIGHT_TAP,          // Subtle feedback for frequent actions (scrolling, typing)
        MEDIUM_CLICK,       // Standard button press feedback
        HEAVY_CLICK,        // Important action confirmation
        SUCCESS,            // Action completed successfully
        ERROR,              // Error or invalid action
        LONG_PRESS,         // Long press detected
        QIBLA_FOUND,        // Unique pattern when Qibla direction is found
        PRAYER_TIME,        // Alert for prayer time
        MESSAGE_RECEIVED,   // New message notification
        TICK               // Clock tick or progress increment
    }
    
    /**
     * Perform haptic feedback on a view
     * Uses HapticFeedbackConstants for standard interactions
     */
    fun performHaptic(view: View, type: HapticType) {
        if (!isHapticsEnabled(view.context)) return
        
        when (type) {
            HapticType.LIGHT_TAP -> {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            HapticType.MEDIUM_CLICK -> {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            HapticType.HEAVY_CLICK -> {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            HapticType.LONG_PRESS -> {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            HapticType.TICK -> {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            else -> {
                // For custom patterns, use vibrator directly
                performCustomHaptic(view.context, type)
            }
        }
    }
    
    /**
     * Perform custom haptic patterns using Vibrator API
     */
    private fun performCustomHaptic(context: Context, type: HapticType) {
        if (!isHapticsEnabled(context)) return
        
        val vibrator = getVibrator(context) ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (type) {
                HapticType.SUCCESS -> {
                    // Double pulse for success
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 50, 50),
                        intArrayOf(0, 180, 0, 180),
                        -1
                    )
                }
                HapticType.ERROR -> {
                    // Triple short pulses for error
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 30, 30, 30, 30, 30),
                        intArrayOf(0, 200, 0, 200, 0, 200),
                        -1
                    )
                }
                HapticType.QIBLA_FOUND -> {
                    // Unique rhythmic pattern for Qibla found
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 100, 100, 100, 100, 200),
                        intArrayOf(0, 150, 0, 200, 0, 255),
                        -1
                    )
                }
                HapticType.PRAYER_TIME -> {
                    // Gentle but noticeable pattern for prayer time
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 200, 200, 200),
                        intArrayOf(0, 180, 0, 220),
                        -1
                    )
                }
                HapticType.MESSAGE_RECEIVED -> {
                    // Subtle pulse for message received
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 40, 80, 40),
                        intArrayOf(0, 160, 0, 160),
                        -1
                    )
                }
                else -> {
                    // Default medium click
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
            }
            vibrator.vibrate(effect)
        } else {
            // Fallback for older devices
            @Suppress("DEPRECATION")
            when (type) {
                HapticType.SUCCESS -> vibrator.vibrate(longArrayOf(0, 50, 50, 50), -1)
                HapticType.ERROR -> vibrator.vibrate(longArrayOf(0, 30, 30, 30, 30, 30), -1)
                HapticType.QIBLA_FOUND -> vibrator.vibrate(longArrayOf(0, 100, 100, 100, 100, 200), -1)
                HapticType.PRAYER_TIME -> vibrator.vibrate(longArrayOf(0, 200, 200, 200), -1)
                HapticType.MESSAGE_RECEIVED -> vibrator.vibrate(longArrayOf(0, 40, 80, 40), -1)
                else -> vibrator.vibrate(50)
            }
        }
    }
    
    /**
     * Get vibrator instance based on Android version
     */
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    /**
     * Check if haptics are enabled in system settings
     */
    private fun isHapticsEnabled(context: Context): Boolean {
        return try {
            android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) == 1
        } catch (e: Exception) {
            true // Default to enabled if setting can't be read
        }
    }
    
    /**
     * Extension function for easy haptic feedback on views
     */
    fun View.haptic(type: HapticType = HapticType.MEDIUM_CLICK) {
        performHaptic(this, type)
    }
}

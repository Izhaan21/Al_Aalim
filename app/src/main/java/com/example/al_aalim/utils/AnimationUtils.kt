package com.example.al_aalim.utils

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * Enhanced animation utilities for Al-Aalim app
 * Provides micro-interactions, haptic feedback, and smooth animations
 */
object AnimationUtils {
    
    // Animation constants
    private const val SCALE_DOWN = 0.95f
    private const val SCALE_NORMAL = 1.0f
    private const val SPRING_STIFFNESS = SpringForce.STIFFNESS_MEDIUM
    private const val SPRING_DAMPING = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
    
    /**
     * Original click animation (kept for compatibility)
     */
    fun animateClick(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(SCALE_DOWN)
            .scaleY(SCALE_DOWN)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(SCALE_NORMAL)
                    .scaleY(SCALE_NORMAL)
                    .setDuration(100)
                    .withEndAction {
                        action()
                    }
                    .start()
            }
            .start()
    }
    
    /**
     * Original extension function (kept for compatibility)
     */
    fun View.setOnClickWithAnimation(action: () -> Unit) {
        this.setOnClickListener {
            animateClick(this, action)
        }
    }
    
    /**
     * Enhanced click animation with haptic feedback and spring bounce
     */
    fun View.setOnClickWithEnhancedAnimation(action: () -> Unit) {
        setOnClickListener {
            // Haptic feedback
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            
            // Scale down animation
            animate()
                .scaleX(SCALE_DOWN)
                .scaleY(SCALE_DOWN)
                .setDuration(100)
                .withEndAction {
                    // Spring back with overshoot
                    createSpringAnimation(this, DynamicAnimation.SCALE_X, SCALE_NORMAL).start()
                    createSpringAnimation(this, DynamicAnimation.SCALE_Y, SCALE_NORMAL).start()
                }
                .start()
            
            action()
        }
    }
    
    /**
     * Create spring animation for natural motion
     */
    private fun createSpringAnimation(
        view: View,
        property: DynamicAnimation.ViewProperty,
        finalPosition: Float
    ): SpringAnimation {
        return SpringAnimation(view, property).apply {
            spring = SpringForce(finalPosition).apply {
                stiffness = SPRING_STIFFNESS
                dampingRatio = SPRING_DAMPING
            }
        }
    }
    
    /**
     * Pulse animation for attention-grabbing elements
     */
    fun View.pulseAnimation(duration: Long = 1000) {
        val scaleUp = 1.1f
        animate()
            .scaleX(scaleUp)
            .scaleY(scaleUp)
            .setDuration(duration / 2)
            .withEndAction {
                animate()
                    .scaleX(SCALE_NORMAL)
                    .scaleY(SCALE_NORMAL)
                    .setDuration(duration / 2)
                    .start()
            }
            .start()
    }
    
    /**
     * Shake animation for errors
     */
    fun View.shakeAnimation() {
        val shake = 10f
        animate()
            .translationX(shake)
            .setDuration(50)
            .withEndAction {
                animate().translationX(-shake).setDuration(50).withEndAction {
                    animate().translationX(shake).setDuration(50).withEndAction {
                        animate().translationX(0f).setDuration(50).start()
                    }.start()
                }.start()
            }
            .start()
        
        // Haptic feedback for error
        performHapticFeedback(HapticFeedbackConstants.REJECT)
    }
    
    /**
     * Success animation with checkmark effect
     */
    fun View.successAnimation() {
        // Scale up briefly then back to normal
        animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(150)
            .withEndAction {
                animate()
                    .scaleX(SCALE_NORMAL)
                    .scaleY(SCALE_NORMAL)
                    .setInterpolator(OvershootInterpolator())
                    .setDuration(300)
                    .start()
            }
            .start()
        
        // Haptic feedback for success
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
    
    /**
     * Fade in animation
     */
    fun View.fadeIn(duration: Long = 300) {
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(duration)
            .start()
    }
    
    /**
     * Fade out animation
     */
    fun View.fadeOut(duration: Long = 300, onComplete: (() -> Unit)? = null) {
        animate()
            .alpha(0f)
            .setDuration(duration)
            .withEndAction {
                visibility = View.GONE
                onComplete?.invoke()
            }
            .start()
    }
    
    /**
     * Slide in from bottom animation
     */
    fun View.slideInFromBottom(duration: Long = 300) {
        translationY = height.toFloat()
        visibility = View.VISIBLE
        animate()
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(OvershootInterpolator())
            .start()
    }
    
    /**
     * Slide out to bottom animation
     */
    fun View.slideOutToBottom(duration: Long = 300, onComplete: (() -> Unit)? = null) {
        animate()
            .translationY(height.toFloat())
            .setDuration(duration)
            .withEndAction {
                visibility = View.GONE
                onComplete?.invoke()
            }
            .start()
    }
    
    /**
     * Rotate animation
     */
    fun View.rotateAnimation(degrees: Float, duration: Long = 300) {
        animate()
            .rotation(degrees)
            .setDuration(duration)
            .start()
    }
    
    /**
     * Vibrate device for haptic feedback
     */
    fun Context.vibrate(milliseconds: Long = 50) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }
    
    /**
     * Color transition animation
     */
    fun View.animateBackgroundColor(fromColor: Int, toColor: Int, duration: Long = 300) {
        ValueAnimator.ofArgb(fromColor, toColor).apply {
            this.duration = duration
            addUpdateListener { animator ->
                setBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }
    }
}

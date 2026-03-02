package com.example.al_aalim.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.RecyclerView

/**
 * Enhanced animation utilities for smooth, modern UI interactions
 * Includes spring animations, entrance effects, and success/error feedback
 */
object EnhancedAnimationUtils {
    
    /**
     * Spring animation for button press (more natural than scale-down)
     * Combines scale and slight rotation for organic feel
     */
    fun View.setOnClickWithSpringAnimation(
        hapticType: HapticUtils.HapticType = HapticUtils.HapticType.MEDIUM_CLICK,
        onClick: () -> Unit
    ) {
        setOnClickListener {
            // Haptic feedback
            HapticUtils.performHaptic(this, hapticType)
            
            // Spring animation
            animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(100)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .setInterpolator(OvershootInterpolator(2f))
                        .start()
                }
                .start()
            
            onClick()
        }
    }
    
    /**
     * Shake animation for errors (e.g., invalid input)
     */
    fun View.shakeAnimation(onComplete: (() -> Unit)? = null) {
        val animator = ObjectAnimator.ofFloat(
            this,
            "translationX",
            0f, -25f, 25f, -25f, 25f, -15f, 15f, -5f, 5f, 0f
        ).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            doOnEnd { onComplete?.invoke() }
        }
        animator.start()
    }
    
    /**
     * Success checkmark animation
     * Creates a green circular background with checkmark
     */
    fun View.showSuccessAnimation(duration: Long = 1500, onComplete: (() -> Unit)? = null) {
        // Store original state
        val originalAlpha = alpha
        val originalScaleX = scaleX
        val originalScaleY = scaleY
        
        // Animate scale up with green tint
        animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .alpha(0.8f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                postDelayed({
                    // Animate back to original
                    animate()
                        .scaleX(originalScaleX)
                        .scaleY(originalScaleY)
                        .alpha(originalAlpha)
                        .setDuration(200)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction { onComplete?.invoke() }
                        .start()
                }, duration - 400)
            }
            .start()
    }
    
    /**
     * Pulse animation for notification badges and important elements
     */
    fun View.pulseAnimation(repeatCount: Int = ValueAnimator.INFINITE) {
        val scaleUp = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.15f).apply {
            duration = 600
            repeatMode = ValueAnimator.REVERSE
            this.repeatCount = repeatCount
        }
        val scaleUpY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.15f).apply {
            duration = 600
            repeatMode = ValueAnimator.REVERSE
            this.repeatCount = repeatCount
        }
        val alphaAnim = ObjectAnimator.ofFloat(this, "alpha", 1f, 0.7f).apply {
            duration = 600
            repeatMode = ValueAnimator.REVERSE
            this.repeatCount = repeatCount
        }
        
        AnimatorSet().apply {
            playTogether(scaleUp, scaleUpY, alphaAnim)
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
    
    /**
     * Stop pulse animation
     */
    fun View.stopPulseAnimation() {
        animate().cancel()
        scaleX = 1f
        scaleY = 1f
        alpha = 1f
    }
    
    /**
     * Fade in animation
     */
    fun View.fadeIn(duration: Long = 300, onComplete: (() -> Unit)? = null) {
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { onComplete?.invoke() }
            .start()
    }
    
    /**
     * Fade out animation
     */
    fun View.fadeOut(duration: Long = 300, onComplete: (() -> Unit)? = null) {
        animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                visibility = View.GONE
                onComplete?.invoke()
            }
            .start()
    }
    
    /**
     * Slide in from bottom animation
     */
    fun View.slideInFromBottom(duration: Long = 400, onComplete: (() -> Unit)? = null) {
        translationY = height.toFloat()
        alpha = 0f
        visibility = View.VISIBLE
        
        animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(OvershootInterpolator(0.5f))
            .withEndAction { onComplete?.invoke() }
            .start()
    }
    
    /**
     * Slide out to bottom animation
     */
    fun View.slideOutToBottom(duration: Long = 300, onComplete: (() -> Unit)? = null) {
        animate()
            .translationY(height.toFloat())
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                visibility = View.GONE
                onComplete?.invoke()
            }
            .start()
    }
    
    /**
     * Entrance animation for RecyclerView items
     * Combines fade and slide for smooth appearance
     */
    fun View.itemEntranceAnimation(position: Int, fromRight: Boolean = false) {
        // Stagger animation based on position
        val delay = (position * 50L).coerceAtMost(300L)
        
        alpha = 0f
        translationX = if (fromRight) 100f else -100f
        translationY = 50f
        
        animate()
            .alpha(1f)
            .translationX(0f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator(0.8f))
            .start()
    }
    
    /**
     * Shimmer loading effect
     * Creates a subtle shine animation across the view
     */
    fun View.startShimmerAnimation() {
        val shimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                // Apply alpha variation to create shimmer effect
                alpha = 0.3f + (fraction * 0.4f)
            }
        }
        tag = shimmerAnimator
        shimmerAnimator.start()
    }
    
    /**
     * Stop shimmer animation
     */
    fun View.stopShimmerAnimation() {
        (tag as? ValueAnimator)?.cancel()
        alpha = 1f
        tag = null
    }
    
    /**
     * Rotate animation (useful for refresh icons, etc.)
     */
    fun View.rotateAnimation(degrees: Float = 360f, duration: Long = 500) {
        animate()
            .rotation(rotation + degrees)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
    
    /**
     * Custom RecyclerView ItemAnimator with slide-in effect
     */
    class SlideInItemAnimator : RecyclerView.ItemAnimator() {
        
        override fun animateDisappearance(
            viewHolder: RecyclerView.ViewHolder,
            preLayoutInfo: ItemHolderInfo,
            postLayoutInfo: ItemHolderInfo?
        ): Boolean {
            viewHolder.itemView.slideOutToBottom()
            return false
        }
        
        override fun animateAppearance(
            viewHolder: RecyclerView.ViewHolder,
            preLayoutInfo: ItemHolderInfo?,
            postLayoutInfo: ItemHolderInfo
        ): Boolean {
            viewHolder.itemView.slideInFromBottom()
            return false
        }
        
        override fun animatePersistence(
            viewHolder: RecyclerView.ViewHolder,
            preLayoutInfo: ItemHolderInfo,
            postLayoutInfo: ItemHolderInfo
        ): Boolean {
            return false
        }
        
        override fun animateChange(
            oldHolder: RecyclerView.ViewHolder,
            newHolder: RecyclerView.ViewHolder,
            preLayoutInfo: ItemHolderInfo,
            postLayoutInfo: ItemHolderInfo
        ): Boolean {
            // Fade out old holder, fade in new holder
            oldHolder.itemView.fadeOut()
            newHolder.itemView.fadeIn()
            return false
        }
        
        override fun runPendingAnimations() {
            // No pending animations to run
        }
        
        override fun endAnimation(item: RecyclerView.ViewHolder) {
            item.itemView.clearAnimation()
        }
        
        override fun endAnimations() {
            // End all animations
        }
        
        override fun isRunning(): Boolean {
            return false
        }
    }
}

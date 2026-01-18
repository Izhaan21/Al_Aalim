package com.example.al_aalim.utils

import android.view.View

/**
 * Animation utilities for UI interactions
 */
object AnimationUtils {
    
    /**
     * Apply a scale-down click animation to a view
     * This creates a "press" effect when the view is clicked
     * 
     * @param view The view to animate
     * @param action The action to perform after animation completes
     */
    fun animateClick(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction {
                        action()
                    }
                    .start()
            }
            .start()
    }
    
    /**
     * Extension function for View to easily apply click animation
     */
    fun View.setOnClickWithAnimation(action: () -> Unit) {
        this.setOnClickListener {
            animateClick(this, action)
        }
    }
}

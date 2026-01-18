package com.example.al_aalim.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    // Circle size: 280dp converted to pixels
    private val circleSizeDp = 280f
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Calculate circle size and position
        val circleSize = circleSizeDp * resources.displayMetrics.density
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = circleSize / 2f
        
        // Use a layer with ARGB_8888 to support transparency
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        
        // Draw the semi-transparent overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        
        // Clear the circle area
        canvas.drawCircle(centerX, centerY, radius, clearPaint)
        
        // Restore the layer
        canvas.restoreToCount(saveCount)
        
        // Draw white border around the circle
        canvas.drawCircle(centerX, centerY, radius, borderPaint)
    }
    
    // Get the crop circle bounds
    fun getCropCircleBounds(): FloatArray {
        val circleSize = circleSizeDp * resources.displayMetrics.density
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = circleSize / 2f
        
        return floatArrayOf(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
    }
}

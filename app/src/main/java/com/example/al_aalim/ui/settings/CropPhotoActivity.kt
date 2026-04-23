package com.example.al_aalim.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.example.al_aalim.utils.ProfileManager
import com.example.al_aalim.ui.theme.AlAalimTheme

class CropPhotoActivity : ComponentActivity() {
    
    private lateinit var accountViewModel: com.example.al_aalim.viewmodel.AccountViewModel
    
    // Image transformation
    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    
    private var mode = NONE
    private val start = android.graphics.PointF()
    private val mid = android.graphics.PointF()
    private var oldDist = 1f
    
    private var imageWidth = 0f
    private var imageHeight = 0f
    private var viewWidth = 0
    private var viewHeight = 0
    private var minScale = 1f
    private var maxScale = 5f
    
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var imageViewGlobal: ImageView? = null

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val RESULT_CROPPED = 100
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val factory = com.example.al_aalim.viewmodel.ViewModelFactory(this)
        accountViewModel = androidx.lifecycle.ViewModelProvider(this, factory)[com.example.al_aalim.viewmodel.AccountViewModel::class.java]
        
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)

        setContent {
            AlAalimTheme {
                Box(modifier = Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            com.example.al_aalim.ui.theme.BackgroundGradientStart,
                            com.example.al_aalim.ui.theme.BackgroundGradientEnd
                        )
                    )
                )) {
                    // Compose wrapper for ImageView to maintain Matrix processing
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            ImageView(context).apply {
                                imageViewGlobal = this
                                layoutParams = android.view.ViewGroup.LayoutParams(-1, -1)
                                scaleType = ImageView.ScaleType.MATRIX
                                
                                setOnTouchListener { _, event ->
                                    handleTouch(event)
                                    true
                                }
                                
                                if (imageUri != null) {
                                    post {
                                        try {
                                            val bmp = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                                            setImageBitmap(bmp)
                                            setupImageMatrix(bmp, this)
                                        } catch (e: Exception) {
                                            Toast.makeText(this@CropPhotoActivity, "Failed to load", Toast.LENGTH_SHORT).show()
                                            finish()
                                        }
                                    }
                                }
                            }
                        }
                    )
                    
                    // Box Overlay (Outside semi-transparent, inside clear circle)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = {}, indication = null, interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource())
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        )
                    }

                        // Bottom controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    setResult(RESULT_CANCELED)
                                    finish()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.08f)
                                )
                            ) {
                                Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            
                            Button(
                                onClick = { cropAndSaveImage() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(
                                                com.example.al_aalim.ui.theme.GoldGradientStart,
                                                com.example.al_aalim.ui.theme.GoldGradientEnd
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                }
            }
        }
    }
    
    private fun setupImageMatrix(bitmap: Bitmap, imageView: ImageView) {
        viewWidth = imageView.width
        viewHeight = imageView.height
        imageWidth = bitmap.width.toFloat()
        imageHeight = bitmap.height.toFloat()
        
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val initialScale = maxOf(scaleX, scaleY)
        minScale = initialScale * 0.3f
        
        matrix.reset()
        matrix.setScale(initialScale, initialScale)
        
        val scaledWidth = imageWidth * initialScale
        val scaledHeight = imageHeight * initialScale
        val translateX = (viewWidth - scaledWidth) / 2
        val translateY = (viewHeight - scaledHeight) / 2
        matrix.postTranslate(translateX, translateY)
        
        imageView.imageMatrix = matrix
    }
    
    private fun handleTouch(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = ZOOM
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.set(savedMatrix)
                    matrix.postTranslate(event.x - start.x, event.y - start.y)
                } else if (mode == ZOOM) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        matrix.postScale(scale, scale, mid.x, mid.y)
                    }
                }
                constrainMatrix()
                imageViewGlobal?.imageMatrix = matrix
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }
        return true
    }
    
    private fun constrainMatrix() {
        val values = FloatArray(9)
        matrix.getValues(values)
        val currentScale = values[Matrix.MSCALE_X]
        
        if (currentScale < minScale) {
            val scaleRatio = minScale / currentScale
            matrix.postScale(scaleRatio, scaleRatio, viewWidth / 2f, viewHeight / 2f)
        } else if (currentScale > maxScale) {
            val scaleRatio = maxScale / currentScale
            matrix.postScale(scaleRatio, scaleRatio, viewWidth / 2f, viewHeight / 2f)
        }
        
        matrix.getValues(values)
        val scaledWidth = imageWidth * values[Matrix.MSCALE_X]
        val scaledHeight = imageHeight * values[Matrix.MSCALE_X]
        
        val cropSize = 280 * resources.displayMetrics.density
        val cropLeft = (viewWidth - cropSize) / 2
        val cropTop = (viewHeight - cropSize) / 2
        val cropRight = cropLeft + cropSize
        val cropBottom = cropTop + cropSize
        
        var newTransX = values[Matrix.MTRANS_X]
        var newTransY = values[Matrix.MTRANS_Y]
        
        if (scaledWidth >= cropSize && scaledHeight >= cropSize) {
            if (newTransX + scaledWidth < cropRight) newTransX = cropRight - scaledWidth
            if (newTransX > cropLeft) newTransX = cropLeft
            if (newTransY + scaledHeight < cropBottom) newTransY = cropBottom - scaledHeight
            if (newTransY > cropTop) newTransY = cropTop
        } else {
            if (newTransX < 0) newTransX = 0f
            if (newTransX + scaledWidth > viewWidth) newTransX = viewWidth - scaledWidth
            if (newTransY < 0) newTransY = 0f
            if (newTransY + scaledHeight > viewHeight) newTransY = viewHeight - scaledHeight
        }
        
        matrix.postTranslate(newTransX - values[Matrix.MTRANS_X], newTransY - values[Matrix.MTRANS_Y])
    }
    
    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt((x * x + y * y).toDouble()).toFloat()
    }
    
    private fun midPoint(point: android.graphics.PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }
    
    private fun cropAndSaveImage() {
        try {
            val drawable = imageViewGlobal?.drawable as? BitmapDrawable
            if (drawable == null) {
                Toast.makeText(this, "No image", Toast.LENGTH_SHORT).show()
                return
            }
            
            val originalBitmap = drawable.bitmap
            val viewBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(viewBitmap)
            canvas.concat(matrix)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)
            
            val cropSize = (280 * resources.displayMetrics.density).toInt()
            val cropLeft = (viewWidth - cropSize) / 2
            val cropTop = (viewHeight - cropSize) / 2
            
            val croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
            val cropCanvas = Canvas(croppedBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            cropCanvas.drawCircle(cropSize / 2f, cropSize / 2f, cropSize / 2f, paint)
            
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            cropCanvas.drawBitmap(viewBitmap, -cropLeft.toFloat(), -cropTop.toFloat(), paint)
            
            val userId = accountViewModel.currentUser?.uid ?: return
            
            val saved = ProfileManager.saveProfileBitmap(this, userId, croppedBitmap)
            if (saved) {
                Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CROPPED)
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector) = true
    }
}

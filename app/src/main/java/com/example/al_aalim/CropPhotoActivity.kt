package com.example.al_aalim

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.al_aalim.databinding.ActivityCropPhotoBinding
import com.example.al_aalim.utils.ProfileManager
import com.example.al_aalim.auth.FirebaseAuthManager

class CropPhotoActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCropPhotoBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private lateinit var authManager: FirebaseAuthManager
    
    // Image transformation
    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    
    // Touch handling
    private var mode = NONE
    private val start = android.graphics.PointF()
    private val mid = android.graphics.PointF()
    private var oldDist = 1f
    
    // Image bounds
    private var imageWidth = 0f
    private var imageHeight = 0f
    private var viewWidth = 0
    private var viewHeight = 0
    private var minScale = 1f
    private var maxScale = 5f
    
    // Scale gesture detector
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    
    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val RESULT_CROPPED = 100
        
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize Firebase Auth Manager
        authManager = FirebaseAuthManager(this)
        
        // Initialize View Binding
        binding = ActivityCropPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configure system bars for dark theme
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        
        // Set dark system bars
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.parseColor("#1A1A1A")
        
        // For Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        
        // Hide action bar
        supportActionBar?.hide()
        
        // Initialize scale gesture detector
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        
        // Load image from intent
        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)
        if (imageUri != null) {
            loadImage(imageUri)
        } else {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Setup touch listener for pan/zoom on both image and overlay
        binding.ivPhoto.setOnTouchListener { _, event ->
            handleTouch(event)
            true
        }
        
        // Also handle touch on overlay so gestures work outside the circle
        binding.cropOverlay.setOnTouchListener { _, event ->
            handleTouch(event)
            true
        }
        
        // Setup buttons
        binding.btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        
        binding.btnDone.setOnClickListener {
            cropAndSaveImage()
        }
    }
    
    private fun loadImage(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            binding.ivPhoto.setImageBitmap(bitmap)
            
            // Wait for layout to complete before setting up matrix
            binding.ivPhoto.post {
                setupImageMatrix(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupImageMatrix(bitmap: Bitmap) {
        viewWidth = binding.ivPhoto.width
        viewHeight = binding.ivPhoto.height
        imageWidth = bitmap.width.toFloat()
        imageHeight = bitmap.height.toFloat()
        
        // Calculate initial scale to fit the view (fill mode)
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val initialScale = maxOf(scaleX, scaleY) // Use max to ensure image fills the crop area
        
        // Allow zooming out to 30% of initial scale
        minScale = initialScale * 0.3f
        
        // Start with image centered and scaled to fill
        matrix.reset()
        matrix.setScale(initialScale, initialScale)
        
        // Center the image
        val scaledWidth = imageWidth * initialScale
        val scaledHeight = imageHeight * initialScale
        val translateX = (viewWidth - scaledWidth) / 2
        val translateY = (viewHeight - scaledHeight) / 2
        matrix.postTranslate(translateX, translateY)
        
        binding.ivPhoto.imageMatrix = matrix
        binding.ivPhoto.scaleType = ImageView.ScaleType.MATRIX
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
                    val dx = event.x - start.x
                    val dy = event.y - start.y
                    matrix.postTranslate(dx, dy)
                } else if (mode == ZOOM) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        matrix.postScale(scale, scale, mid.x, mid.y)
                    }
                }
                
                // Apply bounds checking
                constrainMatrix()
                binding.ivPhoto.imageMatrix = matrix
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
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]
        
        // Constrain scale
        if (currentScale < minScale) {
            val scaleRatio = minScale / currentScale
            matrix.postScale(scaleRatio, scaleRatio, viewWidth / 2f, viewHeight / 2f)
        } else if (currentScale > maxScale) {
            val scaleRatio = maxScale / currentScale
            matrix.postScale(scaleRatio, scaleRatio, viewWidth / 2f, viewHeight / 2f)
        }
        
        // Get updated values after scale constraint
        matrix.getValues(values)
        val scaledWidth = imageWidth * values[Matrix.MSCALE_X]
        val scaledHeight = imageHeight * values[Matrix.MSCALE_X]
        
        // Calculate crop circle bounds (centered, 280dp)
        val cropSize = 280 * resources.displayMetrics.density
        val cropLeft = (viewWidth - cropSize) / 2
        val cropTop = (viewHeight - cropSize) / 2
        val cropRight = cropLeft + cropSize
        val cropBottom = cropTop + cropSize
        
        // Only constrain translation when image is larger than crop area
        // When zoomed out (image smaller than crop), allow free movement but keep centered
        var newTransX = values[Matrix.MTRANS_X]
        var newTransY = values[Matrix.MTRANS_Y]
        
        if (scaledWidth >= cropSize && scaledHeight >= cropSize) {
            // Image is larger than crop - constrain so image always covers crop area
            // Don't let image right edge go past crop left
            if (newTransX + scaledWidth < cropRight) {
                newTransX = cropRight - scaledWidth
            }
            // Don't let image left edge go past crop right
            if (newTransX > cropLeft) {
                newTransX = cropLeft
            }
            // Don't let image bottom edge go past crop top
            if (newTransY + scaledHeight < cropBottom) {
                newTransY = cropBottom - scaledHeight
            }
            // Don't let image top edge go past crop bottom
            if (newTransY > cropTop) {
                newTransY = cropTop
            }
        } else {
            // Image is smaller - keep it within view bounds but allow positioning
            // Keep image within view horizontally
            if (newTransX < 0) {
                newTransX = 0f
            }
            if (newTransX + scaledWidth > viewWidth) {
                newTransX = viewWidth - scaledWidth
            }
            // Keep image within view vertically
            if (newTransY < 0) {
                newTransY = 0f
            }
            if (newTransY + scaledHeight > viewHeight) {
                newTransY = viewHeight - scaledHeight
            }
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
            // Get the current visible bitmap from ImageView
            val drawable = binding.ivPhoto.drawable as? BitmapDrawable
            if (drawable == null) {
                Toast.makeText(this, "No image to crop", Toast.LENGTH_SHORT).show()
                return
            }
            
            val originalBitmap = drawable.bitmap
            
            // Create a bitmap from the ImageView with current matrix transformation
            val viewBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(viewBitmap)
            canvas.concat(matrix)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)
            
            // Calculate crop circle bounds
            val cropSize = (280 * resources.displayMetrics.density).toInt()
            val cropLeft = (viewWidth - cropSize) / 2
            val cropTop = (viewHeight - cropSize) / 2
            
            // Create the cropped circular bitmap
            val croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
            val cropCanvas = Canvas(croppedBitmap)
            
            // Draw circular mask
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            cropCanvas.drawCircle(cropSize / 2f, cropSize / 2f, cropSize / 2f, paint)
            
            // Apply source-in mode to crop
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            cropCanvas.drawBitmap(viewBitmap, -cropLeft.toFloat(), -cropTop.toFloat(), paint)
            
            // Save the cropped bitmap with user ID
            val userId = authManager.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                return
            }
            
            val saved = ProfileManager.saveProfileBitmap(this, userId, croppedBitmap)
            
            if (saved) {
                Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CROPPED)
                finish()
            } else {
                Toast.makeText(this, "Failed to save cropped image", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error cropping image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Handle via touch events instead
            return true
        }
    }
}

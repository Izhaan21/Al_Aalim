package com.example.al_aalim

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.al_aalim.databinding.FragmentHomeBinding
import com.example.al_aalim.repository.ChatRepository
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation
import com.example.al_aalim.utils.GeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val imageUris = mutableListOf<Uri>()
    
    // Firebase repository for chat messages and file uploads
    private lateinit var chatRepository: ChatRepository
    private val fragmentScope = CoroutineScope(Dispatchers.Main)
    private var activeConversationId: String? = null
    
    // Camera permission launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            android.widget.Toast.makeText(
                requireContext(),
                "Camera permission is required to take photos",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // Gallery permission launcher
    private val requestGalleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openGallery()
        } else {
            android.widget.Toast.makeText(
                requireContext(),
                "Storage permission is required to access photos",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // Gallery picker launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            imageUris.add(uri)
            addImagePreview(uri)
        }
    }
    
    // File picker launcher
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            // Take persistent permission for the file
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            // Check MIME type to determine how to display the preview
            val mimeType = requireContext().contentResolver.getType(uri)
            when {
                // Show image preview for image files
                mimeType?.startsWith("image/") == true -> {
                    imageUris.add(uri)
                    addImagePreview(uri)
                }
                // Show video thumbnail for video files
                mimeType?.startsWith("video/") == true -> {
                    addVideoPreview(uri)
                }
                // Show file icon for other file types
                else -> {
                    addFilePreview(uri)
                }
            }
        }
    }
    
    // Camera capture launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            imageUris.lastOrNull()?.let { uri ->
                addImagePreview(uri)
            }
        } else {
            imageUris.removeLastOrNull()
        }
    }
    
    // Speech recognition permission launcher
    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startSpeechRecognition()
        } else {
            android.widget.Toast.makeText(
                requireContext(),
                "Microphone permission is required for voice input",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // Speech recognition launcher
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                // Append to existing text with a space if there's already text
                val currentText = binding.etChatInput.text.toString()
                if (currentText.isNotEmpty()) {
                    binding.etChatInput.setText("$currentText $spokenText")
                } else {
                    binding.etChatInput.setText(spokenText)
                }
                // Move cursor to end
                binding.etChatInput.setSelection(binding.etChatInput.text.length)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase repository
        chatRepository = ChatRepository(requireContext())
        
        // Initialize Gemini AI service
        // TODO: Replace with your API key from https://makersuite.google.com/app/apikey
        GeminiService.initialize()
        
        // Setup attachment button click handler
        binding.ivAddAttachment.setOnClickWithAnimation {
            showAttachmentOptions()
        }
        
        // Request focus and show keyboard on startup
        binding.etChatInput.post {
            binding.etChatInput.requestFocus()
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(binding.etChatInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        
        // Add TextWatcher to change icon based on input or attachments
        binding.etChatInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSendButtonIcon()
            }
            
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        // Setup send button click listener
        setupSendButton()
        
        // Setup menu icon click to open drawer
        binding.ivMenu.setOnClickWithAnimation {
            (activity as? ContainerActivity)?.openDrawer()
        }
        
        // Setup account icon click to open AccountActivity
        binding.ivAccount.setOnClickWithAnimation {
            val intent = android.content.Intent(requireContext(), AccountActivity::class.java)
            startActivity(intent)
        }
        
        // Setup voice input button click listener
        binding.ivVoiceInput.setOnClickWithAnimation {
            launchSpeechRecognition()
        }
        
        // Handle keyboard visibility - adjust padding so chat input stays above keyboard
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // When keyboard is visible, add bottom padding to push content up
            val bottomPadding = if (imeInsets.bottom > 0) {
                imeInsets.bottom
            } else {
                systemBarsInsets.bottom
            }
            
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottomPadding)
            windowInsets
        }
    }
    
    private fun launchSpeechRecognition() {
        // Check if device supports speech recognition
        val packageManager = requireContext().packageManager
        val activities = packageManager.queryIntentActivities(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
        )
        
        if (activities.isEmpty()) {
            android.widget.Toast.makeText(
                requireContext(),
                "Speech recognition is not available on this device",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Check for audio permission
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startSpeechRecognition()
            }
            else -> {
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        speechRecognizerLauncher.launch(intent)
    }
    
    private fun setupSendButton() {
        binding.ivSendMessage.setOnClickWithAnimation {
            val message = binding.etChatInput.text.toString().trim()
            val hasAttachments = binding.imagePreviewContainer.childCount > 0
            
            // Send if there's text OR attachments
            if (message.isNotEmpty() || hasAttachments) {
                sendMessage(message)
            }
        }
    }
    
    // Helper function to update send button icon based on text or attachments
    private fun updateSendButtonIcon() {
        val hasText = binding.etChatInput.text.isNotEmpty()
        val hasAttachments = binding.imagePreviewContainer.childCount > 0
        
        if (hasText || hasAttachments) {
            binding.ivSendMessage.setImageResource(R.drawable.uparrow)
        } else {
            binding.ivSendMessage.setImageResource(R.drawable.voice_lines)
        }
    }
    
    private fun sendMessage(message: String) {
        // Hide welcome container on first message
        binding.welcomeContainer.visibility = View.GONE
        
        // Ensure we have an active conversation
        if (activeConversationId == null) {
            // Create a new conversation with auto-generated title from message
            fragmentScope.launch {
                val conversationTitle = generateConversationTitle(message)
                val result = withContext(Dispatchers.IO) {
                    chatRepository.createConversation(conversationTitle)
                }
                if (result.isSuccess) {
                    activeConversationId = result.getOrNull()
                    (activity as? ContainerActivity)?.activeConversationId = activeConversationId
                    sendMessageToConversation(message)
                }
            }
            return
        }
        
        sendMessageToConversation(message)
    }
    
    /**
     * Generate conversation title from message
     * Takes first 40 characters, truncates at word boundary
     */
    private fun generateConversationTitle(message: String): String {
        // Remove emojis and extra whitespace
        val cleaned = message.replace("[^\\p{L}\\p{N}\\p{P}\\p{Z}]".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
        
        if (cleaned.length <= 40) return cleaned
        
        // Truncate at word boundary
        val truncated = cleaned.substring(0, 40)
        val lastSpace = truncated.lastIndexOf(' ')
        
        return if (lastSpace > 20) {
            "${truncated.substring(0, lastSpace)}..."
        } else {
            "${truncated}..."
        }
    }
    
    private fun sendMessageToConversation(message: String) {
        val conversationId = activeConversationId ?: return
        
        // Collect all file URIs from image preview container
        val fileUris = mutableListOf<Uri>()
        for (i in 0 until binding.imagePreviewContainer.childCount) {
            val view = binding.imagePreviewContainer.getChildAt(i)
            // Get URI from view tag
            (view.tag as? Uri)?.let { fileUris.add(it) }
        }
        
        // Show images/files in chat with loading indicators
        val hasFiles = fileUris.isNotEmpty()
        val mediaViews = mutableListOf<View>()
        
        if (hasFiles) {
            fileUris.forEach { uri ->
                val mediaView = addMediaMessageBubble(uri, isUploading = true)
                mediaViews.add(mediaView)
            }
        }
        
        // Add text message to the chat UI immediately (if any)
        if (message.isNotEmpty()) {
            addMessageBubble(message, isUser = true)
        }
        
        // Clear input field and previews
        binding.etChatInput.text.clear()
        binding.imagePreviewContainer.removeAllViews()
        binding.imagePreviewScroll.visibility = View.GONE
        updateSendButtonIcon()
        
        // Scroll to bottom
        binding.chatScroll.post {
            binding.chatScroll.fullScroll(View.FOCUS_DOWN)
        }
        
        // Upload to Firebase in background
        fragmentScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    chatRepository.sendMessage(
                        conversationId = conversationId,
                        messageText = message,
                        fileUris = fileUris,
                        onUploadProgress = { current, total ->
                            // Could update upload progress here
                        }
                    )
                }
                
                // Update media bubbles to show upload complete
                if (result.isSuccess && hasFiles) {
                    mediaViews.forEach { view ->
                        updateMediaBubbleStatus(view, isUploading = false, success = true)
                    }
                } else if (result.isFailure) {
                    mediaViews.forEach { view ->
                        updateMediaBubbleStatus(view, isUploading = false, success = false)
                    }
                }
                
                // Refresh conversation list in drawer
                (activity as? ContainerActivity)?.loadConversations()
                
                // Get AI response if there's a text message
                if (message.isNotEmpty()) {
                    getAIResponse(message, conversationId)
                }
            } catch (e: Exception) {
                mediaViews.forEach { view ->
                    updateMediaBubbleStatus(view, isUploading = false, success = false)
                }
            }
        }
    }
    
    /**
     * Get AI response from Gemini and display it
     */
    private fun getAIResponse(userMessage: String, conversationId: String) {
        // Show typing indicator
        val typingBubble = addTypingIndicator()
        
        fragmentScope.launch {
            try {
                val result = GeminiService.sendMessage(userMessage)
                
                // Remove typing indicator
                binding.messagesContainer.removeView(typingBubble)
                
                if (result.isSuccess) {
                    val aiResponse = result.getOrNull() ?: "I couldn't respond. Please try again."
                    
                    // Display AI response in chat
                    addMessageBubble(aiResponse, isUser = false)
                    
                    // Scroll to bottom
                    binding.chatScroll.post {
                        binding.chatScroll.fullScroll(View.FOCUS_DOWN)
                    }
                    
                    // Save AI response to Firebase history
                    fragmentScope.launch {
                        chatRepository.saveAIMessage(conversationId, aiResponse)
                    }
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    addMessageBubble("Sorry, I encountered an error: $errorMessage", isUser = false)
                }
            } catch (e: Exception) {
                // Remove typing indicator on error
                binding.messagesContainer.removeView(typingBubble)
                addMessageBubble("Sorry, I couldn't connect to the AI service.", isUser = false)
            }
        }
    }
    
    /**
     * Add typing indicator bubble
     */
    private fun addTypingIndicator(): View {
        val density = resources.displayMetrics.density
        
        val cardView = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.START
                setMargins(
                    (8 * density).toInt(),
                    (4 * density).toInt(),
                    (48 * density).toInt(),
                    (4 * density).toInt()
                )
            }
            radius = 20f * density
            cardElevation = 4f * density
            setCardBackgroundColor(android.graphics.Color.WHITE)
        }
        
        val textView = android.widget.TextView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
            text = "Al-Aalim is typing..."
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_medium))
            setTypeface(null, android.graphics.Typeface.ITALIC)
            setPadding(
                (16 * density).toInt(),
                (12 * density).toInt(),
                (16 * density).toInt(),
                (12 * density).toInt()
            )
        }
        
        cardView.addView(textView)
        binding.messagesContainer.addView(cardView)
        
        // Scroll to show typing indicator
        binding.chatScroll.post {
            binding.chatScroll.fullScroll(View.FOCUS_DOWN)
        }
        
        return cardView
    }
    
    /**
     * Set the active conversation ID
     */
    fun setActiveConversation(conversationId: String?) {
        this.activeConversationId = conversationId
    }
    
    /**
     * Load messages for a specific conversation
     */
    fun loadConversationMessages(conversationId: String) {
        this.activeConversationId = conversationId
        clearCurrentChat()
        binding.welcomeContainer.visibility = View.GONE
        
        fragmentScope.launch {
            val result = withContext(Dispatchers.IO) {
                chatRepository.getMessagesForConversation(conversationId)
            }
            
            if (result.isSuccess) {
                val messages = result.getOrNull() ?: emptyList()
                messages.forEach { msg ->
                    // Display attachments (images/videos/files) first
                    if (msg.attachments.isNotEmpty()) {
                        msg.attachments.forEach { attachment ->
                            addMediaMessageBubbleFromUrl(
                                url = attachment.url,
                                mimeType = attachment.mimeType,
                                fileName = attachment.fileName,
                                isUser = msg.isUser
                            )
                        }
                    }
                    // Display text message if present
                    if (msg.message.isNotEmpty()) {
                        addMessageBubble(msg.message, msg.isUser)
                    }
                }
                
                // Update Gemini AI context with these messages
                GeminiService.setChatHistory(messages)
                
                // Scroll to bottom after loading
                binding.chatScroll.post {
                    binding.chatScroll.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }
    
    /**
     * Clear current chat UI
     */
    fun clearCurrentChat() {
        binding.messagesContainer.removeAllViews()
        binding.welcomeContainer.visibility = View.VISIBLE
    }
    
    private fun addMessageBubble(message: String, isUser: Boolean) {
        val cardView = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Align to end for user messages, start for AI
                gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
                setMargins(
                    if (isUser) (48 * resources.displayMetrics.density).toInt() else (8 * resources.displayMetrics.density).toInt(),
                    (4 * resources.displayMetrics.density).toInt(),
                    if (isUser) (8 * resources.displayMetrics.density).toInt() else (48 * resources.displayMetrics.density).toInt(),
                    (4 * resources.displayMetrics.density).toInt()
                )
            }
            radius = 20f * resources.displayMetrics.density
            cardElevation = 4f * resources.displayMetrics.density
            setCardBackgroundColor(
                if (isUser) ContextCompat.getColor(requireContext(), R.color.primary_teal)
                else android.graphics.Color.WHITE
            )
        }
        
        val textView = android.widget.TextView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
            text = message
            textSize = 15f
            setTextColor(
                if (isUser) android.graphics.Color.WHITE
                else ContextCompat.getColor(requireContext(), R.color.text_dark)
            )
            setPadding(
                (16 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt()
            )
            maxWidth = (250 * resources.displayMetrics.density).toInt()
        }
        
        cardView.addView(textView)
        binding.messagesContainer.addView(cardView)
    }
    
    /**
     * Add image/video/file message bubble with optional loading indicator
     */
    private fun addMediaMessageBubble(uri: Uri, isUploading: Boolean): View {
        val density = resources.displayMetrics.density
        val maxWidth = (250 * density).toInt()
        val imageHeight = (200 * density).toInt()
        
        // Check if it's an image or video
        val mimeType = requireContext().contentResolver.getType(uri)
        val isImage = mimeType?.startsWith("image/") == true
        val isVideo = mimeType?.startsWith("video/") == true
        
        // Create main container card
        val cardView = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.END // User messages align right
                setMargins(
                    (48 * density).toInt(),
                    (4 * density).toInt(),
                    (8 * density).toInt(),
                    (4 * density).toInt()
                )
            }
            radius = 16f * density
            cardElevation = 4f * density
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_teal))
            tag = uri
        }
        
        // Create frame layout to hold media and overlay
        val frameLayout = android.widget.FrameLayout(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                maxWidth,
                imageHeight  // Fixed height to match image so overlay covers fully
            )
        }
        
        // Create image view for the media
        val imageView = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                maxWidth,
                imageHeight
            )
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            
            if (isImage) {
                setImageURI(uri)
            } else if (isVideo) {
                // Load video thumbnail
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(requireContext(), uri)
                    val bitmap = retriever.frameAtTime
                    retriever.release()
                    if (bitmap != null) {
                        setImageBitmap(bitmap)
                    } else {
                        setBackgroundColor(android.graphics.Color.parseColor("#333333"))
                    }
                } catch (e: Exception) {
                    setBackgroundColor(android.graphics.Color.parseColor("#333333"))
                }
            } else {
                // For other files, show a placeholder
                setBackgroundColor(android.graphics.Color.parseColor("#2A3D3D"))
                scaleType = android.widget.ImageView.ScaleType.CENTER
                setImageResource(R.drawable.ic_files)
                setColorFilter(android.graphics.Color.WHITE)
            }
        }
        
        frameLayout.addView(imageView)
        
        // Add play button overlay for videos
        if (isVideo) {
            val playIcon = android.widget.ImageView(requireContext()).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    (48 * density).toInt(),
                    (48 * density).toInt()
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
                setImageResource(android.R.drawable.ic_media_play)
                setColorFilter(android.graphics.Color.WHITE)
                alpha = 0.9f
            }
            frameLayout.addView(playIcon)
        }
        
        // Add loading overlay if uploading
        if (isUploading) {
            val overlayLayout = android.widget.FrameLayout(requireContext()).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                // White transparent overlay like reference image
                setBackgroundColor(android.graphics.Color.parseColor("#B3FFFFFF"))
                tag = "upload_overlay"
            }
            
            val progressBar = android.widget.ProgressBar(requireContext()).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    (40 * density).toInt(),
                    (40 * density).toInt()
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
                isIndeterminate = true
                // Set progress bar color to match app theme (teal)
                indeterminateTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.primary_teal)
                )
                tag = "progress_bar"
            }
            
            val uploadText = android.widget.TextView(requireContext()).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.CENTER
                    topMargin = (50 * density).toInt()
                }
                text = "Uploading..."
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_teal))
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                tag = "upload_text"
            }
            
            overlayLayout.addView(progressBar)
            overlayLayout.addView(uploadText)
            frameLayout.addView(overlayLayout)
        }
        
        cardView.addView(frameLayout)
        binding.messagesContainer.addView(cardView)
        
        return cardView
    }
    
    /**
     * Update media bubble upload status
     */
    private fun updateMediaBubbleStatus(view: View, isUploading: Boolean, success: Boolean) {
        if (view !is androidx.cardview.widget.CardView) return
        
        val frameLayout = view.getChildAt(0) as? android.widget.FrameLayout ?: return
        val overlay = frameLayout.findViewWithTag<View>("upload_overlay")
        
        if (!isUploading) {
            // Remove loading overlay
            overlay?.let { frameLayout.removeView(it) }
            
            // If failed, add error indicator
            if (!success) {
                val density = resources.displayMetrics.density
                val errorOverlay = android.widget.FrameLayout(requireContext()).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.parseColor("#80FF0000"))
                }
                
                val errorText = android.widget.TextView(requireContext()).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = android.view.Gravity.CENTER
                    }
                    text = "❌ Upload failed"
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 14f
                }
                
                errorOverlay.addView(errorText)
                frameLayout.addView(errorOverlay)
            }
        }
    }
    
    /**
     * Add image/video/file message bubble from Firebase Storage URL (for loading from history)
     */
    private fun addMediaMessageBubbleFromUrl(
        url: String,
        mimeType: String,
        fileName: String,
        isUser: Boolean
    ) {
        val density = resources.displayMetrics.density
        val maxWidth = (250 * density).toInt()
        val imageHeight = (200 * density).toInt()
        
        // Check media type from mimeType
        val isImage = mimeType.startsWith("image/")
        val isVideo = mimeType.startsWith("video/")
        
        // Create main container card
        val cardView = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
                setMargins(
                    if (isUser) (48 * density).toInt() else (8 * density).toInt(),
                    (4 * density).toInt(),
                    if (isUser) (8 * density).toInt() else (48 * density).toInt(),
                    (4 * density).toInt()
                )
            }
            radius = 16f * density
            cardElevation = 4f * density
            setCardBackgroundColor(
                if (isUser) ContextCompat.getColor(requireContext(), R.color.primary_teal)
                else android.graphics.Color.WHITE
            )
        }
        
        // Create frame layout to hold media
        val frameLayout = android.widget.FrameLayout(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                maxWidth,
                imageHeight
            )
        }
        
        // Create image view for the media
        val imageView = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                maxWidth,
                imageHeight
            )
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(android.graphics.Color.parseColor("#2A3D3D"))
        }
        
        frameLayout.addView(imageView)
        
        // Create loading overlay (white transparent with teal spinner)
        val loadingOverlay = android.widget.FrameLayout(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.parseColor("#B3FFFFFF"))
            tag = "loading_overlay"
        }
        
        val progressBar = android.widget.ProgressBar(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (40 * density).toInt(),
                (40 * density).toInt()
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            isIndeterminate = true
            indeterminateTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary_teal)
            )
        }
        
        val loadingText = android.widget.TextView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
                topMargin = (50 * density).toInt()
            }
            text = "Loading..."
            setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_teal))
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        loadingOverlay.addView(progressBar)
        loadingOverlay.addView(loadingText)
        
        // Add loading overlay for images/videos
        if (isImage || isVideo) {
            frameLayout.addView(loadingOverlay)
        }
        
        // Load image from URL using Coil
        if (isImage || isVideo) {
            coil.ImageLoader(requireContext()).enqueue(
                coil.request.ImageRequest.Builder(requireContext())
                    .data(url)
                    .target(
                        onStart = { placeholder ->
                            // Loading overlay is already visible
                        },
                        onSuccess = { result ->
                            imageView.setImageDrawable(result)
                            // Remove loading overlay
                            frameLayout.removeView(loadingOverlay)
                        },
                        onError = { error ->
                            // Remove loading overlay and show error
                            frameLayout.removeView(loadingOverlay)
                            imageView.setImageResource(R.drawable.ic_photos)
                            imageView.setColorFilter(android.graphics.Color.WHITE)
                            imageView.scaleType = android.widget.ImageView.ScaleType.CENTER
                        }
                    )
                    .build()
            )
        } else {
            // For other files, show file icon
            imageView.scaleType = android.widget.ImageView.ScaleType.CENTER
            imageView.setImageResource(R.drawable.ic_files)
            imageView.setColorFilter(android.graphics.Color.WHITE)
        }
        
        // Add play button overlay for videos
        if (isVideo) {
            val playIcon = android.widget.ImageView(requireContext()).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    (48 * density).toInt(),
                    (48 * density).toInt()
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
                setImageResource(android.R.drawable.ic_media_play)
                setColorFilter(android.graphics.Color.WHITE)
                alpha = 0.9f
            }
            frameLayout.addView(playIcon)
        }
        
        // Add file name label for non-image files
        if (!isImage && !isVideo) {
            val fileNameLabel = android.widget.TextView(requireContext()).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.BOTTOM
                }
                text = fileName
                setTextColor(android.graphics.Color.WHITE)
                textSize = 11f
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(
                    (8 * density).toInt(),
                    (4 * density).toInt(),
                    (8 * density).toInt(),
                    (8 * density).toInt()
                )
                setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
            }
            frameLayout.addView(fileNameLabel)
        }
        
        cardView.addView(frameLayout)
        binding.messagesContainer.addView(cardView)
    }
    
    private fun showAttachmentOptions() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_attachment, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Set up click listeners for each option
        bottomSheetView.findViewById<View>(R.id.option_camera).setOnClickWithAnimation {
            bottomSheetDialog.dismiss()
            launchCamera()
        }
        
        bottomSheetView.findViewById<View>(R.id.option_photos).setOnClickWithAnimation {
            bottomSheetDialog.dismiss()
            launchGallery()
        }
        
        bottomSheetView.findViewById<View>(R.id.option_files).setOnClickWithAnimation {
            bottomSheetDialog.dismiss()
            openFilePicker()
        }
        
        bottomSheetDialog.show()
    }
    
    private fun launchCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            imageUris.add(photoUri)
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                requireContext(),
                "Error opening camera: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun launchGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            else -> {
                requestGalleryPermissionLauncher.launch(permission)
            }
        }
    }
    
    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }
    
    private fun openFilePicker() {
        pickFileLauncher.launch(arrayOf("*/*"))
    }
    
    private fun addFilePreview(uri: Uri) {
        // Show the scroll view if it's hidden
        binding.imagePreviewScroll.visibility = View.VISIBLE
        
        val itemWidth = (90 * resources.displayMetrics.density).toInt()
        val itemHeight = (90 * resources.displayMetrics.density).toInt()
        
        // Get file name and document type
        val fileName = getFileName(uri)
        val documentType = recognizeDocumentType(uri)
        
        // Create a CardView for the file preview
        val cardView = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                itemWidth,
                itemHeight
            ).apply {
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
            radius = 12f * resources.displayMetrics.density
            cardElevation = 2f * resources.displayMetrics.density
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_background))
            tag = uri // Store URI for later retrieval
        }
        
        // Create FrameLayout to hold icon, name and close button
        val frameLayout = android.widget.FrameLayout(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Create container for file icon, type label, and name
        val contentLayout = android.widget.LinearLayout(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }
        
        // File icon with document type (icons have colors built-in)
        val iconView = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                (32 * resources.displayMetrics.density).toInt(),
                (32 * resources.displayMetrics.density).toInt()
            )
            setImageResource(documentType.iconRes)
            // No color filter needed - icons have colors built-in
        }
        
        // Document type label (e.g., "PDF", "Word", "Excel")
        val typeLabel = android.widget.TextView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = documentType.typeName
            textSize = 9f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(documentType.iconColor)
            gravity = android.view.Gravity.CENTER
        }
        
        // File name text
        val nameView = android.widget.TextView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = fileName
            textSize = 9f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = android.view.Gravity.CENTER
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dark))
        }
        
        contentLayout.addView(iconView)
        contentLayout.addView(typeLabel)
        contentLayout.addView(nameView)
        
        // Create close button CardView
        val closeButtonCard = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (24 * resources.displayMetrics.density).toInt(),
                (24 * resources.displayMetrics.density).toInt()
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                setMargins(4, 4, 4, 4)
            }
            radius = 12f * resources.displayMetrics.density
            cardElevation = 2f * resources.displayMetrics.density
            setCardBackgroundColor(android.graphics.Color.WHITE)
        }
        
        // Create close button ImageView
        val closeButton = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(4, 4, 4, 4)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(android.graphics.Color.BLACK)
            setOnClickListener {
                binding.imagePreviewContainer.removeView(cardView)
                if (binding.imagePreviewContainer.childCount == 0) {
                    binding.imagePreviewScroll.visibility = View.GONE
                }
                updateSendButtonIcon()
            }
        }
        
        // Assemble the views
        closeButtonCard.addView(closeButton)
        frameLayout.addView(contentLayout)
        frameLayout.addView(closeButtonCard)
        cardView.addView(frameLayout)
        
        // Add to container
        binding.imagePreviewContainer.addView(cardView)
        
        // Update send button icon to show upward arrow
        updateSendButtonIcon()
    }
    
    private fun getFileName(uri: Uri): String {
        var name = "File"
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
    
    // Document type information class
    private data class DocumentType(
        val typeName: String,
        val iconRes: Int,
        val iconColor: Int
    )
    
    // Recognize document type from URI
    private fun recognizeDocumentType(uri: Uri): DocumentType {
        val mimeType = requireContext().contentResolver.getType(uri)
        val fileName = getFileName(uri).lowercase()
        
        // Determine document type based on MIME type or file extension
        // Using dedicated PNG icons for supported file types
        return when {
            // PDF - using ic_pdf.png
            mimeType == "application/pdf" || fileName.endsWith(".pdf") -> 
                DocumentType("PDF", R.drawable.ic_pdf, android.graphics.Color.parseColor("#E53935"))
            
            // Word Documents - using ic_word.png
            mimeType == "application/msword" || 
            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
            fileName.endsWith(".doc") || fileName.endsWith(".docx") -> 
                DocumentType("DOC", R.drawable.ic_word, android.graphics.Color.parseColor("#2196F3"))
            
            // Excel Spreadsheets - using ic_excel.png
            mimeType == "application/vnd.ms-excel" ||
            mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
            fileName.endsWith(".xls") || fileName.endsWith(".xlsx") -> 
                DocumentType("XLS", R.drawable.ic_excel, android.graphics.Color.parseColor("#217346"))
            
            // PowerPoint Presentations - using ic_powerpoint.png
            mimeType == "application/vnd.ms-powerpoint" ||
            mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" ||
            fileName.endsWith(".ppt") || fileName.endsWith(".pptx") -> 
                DocumentType("PPT", R.drawable.ic_powerpoint, android.graphics.Color.parseColor("#D24726"))
            
            // Text Files - using generic file icon
            mimeType?.startsWith("text/") == true ||
            fileName.endsWith(".txt") || fileName.endsWith(".csv") || fileName.endsWith(".json") ||
            fileName.endsWith(".xml") || fileName.endsWith(".html") || fileName.endsWith(".htm") -> 
                DocumentType("Text", R.drawable.ic_files, android.graphics.Color.parseColor("#607D8B"))
            
            // Images - using ic_photos
            mimeType?.startsWith("image/") == true ||
            fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") ||
            fileName.endsWith(".gif") || fileName.endsWith(".webp") || fileName.endsWith(".bmp") -> 
                DocumentType("Image", R.drawable.ic_photos, android.graphics.Color.parseColor("#9C27B0"))
            
            // Videos - using ic_camera
            mimeType?.startsWith("video/") == true ||
            fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".avi") ||
            fileName.endsWith(".mov") || fileName.endsWith(".webm") -> 
                DocumentType("Video", R.drawable.ic_camera, android.graphics.Color.parseColor("#F44336"))
            
            // Audio - using ic_audio.png
            mimeType?.startsWith("audio/") == true ||
            fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".ogg") ||
            fileName.endsWith(".flac") || fileName.endsWith(".aac") -> 
                DocumentType("Audio", R.drawable.ic_audio, android.graphics.Color.parseColor("#2196F3"))
            
            // Archives - using ic_archive.png
            mimeType == "application/zip" || mimeType == "application/x-rar-compressed" ||
            mimeType == "application/x-7z-compressed" || mimeType == "application/gzip" ||
            fileName.endsWith(".zip") || fileName.endsWith(".rar") || fileName.endsWith(".7z") ||
            fileName.endsWith(".tar") || fileName.endsWith(".gz") -> 
                DocumentType("ZIP", R.drawable.ic_archive, android.graphics.Color.parseColor("#FFA000"))
            
            // APK (Android Package) - using generic file icon
            mimeType == "application/vnd.android.package-archive" || fileName.endsWith(".apk") -> 
                DocumentType("APK", R.drawable.ic_files, android.graphics.Color.parseColor("#8BC34A"))
            
            // Default unknown type
            else -> DocumentType("File", R.drawable.ic_files, ContextCompat.getColor(requireContext(), R.color.primary_teal))
        }
    }
    
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }
    
    private fun addVideoPreview(uri: Uri) {
        // Show the scroll view if it's hidden
        binding.imagePreviewScroll.visibility = View.VISIBLE
        
        val itemWidth = (90 * resources.displayMetrics.density).toInt()
        val itemHeight = (90 * resources.displayMetrics.density).toInt()
        
        // Create a CardView for the video preview
        val cardView = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                itemWidth,
                itemHeight
            ).apply {
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
            radius = 12f * resources.displayMetrics.density
            cardElevation = 2f * resources.displayMetrics.density
            tag = uri // Store URI for later retrieval
        }
        
        // Create FrameLayout to hold thumbnail, play icon, and close button
        val frameLayout = android.widget.FrameLayout(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Create ImageView for the video thumbnail
        val thumbnailView = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(android.graphics.Color.parseColor("#333333"))
            
            // Try to load video thumbnail
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(requireContext(), uri)
                val bitmap = retriever.frameAtTime
                retriever.release()
                if (bitmap != null) {
                    setImageBitmap(bitmap)
                } else {
                    // Fallback: show video icon if thumbnail can't be generated
                    scaleType = android.widget.ImageView.ScaleType.CENTER
                }
            } catch (e: Exception) {
                // Fallback: show video icon if error occurs

                scaleType = android.widget.ImageView.ScaleType.CENTER
            }
        }
        
        // Create play button overlay
        val playButton = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (32 * resources.displayMetrics.density).toInt(),
                (32 * resources.displayMetrics.density).toInt()
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            setImageResource(android.R.drawable.ic_media_play)
            setColorFilter(android.graphics.Color.WHITE)
            alpha = 0.9f
        }
        
        // Create close button CardView
        val closeButtonCard = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (24 * resources.displayMetrics.density).toInt(),
                (24 * resources.displayMetrics.density).toInt()
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                setMargins(4, 4, 4, 4)
            }
            radius = 12f * resources.displayMetrics.density
            cardElevation = 2f * resources.displayMetrics.density
            setCardBackgroundColor(android.graphics.Color.WHITE)
        }
        
        // Create close button ImageView
        val closeButton = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(4, 4, 4, 4)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(android.graphics.Color.BLACK)
            setOnClickListener {
                binding.imagePreviewContainer.removeView(cardView)
                if (binding.imagePreviewContainer.childCount == 0) {
                    binding.imagePreviewScroll.visibility = View.GONE
                }
                updateSendButtonIcon()
            }
        }
        
        // Assemble the views
        closeButtonCard.addView(closeButton)
        frameLayout.addView(thumbnailView)
        frameLayout.addView(playButton)
        frameLayout.addView(closeButtonCard)
        cardView.addView(frameLayout)
        
        // Add to container
        binding.imagePreviewContainer.addView(cardView)
        
        // Update send button icon to show upward arrow
        updateSendButtonIcon()
    }
    
    private fun addImagePreview(uri: Uri) {
        // Show the scroll view if it's hidden
        binding.imagePreviewScroll.visibility = View.VISIBLE
        
        val imageWidth = (90 * resources.displayMetrics.density).toInt() // 90dp width
        val imageHeight = (90 * resources.displayMetrics.density).toInt() // 90dp height
        
        // Create a CardView for the image preview
        val cardView = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                imageWidth,
                imageHeight
            ).apply {
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
            radius = 12f * resources.displayMetrics.density
            cardElevation = 2f * resources.displayMetrics.density
            tag = uri // Store URI for later retrieval
        }
        
        // Create FrameLayout to hold image and close button
        val frameLayout = android.widget.FrameLayout(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Create ImageView for the preview
        val imageView = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setImageURI(uri)
            tag = uri // Store URI in tag for removal
        }
        
        // Create close button CardView
        val closeButtonCard = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (24 * resources.displayMetrics.density).toInt(),
                (24 * resources.displayMetrics.density).toInt()
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                setMargins(4, 4, 4, 4)
            }
            radius = 12f * resources.displayMetrics.density
            cardElevation = 2f * resources.displayMetrics.density
            setCardBackgroundColor(android.graphics.Color.WHITE)
        }
        
        // Create close button ImageView
        val closeButton = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(4, 4, 4, 4)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(android.graphics.Color.BLACK)
            setOnClickListener {
                removeImagePreview(uri, cardView)
            }
        }
        
        // Assemble the views
        closeButtonCard.addView(closeButton)
        frameLayout.addView(imageView)
        frameLayout.addView(closeButtonCard)
        cardView.addView(frameLayout)
        
        // Add to container
        binding.imagePreviewContainer.addView(cardView)
        
        // Update send button icon to show upward arrow
        updateSendButtonIcon()
    }
    
    private fun removeImagePreview(uri: Uri, cardView: androidx.cardview.widget.CardView) {
        // Remove from list
        imageUris.remove(uri)
        
        // Remove from UI
        binding.imagePreviewContainer.removeView(cardView)
        
        // Hide scroll view if no images
        if (imageUris.isEmpty()) {
            binding.imagePreviewScroll.visibility = View.GONE
        }
        
        // Update send button icon
        updateSendButtonIcon()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

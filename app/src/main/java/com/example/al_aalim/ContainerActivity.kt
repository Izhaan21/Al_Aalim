package com.example.al_aalim

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.al_aalim.databinding.ActivityContainerBinding
import com.example.al_aalim.utils.LocationManager
import com.example.al_aalim.utils.ProfileManager
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation
import com.example.al_aalim.auth.FirebaseAuthManager
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.al_aalim.adapters.ChatConversationAdapter
import com.example.al_aalim.models.ChatConversation
import com.example.al_aalim.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContainerBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private lateinit var authManager: FirebaseAuthManager
    
    // Chat conversation management
    private lateinit var chatRepository: ChatRepository
    private lateinit var conversationAdapter: ChatConversationAdapter
    private var allConversations: List<ChatConversation> = emptyList()
    var activeConversationId: String? = null
    private val activityScope = CoroutineScope(Dispatchers.Main)
    private lateinit var pagerAdapter: ScreenPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display - REQUIRED for keyboard insets to work
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize Firebase Auth Manager
        authManager = FirebaseAuthManager(this)

        // Initialize View Binding
        binding = ActivityContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configure system bars for immersive mode
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        // Set transparent system bars
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // For Android 10+ (API 29+), enable gesture navigation edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Set window soft input mode - adjustResize keeps top elements fixed
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        )

        // Hide action bar for cleaner look
        supportActionBar?.hide()

        // Handle keyboard visibility to hide bottom navigation and disable swipe
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Check if keyboard is visible
            val isKeyboardVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())

            // Hide bottom navigation when keyboard is visible
            binding.bottomNavigation.visibility = if (isKeyboardVisible) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }

            // Disable ViewPager2 swipe when keyboard is visible
            binding.viewPager.isUserInputEnabled = !isKeyboardVisible

            windowInsets
        }

        // Setup ViewPager2
        setupViewPager()

        // Setup bottom navigation
        setupNavigation()

        // Set Home as active initially
        setActiveNavigation(0)

        // Setup drawer search focus behavior
        setupDrawerSearchFocus()

        // Enable drawer swipe from left edge
        setupDrawerSwipe()
        
        // Setup drawer button click handlers
        setupDrawerButtons()
        
        // Load user name and profile image in drawer
        loadDrawerUserName()
        loadDrawerProfileImage()
        
        // Pre-fetch location for instant Qibla display
        LocationManager.initialize(this)
    }
    
    private fun setupDrawerButtons() {
    // Initialize chat repository
    chatRepository = ChatRepository(this)
    
    // Setup RecyclerView for chat history
    setupChatHistoryRecyclerView()
    
    // New Chat button creates a new conversation
    val newChatButton = binding.drawerLayout.findViewById<android.widget.LinearLayout>(R.id.btn_new_chat)
    newChatButton?.setOnClickWithAnimation {
        createNewConversation()
    }
    
    // Settings button opens SettingsActivity
    val settingsButton = binding.drawerLayout.findViewById<android.widget.LinearLayout>(R.id.btn_settings)
    settingsButton?.setOnClickWithAnimation {
        val intent = android.content.Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        closeDrawer()
    }
    
    // Profile avatar opens ProfilePhotoActivity
    val profileAvatar = binding.drawerLayout.findViewById<androidx.cardview.widget.CardView>(R.id.drawer_avatar_card)
    profileAvatar?.setOnClickWithAnimation {
        val intent = android.content.Intent(this, ProfilePhotoActivity::class.java)
        startActivity(intent)
        closeDrawer()
    }
    
    // Profile header (excluding avatar) opens AccountActivity
    val profileHeader = binding.drawerLayout.findViewById<android.widget.LinearLayout>(R.id.drawer_profile_header)
    profileHeader?.setOnClickWithAnimation {
        val intent = android.content.Intent(this, AccountActivity::class.java)
        startActivity(intent)
        closeDrawer()
    }
    
    // Search bar filter
    val searchBar = binding.drawerLayout.findViewById<android.widget.EditText>(R.id.et_drawer_search)
    searchBar?.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            conversationAdapter.filter(s.toString(), allConversations)
        }
        override fun afterTextChanged(s: android.text.Editable?) {}
    })
    
    // Delete All button
    val deleteAllButton = binding.drawerLayout.findViewById<android.widget.TextView>(R.id.btn_delete_all_chats)
    deleteAllButton?.setOnClickListener {
        showDeleteAllConversationsDialog()
    }
    
    // Update delete button visibility
    updateDeleteButtonVisibility()
}

private fun updateDeleteButtonVisibility() {
    val deleteAllButton = binding.drawerLayout.findViewById<android.widget.TextView>(R.id.btn_delete_all_chats)
    // Show delete button only if there are conversations
    deleteAllButton?.visibility = if (allConversations.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
}

private fun showDeleteAllConversationsDialog() {
    if (allConversations.isEmpty()) {
        return
    }
    
    com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
        .setTitle("Delete All Chats")
        .setMessage("Are you sure you want to delete all ${allConversations.size} conversations? This action cannot be undone.")
        .setPositiveButton("Delete All") { dialog, _ ->
            deleteAllConversations()
            dialog.dismiss()
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

private fun deleteAllConversations() {
    activityScope.launch {
        try {
            android.util.Log.d("ContainerActivity", "Attempting to delete all conversations")
            val result = withContext(Dispatchers.IO) {
                chatRepository.deleteAllConversations()
            }
            
            if (result.isSuccess) {
                android.util.Log.d("ContainerActivity", "Successfully deleted all conversations")
                // Clear active conversation
                activeConversationId = null
                val homeFragment = supportFragmentManager.findFragmentByTag("f0") as? HomeFragment
                homeFragment?.clearCurrentChat()
                homeFragment?.setActiveConversation(null)
                
                // Clear the list
                allConversations = emptyList()
                conversationAdapter.submitList(emptyList())
                
                // Update delete button visibility
                updateDeleteButtonVisibility()
                
                android.widget.Toast.makeText(this@ContainerActivity, "All chats deleted", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                android.util.Log.e("ContainerActivity", "Failed to delete all conversations: $error")
                android.widget.Toast.makeText(this@ContainerActivity, "Failed to delete all chats: $error", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("ContainerActivity", "Exception during delete all: ${e.message}")
            android.widget.Toast.makeText(this@ContainerActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

private fun setupChatHistoryRecyclerView() {
    val recyclerView = binding.drawerLayout.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_chat_history)
    
    conversationAdapter = ChatConversationAdapter(
        onConversationClick = { conversation ->
            // Load selected conversation
            loadConversation(conversation)
        },
        onEditClick = { conversation ->
            // Show rename dialog
            showRenameConversationDialog(conversation)
        },
        onDeleteClick = { conversation ->
            // Show delete confirmation dialog
            showDeleteConversationDialog(conversation)
        }
    )
    
    recyclerView?.apply {
        layoutManager = LinearLayoutManager(this@ContainerActivity)
        adapter = conversationAdapter
    }
}

private fun showRenameConversationDialog(conversation: ChatConversation) {
    val input = android.widget.EditText(this)
    input.setText(conversation.title)
    input.setSelection(conversation.title.length)
    
    val padding = (24 * resources.displayMetrics.density).toInt()
    val container = android.widget.FrameLayout(this)
    val params = android.widget.FrameLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    )
    params.setMargins(padding, padding / 2, padding, 0)
    input.layoutParams = params
    container.addView(input)

    com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
        .setTitle("Rename Chat")
        .setView(container)
        .setPositiveButton("Rename") { dialog, _ ->
            val newTitle = input.text.toString().trim()
            if (newTitle.isNotEmpty() && newTitle != conversation.title) {
                renameConversation(conversation.id, newTitle)
            }
            dialog.dismiss()
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

private fun renameConversation(conversationId: String, newTitle: String) {
    activityScope.launch {
        val result = withContext(Dispatchers.IO) {
            chatRepository.updateConversationTitle(conversationId, newTitle)
        }
        
        if (result.isSuccess) {
            val path = result.getOrNull() ?: ""
            loadConversations() // Reload to update UI
            android.widget.Toast.makeText(this@ContainerActivity, "Renamed in Firebase at: $path", android.widget.Toast.LENGTH_LONG).show()
        } else {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            android.widget.Toast.makeText(
                this@ContainerActivity,
                "Failed to rename: $error",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}

private fun showDeleteConversationDialog(conversation: ChatConversation) {
    android.app.AlertDialog.Builder(this)
        .setTitle("Delete Conversation")
        .setMessage("Are you sure you want to delete \"${conversation.title}\"? This will delete all messages and attachments.")
        .setPositiveButton("Delete") { dialog, _ ->
            deleteConversation(conversation)
            dialog.dismiss()
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

private fun deleteConversation(conversation: ChatConversation) {
    activityScope.launch {
        val result = withContext(Dispatchers.IO) {
            chatRepository.deleteConversation(conversation.id)
        }
        
        if (result.isSuccess) {
            // If deleted conversation was active, clear it
            if (activeConversationId == conversation.id) {
                activeConversationId = null
                val homeFragment = supportFragmentManager.findFragmentByTag("f0") as? HomeFragment
                homeFragment?.clearCurrentChat()
                homeFragment?.setActiveConversation(null)
            }
            
            // Reload conversations to update the list
            loadConversations()
        } else {
            android.widget.Toast.makeText(
                this@ContainerActivity,
                "Failed to delete conversation",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}

private fun createNewConversation() {
    // Don't create conversation yet - just clear chat and set active to null
    // Conversation will be created when user sends first message
    activeConversationId = null
    
    // Switch to home fragment and clear chat
    navigateToPage(0)
    val homeFragment = supportFragmentManager.findFragmentByTag("f0") as? HomeFragment
    homeFragment?.clearCurrentChat()
    homeFragment?.setActiveConversation(null)
    
    closeDrawer()
}

private fun loadConversation(conversation: ChatConversation) {
    activeConversationId = conversation.id
    
    // Switch to home fragment
    navigateToPage(0)
    
    // Load conversation messages
    val homeFragment = supportFragmentManager.findFragmentByTag("f0") as? HomeFragment
    homeFragment?.loadConversationMessages(conversation.id)
    homeFragment?.setActiveConversation(conversation.id)
    
    closeDrawer()
}

fun loadConversations() {
    activityScope.launch {
        val result = withContext(Dispatchers.IO) {
            chatRepository.getConversations()
        }
        
        if (result.isSuccess) {
            allConversations = result.getOrNull() ?: emptyList()
            android.util.Log.d("ContainerActivity", "Loaded ${allConversations.size} conversations")
            conversationAdapter.submitList(allConversations)
            
            // Update delete button visibility based on conversation count
            updateDeleteButtonVisibility()
            
            // If no active conversation and we have conversations, set the first one
            if (activeConversationId == null && allConversations.isNotEmpty()) {
                activeConversationId = allConversations.first().id
                val homeFragment = supportFragmentManager.findFragmentByTag("f0") as? HomeFragment
                homeFragment?.setActiveConversation(activeConversationId)
            }
        } else {
            android.util.Log.e("ContainerActivity", "Failed to load conversations: ${result.exceptionOrNull()?.message}")
        }
    }
}


    private fun setupDrawerSwipe() {
        // Ensure drawer is unlocked for swipe gestures
        binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)

        // Handle drawer state to sync main content position (ChatGPT effect)
        val drawerWidth = (280 * resources.displayMetrics.density)
        binding.drawerLayout.addDrawerListener(object :
            androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: android.view.View, slideOffset: Float) {
                // Slide main content as drawer opens
                binding.containerMain.translationX = drawerWidth * slideOffset
            }

            override fun onDrawerClosed(drawerView: android.view.View) {
                binding.containerMain.translationX = 0f
            }
        })
    }

    // Variables for swipe detection
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var isSwipingDrawer = false
    private var isEdgeSwipe = false

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        val drawerWidth = (280 * resources.displayMetrics.density)
        val minSwipeThreshold = 30 * resources.displayMetrics.density
        val edgeThreshold =
            80 * resources.displayMetrics.density // Only swipes starting from left 80dp edge

        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                swipeStartX = event.rawX
                swipeStartY = event.rawY
                isSwipingDrawer = false
                // Only allow drawer swipe if starting from left edge
                isEdgeSwipe = swipeStartX < edgeThreshold
            }

            android.view.MotionEvent.ACTION_MOVE -> {
                // Only process if swipe started from left edge
                if (!isEdgeSwipe) {
                    return super.dispatchTouchEvent(event)
                }

                val diffX = event.rawX - swipeStartX
                val diffY = event.rawY - swipeStartY

                // Start dragging if horizontal movement is dominant
                if (!isSwipingDrawer &&
                    Math.abs(diffX) > minSwipeThreshold &&
                    Math.abs(diffX) > Math.abs(diffY) * 2 &&
                    diffX > 0
                ) { // Only right swipe
                    isSwipingDrawer = true
                }

                // Handle right swipe to open drawer
                if (isSwipingDrawer && !binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.containerMain.translationX = diffX.coerceAtMost(drawerWidth)
                    return true
                }
            }

            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                if (isSwipingDrawer && isEdgeSwipe) {
                    val diffX = event.rawX - swipeStartX

                    if (diffX > drawerWidth * 0.3f) {
                        // Open drawer
                        openDrawer()
                    } else {
                        // Snap back
                        binding.containerMain.animate()
                            .translationX(0f)
                            .setDuration(200)
                            .start()
                    }
                    isSwipingDrawer = false
                    isEdgeSwipe = false
                    return true
                }
                isEdgeSwipe = false
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun setupDrawerSearchFocus() {
        // Get drawer views
        val drawerRoot =
            binding.drawerLayout.findViewById<android.widget.LinearLayout>(R.id.drawer_root)
        val profileHeader =
            binding.drawerLayout.findViewById<android.widget.LinearLayout>(R.id.drawer_profile_header)
        val searchEditText =
            binding.drawerLayout.findViewById<android.widget.EditText>(R.id.et_drawer_search)
        val searchBarCard =
            binding.drawerLayout.findViewById<androidx.cardview.widget.CardView>(R.id.search_bar_card)
        val newChatButton =
            binding.drawerLayout.findViewById<android.widget.LinearLayout>(R.id.btn_new_chat)
        val proCard =
            binding.drawerLayout.findViewById<android.widget.LinearLayout>(R.id.drawer_pro_card)

        // Calculate status bar height for top margin when header is hidden
        val statusBarHeight = resources.getDimensionPixelSize(
            resources.getIdentifier("status_bar_height", "dimen", "android")
        ).takeIf { it > 0 } ?: (24 * resources.displayMetrics.density).toInt()

        val originalTopMargin = (8 * resources.displayMetrics.density).toInt()

        searchEditText?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Expand drawer to full screen width
                val params = drawerRoot?.layoutParams
                params?.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                drawerRoot?.layoutParams = params

                // Hide profile header, new chat button, and pro card
                profileHeader?.visibility = android.view.View.GONE
                newChatButton?.visibility = android.view.View.GONE
                proCard?.visibility = android.view.View.GONE

                // Add top margin to search bar to account for hidden header/status bar
                (searchBarCard?.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { searchParams ->
                    searchParams.topMargin = statusBarHeight + originalTopMargin
                    searchBarCard.layoutParams = searchParams
                }
            } else {
                // Restore drawer to original width (280dp)
                val params = drawerRoot?.layoutParams
                params?.width = (280 * resources.displayMetrics.density).toInt()
                drawerRoot?.layoutParams = params

                // Show profile header, new chat button, and pro card
                profileHeader?.visibility = android.view.View.VISIBLE
                newChatButton?.visibility = android.view.View.VISIBLE
                proCard?.visibility = android.view.View.VISIBLE

                // Restore original top margin for search bar
                (searchBarCard?.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { searchParams ->
                    searchParams.topMargin = originalTopMargin
                    searchBarCard.layoutParams = searchParams
                }
            }
        }

        // Clear focus when drawer closes
        binding.drawerLayout.addDrawerListener(object :
            androidx.drawerlayout.widget.DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: android.view.View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: android.view.View) {}
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerClosed(drawerView: android.view.View) {
                searchEditText?.clearFocus()
                // Hide keyboard
                val imm =
                    getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(searchEditText?.windowToken, 0)
            }
        })
    }

    private fun setupViewPager() {
        pagerAdapter = ScreenPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // Disable user swipe input (we'll enable it after testing)
        binding.viewPager.isUserInputEnabled = true

        // Listen to page changes to update bottom navigation
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setActiveNavigation(position)
            }
        })
    }

    private fun setupNavigation() {
        // Home navigation
        binding.navHome.setOnClickWithAnimation {
            binding.viewPager.currentItem = 0
        }

        // Qibla navigation
        binding.navQibla.setOnClickWithAnimation {
            binding.viewPager.currentItem = 1
        }

        // Book/Quran navigation
        binding.navBook.setOnClickWithAnimation {
            binding.viewPager.currentItem = 2
        }

        // More navigation
        binding.navMore.setOnClickWithAnimation {
            binding.viewPager.currentItem = 3
        }
    }

    private fun setActiveNavigation(position: Int) {
        // Reset all - scale down to normal
        animateNavItem(binding.navHome, false)
        binding.navHome.isSelected = false
        binding.ivNavHome.isSelected = false
        binding.tvNavHome.isSelected = false

        animateNavItem(binding.navQibla, false)
        binding.navQibla.isSelected = false
        binding.ivNavQibla.isSelected = false
        binding.tvNavQibla.isSelected = false

        animateNavItem(binding.navBook, false)
        binding.navBook.isSelected = false
        binding.ivNavBook.isSelected = false
        binding.tvNavBook.isSelected = false

        // Reset More selection
        animateNavItem(binding.navMore, false)
        binding.navMore.isSelected = false
        binding.ivNavMore.isSelected = false
        binding.tvNavMore.isSelected = false

        // Set active based on position - scale up selected
        when (position) {
            0 -> {
                animateNavItem(binding.navHome, true)
                binding.navHome.isSelected = true
                binding.ivNavHome.isSelected = true
                binding.tvNavHome.isSelected = true
            }

            1 -> {
                animateNavItem(binding.navQibla, true)
                binding.navQibla.isSelected = true
                binding.ivNavQibla.isSelected = true
                binding.tvNavQibla.isSelected = true
            }

            2 -> {
                animateNavItem(binding.navBook, true)
                binding.navBook.isSelected = true
                binding.ivNavBook.isSelected = true
                binding.tvNavBook.isSelected = true
            }

            3 -> {
                animateNavItem(binding.navMore, true)
                binding.navMore.isSelected = true
                binding.ivNavMore.isSelected = true
                binding.tvNavMore.isSelected = true
            }
        }
    }
    
    private fun animateNavItem(view: View, selected: Boolean) {
        val scale = if (selected) 1.25f else 1.0f
        val translationY = if (selected) -8 * resources.displayMetrics.density else 0f
        
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .translationY(translationY)
            .setDuration(200)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()
    }

    fun navigateToPage(position: Int) {
        binding.viewPager.currentItem = position
    }

    fun openDrawer() {
        hideKeyboard()
        loadDrawerUserName()
        loadDrawerProfileImage()
        loadConversations() // Load chat history when drawer opens
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }
    
    
    private fun loadDrawerUserName() {
        val userId = authManager.currentUser?.uid ?: return
        val tvUserName = binding.drawerLayout.findViewById<TextView>(R.id.tv_drawer_user_name)
        tvUserName?.text = ProfileManager.getUserName(this, userId)
    }
    
    private fun loadDrawerProfileImage() {
        val userId = authManager.currentUser?.uid ?: return
        val ivProfilePhoto = binding.drawerLayout.findViewById<ImageView>(R.id.iv_drawer_profile_photo)
        val tvInitials = binding.drawerLayout.findViewById<TextView>(R.id.tv_drawer_avatar_initials)
        
        val profileBitmap = ProfileManager.getProfileImage(this, userId)
        if (profileBitmap != null) {
            ivProfilePhoto?.setImageBitmap(profileBitmap)
            ivProfilePhoto?.visibility = View.VISIBLE
            tvInitials?.visibility = View.GONE
        } else {
            ivProfilePhoto?.visibility = View.GONE
            tvInitials?.visibility = View.VISIBLE
            tvInitials?.text = ProfileManager.getUserInitials(this, userId)
        }
    }

    private fun hideKeyboard() {
        val imm =
            getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }

    fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.al_aalim.utils.LanguageManager.applyLanguage(newBase))
    }
}

package com.example.al_aalim.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.tooling.preview.Preview
import com.example.al_aalim.R
import com.example.al_aalim.repository.ChatRepository
import com.example.al_aalim.models.ChatMessage
import com.example.al_aalim.ui.theme.*
import com.example.al_aalim.viewmodel.HomeViewModel
import com.example.al_aalim.ui.components.AdaptiveContainer
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onMenuClick: () -> Unit,
    onAccountClick: () -> Unit,
    onRefreshDrawer: (() -> Unit)? = null  // Fix 6: propagate to viewModel.sendMessage
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAITyping by viewModel.isAITyping.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    // Fix 5: Proper Compose keyboard controller — no Activity cast needed
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // Error handling Toast
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // Fix 4: Auto-scroll to bottom — use (size - 1) as target to avoid off-by-one.
    // The previous code used chatMessages.size which is 1 past the last valid index.
    LaunchedEffect(chatMessages.size, isAITyping) {
        if (chatMessages.isNotEmpty()) {
            val lastIndex = (chatMessages.size - 1).coerceAtLeast(0) + if (isAITyping) 1 else 0
            listState.animateScrollToItem(lastIndex)
        }
    }

    // --- Launchers ---
    // Camera
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                selectedMediaUris = selectedMediaUris + uri
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val (file, uri) = createImageFile(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedMediaUris = selectedMediaUris + uris
    }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            galleryLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Files
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        selectedMediaUris = selectedMediaUris + uris
    }

    // Speech
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                inputText = if (inputText.isNotEmpty()) "$inputText $spokenText" else spokenText
            }
        }
    }
    val speechPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechLauncher.launch(intent)
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                onMenuClick = onMenuClick,
                onAccountClick = onAccountClick
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        AdaptiveContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(BackgroundGradientStart, BackgroundGradientEnd)
                        )
                    )
                    .padding(paddingValues)
                    .imePadding()
            ) {
            // Chat Content
            Box(modifier = Modifier.weight(1f)) {
                if (chatMessages.isEmpty() && viewModel.getActiveConversationId() == null) {
                    WelcomeScreen()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(chatMessages) { message ->
                            ChatMessageItem(
                                message = message,
                                onCopy = { text ->
                                    clipboardManager.setText(AnnotatedString(text))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onEdit = { text ->
                                    inputText = text
                                },
                                onDelete = { id ->
                                    viewModel.deleteMessage(id)
                                },
                                onShare = { text ->
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, text)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                            )
                        }
                        if (isAITyping) {
                            item {
                                TypingIndicator()
                            }
                        }
                    }
                    
                    val isAtBottom by remember {
                        derivedStateOf {
                            val layoutInfo = listState.layoutInfo
                            if (layoutInfo.visibleItemsInfo.isEmpty()) true
                            else {
                                val lastVisible = layoutInfo.visibleItemsInfo.last()
                                lastVisible.index == layoutInfo.totalItemsCount - 1
                            }
                        }
                    }
                    if (!isAtBottom && chatMessages.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem((chatMessages.size - 1).coerceAtLeast(0) + if (isAITyping) 1 else 0) } },
                            shape = CircleShape,
                            containerColor = Gold,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 8.dp)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll Down", tint = Color.White)
                        }
                    }
                }
            }

            // Input Bar
            ChatInputBar(
                inputText = inputText,
                onInputTextChanged = { inputText = it },
                selectedMediaUris = selectedMediaUris,
                onRemoveMedia = { uri -> selectedMediaUris = selectedMediaUris - uri },
                onAttachmentClick = { showAttachmentSheet = true },
                onVoiceClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        }
                        speechLauncher.launch(intent)
                    } else {
                        speechPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onSendClick = {
                    if (inputText.isNotBlank() || selectedMediaUris.isNotEmpty()) {
                        viewModel.sendMessage(
                            inputText.trim(),
                            selectedMediaUris,
                            onConversationCreated = { /* viewModel handles it internally */ },
                            onRefreshDrawer = onRefreshDrawer  // Fix 6: notify drawer
                        )
                        inputText = ""
                        selectedMediaUris = emptyList()
                        // Fix 5: Use Compose keyboard controller instead of unsafe Activity cast
                        keyboardController?.hide()
                    }
                }
            )
        }

        // Attachment Bottom Sheet
        if (showAttachmentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAttachmentSheet = false },
                containerColor = Color(0xFF0D4A4C)
            ) {
                AttachmentSheetContent(
                    onCameraClick = {
                        showAttachmentSheet = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val (file, uri) = createImageFile(context)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onGalleryClick = {
                        showAttachmentSheet = false
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            galleryLauncher.launch("image/*")
                        } else {
                            galleryPermissionLauncher.launch(permission)
                        }
                    }
                )
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onMenuClick: () -> Unit, onAccountClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .border(1.dp, Gold, RoundedCornerShape(30.dp))
                    .background(TooltipBackgroundTeal)
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Text("Al Aalim", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick, modifier = Modifier.padding(start = 16.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_navigation), contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        },
        actions = {
            IconButton(onClick = onAccountClick, modifier = Modifier.padding(end = 16.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_account_main), contentDescription = "Account", tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun WelcomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Assalamu Alaikum",
            textAlign = TextAlign.Center,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Gold
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "I am Al-Aalim, your Islamic knowledge companion. Ask me anything about the Quran, Hadith, prayer times, Islamic history, or daily guidance. I'm here to help you on your spiritual journey.",
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun TypingIndicator() {
    val dotCount = 3
    // Each dot is staggered by 160ms so they bounce in sequence
    val dotDelays = listOf(0, 160, 320)
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    Row(
        modifier = Modifier
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(dotCount) { index ->
            // Each dot animates its Y offset independently with a phase delay
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 500,
                        delayMillis = dotDelays[index],
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(GoldGradientStart) // Gold dot
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onEdit: (String) -> Unit,
    onCopy: (String) -> Unit,
    onDelete: (String) -> Unit,
    onShare: (String) -> Unit
) {
    val isUser = message.isUser
    var showMenu by remember { mutableStateOf(false) }
    
    // Parent layout aligned properly
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 60.dp else 0.dp,
                end = if (isUser) 0.dp else 60.dp
            ),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Render attachments if any
        if (message.attachments.isNotEmpty()) {
            message.attachments.forEach { attachment ->
                // Determine the image model: file path or URI
                val imageModel: Any = if (attachment.url.startsWith("/")) {
                    // Absolute file path from internal storage
                    java.io.File(attachment.url)
                } else {
                    // content:// or https:// URI
                    Uri.parse(attachment.url)
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(200.dp)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // Render Text
        if (message.message.isNotEmpty()) {
            if (isUser) {
                Box {
                    Card(
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { showMenu = true }
                        ),
                        colors = CardDefaults.cardColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = message.message,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    
                    MaterialTheme(
                        colorScheme = MaterialTheme.colorScheme.copy(
                            surface = BackgroundGradientEnd,
                            onSurface = Color.White
                        )
                    ) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(BackgroundGradientEnd).border(1.dp, Gold, RoundedCornerShape(4.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit", color = Color.White) },
                                onClick = { onEdit(message.message); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Gold) }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy", color = Color.White) },
                                onClick = { onCopy(message.message); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Gold) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.White) },
                                onClick = { onDelete(message.id); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Gold) }
                            )
                        }
                    }
                }
            } else {
                Column {
                    Text(
                        text = message.message,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                    
                    // Interaction row for AI message
                    Row(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                        IconButton(
                            onClick = { onCopy(message.message) }, 
                            modifier = Modifier.size(28.dp).padding(4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onShare(message.message) }, 
                            modifier = Modifier.size(28.dp).padding(4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    selectedMediaUris: List<Uri>,
    onRemoveMedia: (Uri) -> Unit,
    onAttachmentClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit
) {
    Column(modifier = Modifier.background(Color.Transparent)) {
        // Media Previews
        if (selectedMediaUris.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedMediaUris) { uri ->
                    Box(modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(12.dp))) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { onRemoveMedia(uri) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color.White, CircleShape)
                         ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        
        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BottomSheetIconTeal)
                    .clickable { onAttachmentClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Attach", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = { Text("Type a message...", color = Color.White.copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.3f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedContainerColor = MessageInputTeal,
                    unfocusedContainerColor = MessageInputTeal,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Gold
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.weight(1f),
                maxLines = 4,
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mic), 
                        contentDescription = "Voice", 
                        tint = Color.Unspecified, 
                        modifier = Modifier.size(32.dp).clickable { onVoiceClick() }.padding(end = 4.dp)
                    )
                }
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            // Send button — enabled only when there is text OR attached media
            val canSend = inputText.isNotBlank() || selectedMediaUris.isNotEmpty()
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (canSend) {
                            Brush.linearGradient(listOf(GoldGradientStart, GoldGradientEnd))
                        } else {
                            Brush.linearGradient(listOf(BottomSheetIconTeal, BottomSheetIconTeal))
                        }
                    )
                    .then(
                        if (canSend) Modifier.clickable { onSendClick() }
                        else Modifier  // no-op when disabled — prevents accidental empty sends
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Send",
                    tint = Color.White.copy(alpha = if (canSend) 1f else 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AttachmentSheetContent(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add Attachment", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            AttachmentOption(icon = R.drawable.ic_camera, text = "Camera", onClick = onCameraClick)
            Spacer(modifier = Modifier.width(48.dp)) // Increased spacing for better balance
            AttachmentOption(icon = R.drawable.ic_photos, text = "Gallery", onClick = onGalleryClick)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AttachmentOption(icon: Int, text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BackgroundGradientStart)
        ) {
            Icon(painter = painterResource(id = icon), contentDescription = text, tint = Gold, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = text, color = Color.White, fontSize = 14.sp)
    }
}

private fun createImageFile(context: android.content.Context): Pair<File, Uri> {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return Pair(file, uri)
}

@Preview(showBackground = true)
@Composable
fun HomeRoutePreview() {
    val context = LocalContext.current
    val repository = ChatRepository(context)
    val viewModel = HomeViewModel(repository, context)
    HomeRoute(
        viewModel = viewModel,
        onMenuClick = {},
        onAccountClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun HomeTopBarPreview() {
    HomeTopBar(
        onMenuClick = {},
        onAccountClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen()
}

@Preview(showBackground = true)
@Composable
fun TypingIndicatorPreview() {
    TypingIndicator()
}

@Preview(showBackground = true)
@Composable
fun ChatMessageItemPreview() {
    val message = ChatMessage(
        id = "1",
        message = "Assalamu Alaikum, how can I help you today?",
        isUser = false
    )
    ChatMessageItem(
        message = message,
        onEdit = {},
        onCopy = {},
        onDelete = {},
        onShare = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ChatInputBarPreview() {
    ChatInputBar(
        inputText = "Hello!",
        onInputTextChanged = {},
        selectedMediaUris = emptyList(),
        onRemoveMedia = {},
        onAttachmentClick = {},
        onVoiceClick = {},
        onSendClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun AttachmentSheetContentPreview() {
    AttachmentSheetContent(
        onCameraClick = {},
        onGalleryClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun AttachmentOptionPreview() {
    AttachmentOption(
        icon = R.drawable.ic_camera,
        text = "Camera",
        onClick = {}
    )
}

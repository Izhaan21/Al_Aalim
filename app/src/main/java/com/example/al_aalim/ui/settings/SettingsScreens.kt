package com.example.al_aalim.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.al_aalim.R
import com.example.al_aalim.model.Language
import com.example.al_aalim.ui.components.LanguageListItem
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.ViewModelProvider
import com.example.al_aalim.viewmodel.AccountViewModel
import com.example.al_aalim.viewmodel.ViewModelFactory
import com.example.al_aalim.ui.components.AdaptiveContainer

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeBrush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
    ) {
        AdaptiveContainer {
            Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(id = R.drawable.ic_back), "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Gold, RoundedCornerShape(16.dp))
                        .background(Color(0xFF233F40))
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Text("Privacy Policy", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Box(modifier = Modifier.size(44.dp))
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.privacy_policy_content),
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("For more information, visit our website:", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val link = stringResource(R.string.privacy_policy_website_link)
                        Text(
                            text = link,
                            color = Color(0xFF80CBC4),
                            fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Website Coming Soon!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
fun TermsOfUseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeBrush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
    ) {
        AdaptiveContainer {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(top = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(id = R.drawable.ic_back), "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Gold, RoundedCornerShape(16.dp))
                            .background(Color(0xFF233F40))
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Text("Terms of Use", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Box(modifier = Modifier.size(44.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.terms_of_use_content),
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("For more information, visit our website:", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            val link = stringResource(R.string.terms_of_use_website_link)
                            Text(
                                text = link,
                                color = Color(0xFF80CBC4),
                                fontSize = 14.sp,
                                modifier = Modifier.clickable {
                                    android.widget.Toast.makeText(context, "Website Coming Soon!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FaqFeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("Bug") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeBrush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
    ) {
        AdaptiveContainer {
            Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(id = R.drawable.ic_back), "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Gold, RoundedCornerShape(16.dp))
                        .background(Color(0xFF233F40))
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Text("FAQ & Feedback", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Box(modifier = Modifier.size(44.dp))
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Frequently Asked Questions", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 4.dp))
                
                val faqs = listOf(
                    "How do I change my reciter?" to "Navigate to Settings -> App Settings -> Reciter Selection. You can choose from various world-renowned reciter.",
                    "Why isn't Qibla showing the exact direction?" to "Ensure your phone's GPS is on and calibrate your compass by moving the device in a figure-8 motion. Avoid metal objects.",
                    "How can I delete my reading history?" to "Go to Account Settings -> User Data -> Reading History. You will find a 'Delete All' option there.",
                    "Is the offline mode available?" to "Yes, once you play a Surah, it is cached for offline listening. You can manage your downloads in the settings.",
                    "Can I use Al Aalim on multiple devices?" to "Yes, simply log in with your account on any device to sync your bookmarks and history."
                )

                faqs.forEach { (question, answer) ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Q: $question", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = answer, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }

                Text("Send Us Feedback", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 4.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Category", color = Gold, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Bug", "Feature", "General").forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) Gold else Color.White.copy(alpha = 0.1f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Gold else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when(cat) { "Bug" -> "🐛 Bug"; "Feature" -> "💡 Feature"; else -> "💬 General" },
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Subject", color = Gold, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Brief summary", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Gold, unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White.copy(alpha = 0.1f), unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Message", color = Gold, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("Describe your feedback...", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Gold, unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White.copy(alpha = 0.1f), unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PrimaryButton(text = "Submit Feedback", onClick = {
                            if (subject.isNotBlank() && message.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("infisoft.innvoations@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, "App Feedback [$selectedCategory]: $subject")
                                    putExtra(Intent.EXTRA_TEXT, "Category: $selectedCategory\n\n$message")
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Send Feedback"))
                                    onBack()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }
            }
        }
    }
}

@Composable
fun LanguageSelectionScreen(
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    onLanguageSelected: (Language) -> Unit
) {
    var selectedLanguage by remember { mutableStateOf<Language?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val allLanguages = remember {
        listOf(
            Language("ar", "العربية", "Arabic", "🇸🇦"), Language("en", "English", "English", "🇺🇸"),
            Language("ur", "اردو", "Urdu", "🇵🇰"), Language("bn", "বাংলা", "Bengali", "🇧🇩"),
            Language("hi", "हिन्दी", "Hindi", "🇮🇳"), Language("id", "Bahasa Indonesia", "Indonesian", "🇮🇩"),
            Language("ms", "Bahasa Melayu", "Malay", "🇲🇾"), Language("ha", "Hausa", "Hausa", "🇳🇬"),
            Language("sw", "Kiswahili", "Swahili", "🇰🇪"), Language("fa", "فارسی", "Persian", "🇮🇷"),
            Language("tr", "Türkçe", "Turkish", "🇹🇷"), Language("fr", "Français", "French", "🇫🇷"),
            Language("de", "Deutsch", "German", "🇩🇪"), Language("es", "Español", "Spanish", "🇪🇸"),
            Language("uz", "O'zbek", "Uzbek", "🇺🇿"), Language("zh", "中文", "Chinese", "🇨🇳"),
            Language("ru", "Русский", "Russian", "🇷🇺")
        )
    }

    val filteredLanguages = allLanguages.filter {
        it.nativeName.contains(searchQuery, ignoreCase = true) || 
        it.englishName.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeBrush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
    ) {
        AdaptiveContainer {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(top = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(id = R.drawable.ic_back), "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    } else {
                        Box(modifier = Modifier.size(44.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Gold, RoundedCornerShape(16.dp))
                            .background(Color(0xFF233F40))
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Text("Select Language", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Box(modifier = Modifier.size(44.dp))
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search language...", color = Color.White.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Gold, unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White.copy(alpha = 0.1f), unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Text(
                    text = "${filteredLanguages.size} languages found",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLanguages, key = { it.code }) { language ->
                        LanguageListItem(
                            nativeName = language.nativeName,
                            englishName = language.englishName,
                            flag = language.flagEmoji,
                            isSelected = language == selectedLanguage,
                            onClick = { selectedLanguage = language }
                        )
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = selectedLanguage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(modifier = Modifier.padding(16.dp).padding(bottom = 24.dp)) {
                PrimaryButton(text = "Continue", onClick = {
                    selectedLanguage?.let { onLanguageSelected(it) }
                })
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeBrush.linearGradient(listOf(Gold, Color(0xFFC59A45))))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun AccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val factory = remember { ViewModelFactory(context) }
    val accountViewModel = remember { ViewModelProvider(context as androidx.activity.ComponentActivity, factory)[AccountViewModel::class.java] }
    val profileState by accountViewModel.profileState.collectAsState()
    
    var showEditProfile by remember { mutableStateOf(false) }
    var showChangePwd by remember { mutableStateOf(false) }
    var showChangeEmail by remember { mutableStateOf(false) }
    var showGenderSelection by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    var showDeleteConfirmPassword by remember { mutableStateOf(false) }
    var showLogout by remember { mutableStateOf(false) }
    var showDeleteChat by remember { mutableStateOf(false) }
    var showClearCache by remember { mutableStateOf(false) }
    
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                accountViewModel.loadProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeBrush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(id = R.drawable.ic_back), "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Gold, RoundedCornerShape(16.dp))
                        .background(Color(0xFF233F40))
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Text("Account Settings", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Box(modifier = Modifier.size(44.dp))
            }
            
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(
                            width = 2.dp,
                            brush = ComposeBrush.horizontalGradient(listOf(Gold, com.example.al_aalim.ui.theme.GoldDeep)),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .padding(4.dp) // Gap between border and image
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable { context.startActivity(Intent(context, ProfilePhotoActivity::class.java)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileState.profileImage != null) {
                        Image(
                            bitmap = profileState.profileImage!!.asImageBitmap(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(ComposeBrush.horizontalGradient(listOf(Gold, com.example.al_aalim.ui.theme.GoldDeep))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(profileState.initials.ifEmpty { "U" }, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Salam, ${profileState.name.ifEmpty { "User" }}",
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White
                )
                Text(
                    text = accountViewModel.currentUser?.email ?: "",
                    fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item { SettingsSectionHeader("User Data", "Your personalized app data") }
                item {
                    SettingsCard {
                        SettingsRowItem(iconRes = R.drawable.ic_verse_bookmark, title = "Bookmarks", onClick = {
                            context.startActivity(Intent(context, FavoritesActivity::class.java))
                        })
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        SettingsRowItem(iconRes = R.drawable.ic_account_history, title = "Reading History", onClick = {
                            context.startActivity(Intent(context, ReadingHistoryActivity::class.java))
                        })
                    }
                }

                item { SettingsSectionHeader("Profile Management", "Manage your profile details") }
                item {
                    SettingsCard {
                        SettingsRowItem(iconRes = R.drawable.ic_edit_profile_pencil, title = "Edit Profile", onClick = { showEditProfile = true })
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        SettingsRowItem(iconRes = R.drawable.ic_gender, title = "Gender", onClick = { showGenderSelection = true })
                    }
                }

                if (!accountViewModel.isGoogleUser) {
                    item { SettingsSectionHeader("Security", "Update your credentials") }
                    item {
                        SettingsCard {
                            SettingsRowItem(iconRes = R.drawable.ic_account_password, title = "Change Password", onClick = { showChangePwd = true })
                        }
                    }
                }

                item { SettingsSectionHeader("Account Actions", "Manage your account actions") }
                item {
                    SettingsCard {
                        SettingsRowItem(iconRes = R.drawable.ic_account_delete, title = "Delete Account", onClick = { showDeleteAccount = true })
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color(0x80F44336), RoundedCornerShape(16.dp))
                            .clickable { showLogout = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Logout All", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
        
        // --- Dialogs ---
        if (showLogout) {
            com.example.al_aalim.ui.components.ConfirmationDialog(
                title = "Logout",
                message = "Are you sure you want to log out?",
                confirmText = "Logout",
                cancelText = "Cancel",
                onConfirm = {
                    accountViewModel.logout()
                    val intent = Intent(context, com.example.al_aalim.ui.auth.WelcomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    (context as? android.app.Activity)?.finish()
                    showLogout = false
                },
                onDismiss = { showLogout = false }
            )
        }

        if (showDeleteAccount) {
            com.example.al_aalim.ui.components.ConfirmationDialog(
                title = "Delete Account",
                message = "Are you sure you want to permanently delete your account? All your data including chat history, bookmarks, and profile will be erased. This action cannot be undone.",
                confirmText = "Proceed",
                cancelText = "Cancel",
                onConfirm = {
                    showDeleteAccount = false
                    if (accountViewModel.isGoogleUser) {
                        accountViewModel.deleteAccount("") {
                            val intent = Intent(context, com.example.al_aalim.ui.auth.WelcomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                            (context as? android.app.Activity)?.finish()
                        }
                    } else {
                        showDeleteConfirmPassword = true
                    }
                },
                onDismiss = { showDeleteAccount = false }
            )
        }

        if (showDeleteConfirmPassword) {
            com.example.al_aalim.ui.components.ConfirmWithPasswordDialog(
                title = "Confirm Deletion",
                message = "Enter your password to permanently delete your account. This cannot be undone.",
                confirmText = "Delete Account",
                onConfirm = { password ->
                    accountViewModel.deleteAccount(password) {
                        val intent = Intent(context, com.example.al_aalim.ui.auth.WelcomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                        (context as? android.app.Activity)?.finish()
                    }
                    showDeleteConfirmPassword = false
                },
                onDismiss = { showDeleteConfirmPassword = false }
            )
        }


        
        if (showGenderSelection) {
            val genderOptions = listOf("Male", "Female", "Prefer not to say")
            var currentGenderIndex = genderOptions.indexOf(profileState.gender).takeIf { it >= 0 } ?: -1
            
            com.example.al_aalim.ui.components.SelectionListDialog(
                title = "Select Gender",
                options = genderOptions,
                selectedIndex = currentGenderIndex,
                autoApply = false,
                onSelect = { _, selected ->
                    accountViewModel.setGender(selected)
                    showGenderSelection = false
                },
                onDismiss = { showGenderSelection = false }
            )
        }

        if (showEditProfile) {
            com.example.al_aalim.ui.components.EditProfileDialog(
                initialName = profileState.name,
                profileImageBitmap = profileState.profileImage,
                initials = profileState.initials.ifEmpty { "U" },
                onSave = { name ->
                    accountViewModel.saveProfile(name)
                    showEditProfile = false
                },
                onChangePhoto = {
                    context.startActivity(Intent(context, ProfilePhotoActivity::class.java))
                },
                onDismiss = { showEditProfile = false }
            )
        }

        if (showChangePwd) {
            com.example.al_aalim.ui.components.ChangePasswordDialog(
                isLoading = false,
                onConfirm = { current, newPwd, confirm ->
                    accountViewModel.changePassword(current, newPwd, confirm)
                    showChangePwd = false
                },
                onDismiss = { showChangePwd = false }
            )
        }


    }
}

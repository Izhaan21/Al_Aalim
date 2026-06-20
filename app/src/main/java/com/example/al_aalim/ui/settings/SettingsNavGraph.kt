package com.example.al_aalim.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.al_aalim.R
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import com.example.al_aalim.ui.permissions.LocationPermissionActivity
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.al_aalim.ui.components.AdaptiveContainer

sealed class SettingsRoute(val route: String) {
    object Main : SettingsRoute("settings_main")
    object Account : SettingsRoute("settings_account")
    object Language : SettingsRoute("settings_language")
    object Privacy : SettingsRoute("settings_privacy")
    object Terms : SettingsRoute("settings_terms")
    object Faq : SettingsRoute("settings_faq")
}

@Composable
fun SettingsNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = SettingsRoute.Main.route,
    onFinish: () -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsChanged: (Boolean) -> Unit,
    onNavigateToReciter: () -> Unit,
    onShowDialog: (String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(SettingsRoute.Main.route) {
            MainSettingsScreen(
                onBack = onFinish,
                onNavigate = { route -> navController.navigate(route) },
                notificationsEnabled = notificationsEnabled,
                onNotificationsChanged = onNotificationsChanged,
                onNavigateToReciter = onNavigateToReciter,
                onShowDialog = onShowDialog
            )
        }
        composable(SettingsRoute.Account.route) {
            AccountScreen(onBack = { 
                if (!navController.popBackStack()) {
                    onFinish()
                }
            })
        }
        composable(SettingsRoute.Privacy.route) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsRoute.Terms.route) {
            TermsOfUseScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsRoute.Faq.route) {
            FaqFeedbackScreen(onBack = { navController.popBackStack() })
        }
        composable(SettingsRoute.Language.route) {
            LanguageSelectionScreen(
                onBack = { navController.popBackStack() },
                onLanguageSelected = { /* Handle language switch */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsChanged: (Boolean) -> Unit,
    onNavigateToReciter: () -> Unit,
    onShowDialog: (String) -> Unit
) {
    val context = LocalContext.current
    val backgroundBrush = ComposeBrush.verticalGradient(
        colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        AdaptiveContainer {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Home Style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .border(1.dp, Gold, RoundedCornerShape(30.dp))
                            .background(Color(0xFF233F40))
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Text("Settings", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Spacer(modifier = Modifier.size(44.dp)) // Maintain balance
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item { SettingsSectionHeader("General Settings", "Customize your app preferences") }
                    item {
                        SettingsCard {
                            SettingsRowItem(iconRes = R.drawable.ic_settings_location, title = "Location", onClick = {
                                context.startActivity(Intent(context, LocationPermissionActivity::class.java).apply {
                                    putExtra("from_settings", true)
                                })
                            })
                        }
                    }

                    item { SettingsSectionHeader("App Settings", "Customize your app experience") }
                    item {
                        SettingsCard {
                            SettingsSwitchItem(iconRes = R.drawable.ic_settings_notification, title = "Notifications", isChecked = notificationsEnabled, onCheckedChange = onNotificationsChanged)
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            SettingsRowItem(iconRes = R.drawable.ic_mic_gold, title = "Reciter", onClick = onNavigateToReciter)
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            SettingsRowItem(iconRes = R.drawable.ic_settings_quran_filled, title = "Quran Script", onClick = { onShowDialog("script") })
                        }
                    }

                    item { SettingsSectionHeader("Data & Storage", "Manage app data and cache") }
                    item {
                        SettingsCard {
                            SettingsRowItem(iconRes = R.drawable.ic_settings_clear_history, title = "Delete Chat History", onClick = { onShowDialog("clear_history") })
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            SettingsRowItem(iconRes = R.drawable.ic_storage, title = "Clear Cache", onClick = { onShowDialog("clear_cache") })
                        }
                    }

                    item { SettingsSectionHeader("App Information", "Legal and app details") }
                    item {
                        SettingsCard {
                            SettingsRowItem(iconRes = R.drawable.ic_settings_support, title = "Support Us", onClick = { onShowDialog("support") })
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            SettingsRowItem(iconRes = R.drawable.ic_settings_faq, title = "FAQ & Feedback", onClick = { onNavigate(SettingsRoute.Faq.route) })
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            SettingsRowItem(iconRes = R.drawable.ic_settings_privacy, title = "Privacy Policy", onClick = { onNavigate(SettingsRoute.Privacy.route) })
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            SettingsRowItem(iconRes = R.drawable.ic_settings_privacy, title = "Terms of Use", onClick = { onNavigate(SettingsRoute.Terms.route) })
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            SettingsRowItem(iconRes = R.drawable.ic_settings_about, title = "About", tint = Color.Unspecified, onClick = { onShowDialog("about") })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 4.dp)) {
        Text(text = title, color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
    }
}

@Composable
fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsRowItem(iconRes: Int, title: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(id = iconRes), contentDescription = title, tint = tint, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
    }
}

@Composable
fun SettingsSwitchItem(iconRes: Int, title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(id = iconRes), contentDescription = title, tint = Color.Unspecified, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Gold), modifier = Modifier.scale(0.8f))
    }
}

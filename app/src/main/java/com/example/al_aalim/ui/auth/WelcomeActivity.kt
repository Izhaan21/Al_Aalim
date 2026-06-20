package com.example.al_aalim.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.al_aalim.R
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import com.example.al_aalim.ui.components.AdaptiveContainer
import com.example.al_aalim.viewmodel.AuthState
import com.example.al_aalim.viewmodel.AuthViewModel
import com.example.al_aalim.viewmodel.ViewModelFactory
import com.example.al_aalim.ui.permissions.LocationPermissionActivity
import com.example.al_aalim.ui.settings.SettingsActivity
import com.example.al_aalim.ui.settings.SettingsRoute

class WelcomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            AlAalimTheme {
                WelcomeScreen(
                    onLoginClick = { startActivity(Intent(this, LoginActivity::class.java)) },
                    onRegisterClick = { startActivity(Intent(this, RegisterActivity::class.java)) },
                    onLoginSuccess = {
                        val intent = Intent(this, LocationPermissionActivity::class.java)
                        intent.putExtra("from_auth", true)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginSuccess: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    var isGoogleLoading by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleGoogleSignInResult(result.data)
        } else {
            isGoogleLoading = false
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeBrush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
    ) {
        AdaptiveContainer {
            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Al Aalim",
                    color = Gold,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                )
                
                Text(
                    text = "Your Spiritual Companion",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(64.dp))
                
                // Buttons
                androidx.compose.material3.Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            brush = ComposeBrush.horizontalGradient(listOf(Gold, com.example.al_aalim.ui.theme.GoldDeep)),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                androidx.compose.material3.Button(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            brush = ComposeBrush.horizontalGradient(listOf(Gold, com.example.al_aalim.ui.theme.GoldDeep)),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Register", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // ── OR divider ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.25f)
                    )
                    Text(
                        "  OR  ",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.25f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Google Sign-In Button
                OutlinedButton(
                    onClick = {
                        isGoogleLoading = true
                        val intent = viewModel.getGoogleSignInIntent()
                        if (intent != null) {
                            googleSignInLauncher.launch(intent)
                        } else {
                            isGoogleLoading = false
                            Toast.makeText(context, "Google Sign-In unavailable", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = authState !is AuthState.Loading && !isGoogleLoading,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    if (isGoogleLoading || (authState is AuthState.Loading && isGoogleLoading)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            "Continue with Google",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Terms & Privacy footer
                Text(
                    text = "By continuing, you agree to our",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Terms of Use",
                        color = Gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val intent = Intent(context, SettingsActivity::class.java)
                            intent.putExtra("start_destination", SettingsRoute.Terms.route)
                            context.startActivity(intent)
                        }
                    )
                    Text(
                        text = "  &  ",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Privacy Policy",
                        color = Gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val intent = Intent(context, SettingsActivity::class.java)
                            intent.putExtra("start_destination", SettingsRoute.Privacy.route)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    // Auth state observer
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                isGoogleLoading = false
                onLoginSuccess()
            }
            is AuthState.Error -> {
                isGoogleLoading = false
                Toast.makeText(
                    context,
                    (authState as AuthState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }
}

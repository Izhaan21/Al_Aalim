package com.example.al_aalim.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.al_aalim.R
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import com.example.al_aalim.ui.components.AdaptiveContainer

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
                    onRegisterClick = { startActivity(Intent(this, RegisterActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(onLoginClick: () -> Unit, onRegisterClick: () -> Unit) {
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
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier.size(180.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Al-Aalim",
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
            }
        }
    }
}

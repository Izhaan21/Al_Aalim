package com.example.al_aalim.ui.features

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.example.al_aalim.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.al_aalim.ui.main.BottomNavBar
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import com.example.al_aalim.ui.components.AdaptiveContainer

class StoreActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AlAalimTheme {
                StoreScreen(onBackClick = { finish() })
            }
        }
    }
}

@Composable
fun StoreScreen(onBackClick: () -> Unit, showBottomNav: Boolean = true) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeBrush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
    ) {
        AdaptiveContainer {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding()
            ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(onClick = onBackClick) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_back), contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .border(1.dp, Gold, RoundedCornerShape(30.dp))
                        .background(Color(0xFF233F40))
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Text("Store", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                // Right icon bag
                IconButton(onClick = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() }) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Gold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_store_modern),
                            contentDescription = null,
                            tint = Color(0xFFAAB648), // Gold/Green tint from design
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Full Store Coming Soon",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "We are creating the best islamic products\nfor you. Stay tuned!",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(ComposeBrush.linearGradient(listOf(Gold, Color(0xFFC59A45))))
                                .clickable { Toast.makeText(context, "You will be notified!", Toast.LENGTH_SHORT).show() }
                                .padding(horizontal = 36.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Notify Me", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) 
                        }
                    }
                }
            }
        }

        // Bottom Nav
        if (showBottomNav) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                BottomNavBar(
                    selectedTab = 3,
                    onTabSelected = { tab ->
                        when(tab) {
                            0 -> { context.startActivity(android.content.Intent(context, com.example.al_aalim.ui.main.ContainerActivity::class.java)); onBackClick() }
                            1 -> { context.startActivity(android.content.Intent(context, QiblaActivity::class.java)); onBackClick() }
                            2 -> { context.startActivity(android.content.Intent(context, QuranActivity::class.java)); onBackClick() }
                            3 -> { /* already here */ }
                        }
                    }
                )
            }
            }
        }
    }
}


package com.example.al_aalim.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.al_aalim.R
import com.example.al_aalim.ui.theme.*

data class NavItem(val labelRes: Int, val iconRes: Int)

val NAV_ITEMS = listOf(
    NavItem(R.string.nav_home,  R.drawable.ic_nav_home_modern),
    NavItem(R.string.nav_qibla, R.drawable.ic_nav_qibla_modern),
    NavItem(R.string.nav_book,  R.drawable.ic_nav_book_modern),
    NavItem(R.string.nav_store, R.drawable.ic_nav_store_modern),
)

@Composable
fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .height(84.dp) // Accounts for the floating FAB
    ) {
        // The actual rounded nav bar container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BottomNavStartTeal, BottomNavEndTeal) // Dark Teal
                    )
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NAV_ITEMS.forEachIndexed { index, item ->
                // Leave center gap for the FAB overlay (between items 1 and 2)
                if (index == 2) {
                    Spacer(modifier = Modifier.weight(1.2f))
                }

                val isSelected = index == selectedTab
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1f,
                    animationSpec = tween(200),
                    label = "scale$index"
                )
                val translateY by animateFloatAsState(
                    targetValue = if (isSelected) -4f else 0f,
                    animationSpec = tween(200),
                    label = "ty$index"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationY = translateY
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onTabSelected(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = stringResource(item.labelRes),
                        tint = if (isSelected) Gold else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(item.labelRes),
                        color = if (isSelected) Gold else Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        
        // Central Huge FAB over the bottom bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(64.dp)
                .clip(CircleShape)
                .background(BottomNavEndTeal)
                .border(2.dp, Brush.linearGradient(listOf(Gold, GoldBorder)), CircleShape)
                .clickable { 
                    Toast.makeText(context, "Al Aalim voice feature coming soon", Toast.LENGTH_SHORT).show()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_voice_waveform),
                contentDescription = "Voice Assistant",
                tint = Color.Unspecified,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

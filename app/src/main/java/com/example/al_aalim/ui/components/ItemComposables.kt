package com.example.al_aalim.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.al_aalim.R

// ─────────────────── Shared constants ──────────────────────────────────────

private val Gold       = Color(0xFFD4A843)
private val TextWhite  = Color(0xFFFFFFFF)
private val TextMuted  = Color(0xAAFFFFFF)
private val GlassBg    = Color(0xFF252540)
private val GlassBorder = Color(0x33FFFFFF)
private val CardShape  = RoundedCornerShape(16.dp)

private val GlassBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF2A2A45), Color(0xFF1E1E35))
)

// ─────────────────── Surah List Item ───────────────────────────────────────

/**
 * Compose equivalent of item_surah.xml — for use in a LazyColumn surah list.
 */
@Composable
fun SurahListItem(
    number: Int,
    nameArabic: String,
    nameEnglish: String,
    meaning: String,
    verseCount: Int,
    revelationType: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) Color(0x22D4A843) else Color.Transparent, label = "surahBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(GlassBrush)
            .border(1.dp, if (isSelected) Gold.copy(alpha = 0.5f) else GlassBorder, CardShape)
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number badge
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x22D4A843)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = Gold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(14.dp))

        // Name + meaning
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nameEnglish,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$meaning  •  $verseCount verses  •  $revelationType",
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(12.dp))

        // Arabic name
        Text(
            text = nameArabic,
            color = Gold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────── Reciter Card ─────────────────────────────────────────

/**
 * Compose equivalent of item_reciter.xml — grid card for reciter selection.
 */
@Composable
fun ReciterCard(
    nameEnglish: String,
    country: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) Gold else GlassBorder, label = "recBorder"
    )

    Column(
        modifier = Modifier
            .width(160.dp)
            .height(160.dp)
            .clip(CardShape)
            .background(GlassBrush)
            .border(1.dp, borderColor, CardShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color(0x33D4A843) else Color(0x22FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_account),
                contentDescription = null,
                tint = if (isSelected) Gold else TextMuted,
                modifier = Modifier.size(28.dp)
            )
            // Selection badge
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Gold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = "Selected",
                        tint = Color(0xFF1A1A2E),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = nameEnglish,
            color = if (isSelected) Gold else TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = country,
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────── Language Item ─────────────────────────────────────────

/**
 * Compose equivalent of item_language.xml — for language selection list.
 */
@Composable
fun LanguageListItem(
    nativeName: String,
    englishName: String,
    flag: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) Color(0x22D4A843) else Color.Transparent, label = "langBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = flag, fontSize = 24.sp)

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nativeName,
                color = if (isSelected) Gold else TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = englishName,
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = "Selected",
                tint = Gold,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ─────────────────── Verse Item ───────────────────────────────────────────

/**
 * Compose equivalent of item_verse.xml — Quran verse display.
 */
@Composable
fun VerseItem(
    verseNumber: Int,
    arabicText: String,
    translationText: String,
    isBookmarked: Boolean = false,
    onBookmarkClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onCopyClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(GlassBrush)
            .border(1.dp, GlassBorder, CardShape)
            .padding(16.dp)
    ) {
        // Verse number badge + actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x22D4A843)),
                contentAlignment = Alignment.Center
            ) {
                Text(verseNumber.toString(), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBookmarkClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(
                            if (isBookmarked) R.drawable.ic_star else R.drawable.ic_star
                        ),
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Gold else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onCopyClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = "Copy",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onShareClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = "Share",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Arabic text
        Text(
            text = arabicText,
            color = TextWhite,
            fontSize = 22.sp,
            lineHeight = 38.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Translation
        Text(
            text = translationText,
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

// ─────────────────── FAQ Item ─────────────────────────────────────────────

/**
 * Compose equivalent of item_faq.xml — expandable FAQ entry.
 */
@Composable
fun FaqItem(
    question: String,
    answer: String,
    isExpanded: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isExpanded) Color(0x11D4A843) else Color.Transparent)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = question,
                color = if (isExpanded) Gold else TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(
                    if (isExpanded) R.drawable.uparrow else R.drawable.ic_arrow_down
                ),
                contentDescription = null,
                tint = if (isExpanded) Gold else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        if (isExpanded) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = answer,
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// ─────────────────── Bookmark / Favorite Item ─────────────────────────────

/**
 * Compose equivalent of item_favorite.xml — bookmarked verse card.
 */
@Composable
fun BookmarkItem(
    surahNumber: Int,
    verseNumber: Int,
    surahName: String,
    surahMeaning: String,
    arabicSnippet: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x22D4A843)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$surahNumber:$verseNumber",
                color = Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(surahName, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(surahMeaning, color = TextMuted, fontSize = 12.sp, maxLines = 1)
            if (arabicSnippet.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    arabicSnippet, color = Gold.copy(alpha = 0.7f),
                    fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Text("Ayah $verseNumber", color = TextMuted, fontSize = 11.sp)
    }
}

// ─────────────────── Reading History Item ──────────────────────────────────

/**
 * Compose equivalent of item_reading_history.xml — reading history row.
 */
@Composable
fun ReadingHistoryItemRow(
    surahNumber: Int,
    surahName: String,
    surahMeaning: String,
    lastVerseNumber: Int,
    timeAgo: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x22D4A843)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$surahNumber:$lastVerseNumber",
                color = Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(surahName, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(surahMeaning, color = TextMuted, fontSize = 12.sp, maxLines = 1)
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text("Last: Ayah $lastVerseNumber", color = Gold.copy(alpha = 0.8f), fontSize = 11.sp)
            Spacer(Modifier.height(2.dp))
            Text(timeAgo, color = TextMuted, fontSize = 10.sp)
        }
    }
}

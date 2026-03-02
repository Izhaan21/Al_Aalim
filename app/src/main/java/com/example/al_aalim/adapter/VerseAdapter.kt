package com.example.al_aalim.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.al_aalim.R

data class Verse(
    val numberInSurah: Int,
    val arabicText: String,
    val translation: String
)

enum class DisplayMode { ARABIC, TRANSLATION, BOTH }

class VerseAdapter(
    private val verses: List<Verse>,
    private val surahName: String = "",
    private val onVersePlayClick: (Int) -> Unit  // passes numberInSurah
) : RecyclerView.Adapter<VerseAdapter.VerseViewHolder>() {

    var displayMode: DisplayMode = DisplayMode.BOTH
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var quranScript: String = "Uthmani"
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var playingIndex: Int = -1      // index into `verses`, -1 = nothing playing
        set(value) {
            val old = field
            field = value
            if (old >= 0) notifyItemChanged(old)
            if (value >= 0) notifyItemChanged(value)
        }

    var isMediaPlaying: Boolean = false
        set(value) {
            field = value
            if (playingIndex >= 0) notifyItemChanged(playingIndex)
        }

    var arabicTextSizeSp: Float = 22f
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    // Track bookmarked verses
    private val bookmarkedVerses = mutableSetOf<Int>()

    inner class VerseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvArabic: TextView     = itemView.findViewById(R.id.tv_arabic_text)
        val tvTranslation: TextView = itemView.findViewById(R.id.tv_translation)
        val tvNumber: TextView     = itemView.findViewById(R.id.tv_verse_number)
        val ivPlay: ImageView      = itemView.findViewById(R.id.iv_verse_play)
        val ivShare: ImageView     = itemView.findViewById(R.id.iv_verse_share)
        val ivBookmark: ImageView  = itemView.findViewById(R.id.iv_verse_bookmark)
        val cardInner: View        = itemView.findViewById(R.id.verse_card_inner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verse, parent, false)
        return VerseViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerseViewHolder, position: Int) {
        val verse = verses[position]
        val context = holder.itemView.context

        holder.tvNumber.text = verse.numberInSurah.toString()

        // Arabic text - turning gold when recited
        val isCurrent = (position == playingIndex)
        holder.tvArabic.text = verse.arabicText
        holder.tvArabic.setTextColor(
            ContextCompat.getColor(
                context,
                if (isCurrent && isMediaPlaying) R.color.gold else android.R.color.white
            )
        )
        holder.tvArabic.textSize = arabicTextSizeSp

        // Apply script-specific visual styles
        when (quranScript) {
            "Indopak" -> holder.tvArabic.setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            "Simple Enhanced" -> holder.tvArabic.setTypeface(android.graphics.Typeface.SANS_SERIF)
            else -> holder.tvArabic.setTypeface(android.graphics.Typeface.DEFAULT) // "Uthmani"
        }

        holder.tvArabic.visibility = when (displayMode) {
            DisplayMode.ARABIC, DisplayMode.BOTH -> View.VISIBLE
            DisplayMode.TRANSLATION -> View.GONE
        }

        // Translation - ensured white
        holder.tvTranslation.text = "${verse.numberInSurah}. ${verse.translation}"
        holder.tvTranslation.setTextColor(ContextCompat.getColor(context, android.R.color.white))
        holder.tvTranslation.visibility = when (displayMode) {
            DisplayMode.TRANSLATION, DisplayMode.BOTH -> View.VISIBLE
            DisplayMode.ARABIC -> View.GONE
        }

        // Highlight currently playing verse

        holder.ivPlay.setImageResource(
            if (isCurrent && isMediaPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        holder.ivPlay.alpha = 1f
        holder.ivPlay.setColorFilter(ContextCompat.getColor(context, android.R.color.white))

        // Gold card highlight for the playing verse
        holder.cardInner.setBackgroundResource(
            if (isCurrent && isMediaPlaying) R.drawable.verse_playing_highlight
            else R.drawable.input_field_glass
        )

        // Play functionality on the whole item
        holder.itemView.setOnClickListener {
            onVersePlayClick(verse.numberInSurah)
        }
        
        // Also keep icons clickable separately if needed
        holder.ivPlay.setOnClickListener {
            onVersePlayClick(verse.numberInSurah)
        }

        // Share button - ensured white
        holder.ivShare.setColorFilter(ContextCompat.getColor(context, android.R.color.white))
        holder.ivShare.setOnClickListener {
            val ctx = holder.itemView.context
            val shareText = buildString {
                append(verse.arabicText)
                append("\n\n")
                append("${verse.numberInSurah}. ${verse.translation}")
                if (surahName.isNotEmpty()) {
                    append("\n\n— $surahName, Ayah ${verse.numberInSurah}")
                }
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            ctx.startActivity(Intent.createChooser(intent, "Share Verse"))
        }

        // Bookmark button - ensured white, removed any gold logic
        val isBookmarked = bookmarkedVerses.contains(verse.numberInSurah)
        holder.ivBookmark.alpha = 1f
        holder.ivBookmark.setColorFilter(ContextCompat.getColor(context, android.R.color.white))

        holder.ivBookmark.setOnClickListener {
            val ctx = holder.itemView.context
            if (bookmarkedVerses.contains(verse.numberInSurah)) {
                bookmarkedVerses.remove(verse.numberInSurah)
                Toast.makeText(ctx, "Bookmark removed", Toast.LENGTH_SHORT).show()
            } else {
                bookmarkedVerses.add(verse.numberInSurah)
                Toast.makeText(ctx, "Ayah ${verse.numberInSurah} bookmarked", Toast.LENGTH_SHORT).show()
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = verses.size
}

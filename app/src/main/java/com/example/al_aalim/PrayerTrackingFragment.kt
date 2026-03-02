package com.example.al_aalim

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.al_aalim.databinding.FragmentPrayerTrackingBinding
import com.example.al_aalim.model.PrayerName
import com.example.al_aalim.repository.PrayerRepository
import com.example.al_aalim.utils.EnhancedAnimationUtils.fadeIn
import com.example.al_aalim.utils.EnhancedAnimationUtils.showSuccessAnimation
import com.example.al_aalim.utils.HapticUtils
import com.example.al_aalim.utils.HapticUtils.haptic
import com.example.al_aalim.utils.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment for tracking daily prayers with visual progress and statistics
 */
class PrayerTrackingFragment : Fragment() {
    
    private var _binding: FragmentPrayerTrackingBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var prayerRepository: PrayerRepository
    private val fragmentScope = CoroutineScope(Dispatchers.Main)
    
    private var completedCount = 0
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrayerTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prayerRepository = PrayerRepository(requireContext())
        
        // Setup back button
        binding.ivBack.setOnClickListener {
            it.haptic(HapticUtils.HapticType.MEDIUM_CLICK)
            (activity as? ContainerActivity)?.navigateToPage(0)
        }
        
        // Load prayer data
        loadPrayerData()
    }
    
    private fun loadPrayerData() {
        fragmentScope.launch {
            try {
                // Get user location
                val latitude = LocationManager.userLatitude
                val longitude = LocationManager.userLongitude
                
                // Load today's prayers
                val prayerResult = withContext(Dispatchers.IO) {
                    prayerRepository.getTodaysPrayerRecord(latitude, longitude)
                }
                
                if (prayerResult.isSuccess) {
                    val record = prayerResult.getOrNull()
                    if (record != null) {
                        completedCount = record.completionCount
                        
                        // Update progress UI
                        updateProgressUI(record.completionCount)
                        
                        // Create prayer cards
                        createPrayerCards(record.prayers)
                    }
                }
                
                // Load statistics
                val statsResult = withContext(Dispatchers.IO) {
                    prayerRepository.getPrayerStats()
                }
                
                if (statsResult.isSuccess) {
                    val stats = statsResult.getOrNull()
                    if (stats != null) {
                        updateStatsUI(stats)
                    }
                }
            } catch (e: Exception) {
                Log.e("PrayerTracking", "Error loading prayer data", e)
            }
        }
    }
    
    private fun updateProgressUI(completedCount: Int) {
        binding.circularProgress.progress = completedCount
        binding.tvProgressCount.text = "$completedCount/5"
    }
    
    private fun updateStatsUI(stats: com.example.al_aalim.model.PrayerStats) {
        // Update streak
        binding.tvStreakCount.text = stats.currentStreak.toString()
        
        // Update weekly progress
        binding.weeklyProgress.progress = stats.weeklyCompletionRate.toInt()
        binding.tvWeeklyPercent.text = "${stats.weeklyCompletionRate.toInt()}%"
        
        // Update monthly progress
        binding.monthlyProgress.progress = stats.monthlyCompletionRate.toInt()
        binding.tvMonthlyPercent.text = "${stats.monthlyCompletionRate.toInt()}%"
        
        // Update total prayers
        binding.tvTotalPrayers.text = stats.totalPrayersCompleted.toString()
    }
    
    private fun createPrayerCards(prayers: Map<String, com.example.al_aalim.model.PrayerTime>) {
        binding.prayerCardsContainer.removeAllViews()
        
        PrayerName.getAllPrayers().forEach { prayerName ->
            val prayerTime = prayers[prayerName.name]
            if (prayerTime != null) {
                val card = createPrayerCard(prayerName.displayName, prayerTime.time, prayerTime.isCompleted, prayerName.name)
                binding.prayerCardsContainer.addView(card)
            }
        }
    }
    
    private fun createPrayerCard(name: String, time: String, isCompleted: Boolean, prayerKey: String): View {
        val density = resources.displayMetrics.density
        
        // Create card
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * density).toInt()
            }
            radius = 16f * density
            setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
            cardElevation = 0f
        }
        
        // Card content container
        val contentLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                (20 * density).toInt(),
                (16 * density).toInt(),
                (20 * density).toInt(),
                (16 * density).toInt()
            )
            background = ContextCompat.getDrawable(requireContext(), R.drawable.glass_card_background)
        }
        
        // Prayer info (left side)
        val infoLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            orientation = LinearLayout.VERTICAL
        }
        
        val nameText = TextView(requireContext()).apply {
            text = name
            textSize = 18f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val timeText = TextView(requireContext()).apply {
            text = time
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#DDFFFFFF"))
            setPadding(0, (4 * density).toInt(), 0, 0)
        }
        
        infoLayout.addView(nameText)
        infoLayout.addView(timeText)
        
        // Checkmark (right side)
        val checkIcon = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                (48 * density).toInt(),
                (48 * density).toInt()
            )
            setPadding(
                (12 * density).toInt(),
                (12 * density).toInt(),
                (12 * density).toInt(),
                (12 * density).toInt()
            )
            
            if (isCompleted) {
                setImageResource(R.drawable.ic_check)
                setColorFilter(ContextCompat.getColor(requireContext(), R.color.gold))
            } else {
                setImageResource(R.drawable.ic_circle_outline)
                setColorFilter(android.graphics.Color.parseColor("#99FFFFFF"))
            }
            
            background = ContextCompat.getDrawable(requireContext(), android.R.drawable.btn_default)
        }
        
        // Click handler for check/uncheck
        checkIcon.setOnClickListener {
            it.haptic(HapticUtils.HapticType.MEDIUM_CLICK)
            togglePrayer(prayerKey, !isCompleted, checkIcon)
        }
        
        contentLayout.addView(infoLayout)
        contentLayout.addView(checkIcon)
        cardView.addView(contentLayout)
        
        // Fade in animation
        cardView.alpha = 0f
        cardView.fadeIn()
        
        return cardView
    }
    
    private fun togglePrayer(prayerKey: String, nowCompleted: Boolean, checkIcon: ImageView) {
        fragmentScope.launch {
            try {
                val result = if (nowCompleted) {
                    withContext(Dispatchers.IO) {
                        prayerRepository.markPrayerCompleted(prayerKey)
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        prayerRepository.unmarkPrayer(prayerKey)
                    }
                }
                
                if (result.isSuccess) {
                    // Update UI
                    if (nowCompleted) {
                        checkIcon.setImageResource(R.drawable.ic_check)
                        checkIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gold))
                        checkIcon.showSuccessAnimation()
                        checkIcon.haptic(HapticUtils.HapticType.SUCCESS)
                        completedCount++
                    } else {
                        checkIcon.setImageResource(R.drawable.ic_circle_outline)
                        checkIcon.setColorFilter(android.graphics.Color.parseColor("#99FFFFFF"))
                        completedCount--
                    }
                    
                    // Update progress
                    updateProgressUI(completedCount)
                    
                    // Reload stats
                    val statsResult = withContext(Dispatchers.IO) {
                        prayerRepository.getPrayerStats()
                    }
                    if (statsResult.isSuccess) {
                        statsResult.getOrNull()?.let { updateStatsUI(it) }
                    }
                }
            } catch (e: Exception) {
                Log.e("PrayerTracking", "Error toggling prayer", e)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

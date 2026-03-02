package com.example.al_aalim

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.al_aalim.databinding.FragmentMoreBinding
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation

class MoreFragment : Fragment() {
    
    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup clicks
        setupHeaderActions()
        setupPrayerWorshipClicks()
        setupCommunityClicks()
    }
    
    private fun setupHeaderActions() {
        binding.ivBackHeader.setOnClickWithAnimation {
            // Navigate back to Home page (index 0) in ContainerActivity
            (activity as? ContainerActivity)?.navigateToPage(0)
        }
    }
    
    private fun setupPrayerWorshipClicks() {
        // Prayer Tracker
        binding.btnPrayerTracker.setOnClickWithAnimation {
            Toast.makeText(requireContext(), "Prayer Tracker - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Tasbih Counter
        binding.btnTasbih.setOnClickWithAnimation {
            Toast.makeText(requireContext(), "Tasbih Counter - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Daily Dhikr
        binding.btnDhikr.setOnClickWithAnimation {
            Toast.makeText(requireContext(), "Daily Dhikr & Azkar - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Dua Collections
        binding.btnDuas.setOnClickWithAnimation {
            Toast.makeText(requireContext(), "Dua Collections - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Ramadan Calendar
        binding.btnRamadan.setOnClickWithAnimation {
            val intent = Intent(requireContext(), RamadanActivity::class.java)
            startActivity(intent)
        }
    }
    private fun setupCommunityClicks() {
        // Nearby Mosques
        binding.btnMosques.setOnClickWithAnimation {
            Toast.makeText(requireContext(), "Nearby Mosques - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Islamic Events
        binding.btnEvents.setOnClickWithAnimation {
            Toast.makeText(requireContext(), "Islamic Events - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Halal Restaurants
        binding.btnHalal.setOnClickWithAnimation {
            Toast.makeText(requireContext(), "Halal Restaurants - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
    }

    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

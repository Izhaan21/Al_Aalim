package com.example.al_aalim

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.al_aalim.databinding.FragmentQuranBinding

class QuranFragment : Fragment() {
    
    private var _binding: FragmentQuranBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuranBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup back button if needed
        binding.ivBack.setOnClickListener {
            // Navigate back to home via ViewPager
            (activity as? ContainerActivity)?.navigateToPage(0)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

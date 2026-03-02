package com.example.al_aalim

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.al_aalim.databinding.FragmentQuranBinding

class QuranFragment : Fragment() {

    private var _binding: FragmentQuranBinding? = null
    private val binding get() = _binding!!
    private lateinit var surahAdapter: SurahAdapter

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

        // Back button
        binding.ivBack.setOnClickListener {
            (activity as? ContainerActivity)?.navigateToPage(0)
        }

        // RecyclerView
        surahAdapter = SurahAdapter(SurahAdapter.getAllSurahs())
        binding.rvSurahList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = surahAdapter
            setHasFixedSize(false)
        }

        // Live search
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                surahAdapter.filter.filter(s)
                binding.ivClearSearch.visibility =
                    if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear search
        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
            binding.ivClearSearch.visibility = View.GONE
            surahAdapter.filter.filter("")
        }

        // --- Tabs ---
        binding.tabAll.setOnClickListener {
            selectTab(favouritesMode = false)
        }
        binding.tabFavorites.setOnClickListener {
            selectTab(favouritesMode = true)
        }
    }

    private fun selectTab(favouritesMode: Boolean) {
        val selectedColor = requireContext().getColor(R.color.white)
        val unselectedColor = android.graphics.Color.parseColor("#CCFFFFFF")
        // Visuals
        if (favouritesMode) {
            binding.tabFavorites.setBackgroundResource(R.drawable.tab_selected_background)
            binding.tabFavorites.setTextColor(selectedColor)
            binding.tabAll.setBackgroundResource(R.drawable.surah_item_background)
            binding.tabAll.setTextColor(unselectedColor)
        } else {
            binding.tabAll.setBackgroundResource(R.drawable.tab_selected_background)
            binding.tabAll.setTextColor(selectedColor)
            binding.tabFavorites.setBackgroundResource(R.drawable.surah_item_background)
            binding.tabFavorites.setTextColor(unselectedColor)
        }
        // Data
        surahAdapter.setFavouritesOnly(favouritesMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

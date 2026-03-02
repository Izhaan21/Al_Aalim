package com.example.al_aalim

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ScreenPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    
    override fun getItemCount(): Int = 4
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> QiblaFragment()
            2 -> QuranFragment()
            3 -> MoreFragment()
            else -> HomeFragment()
        }
    }
}

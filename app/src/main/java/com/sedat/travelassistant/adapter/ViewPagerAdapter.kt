package com.sedat.travelassistant.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sedat.travelassistant.fragment.LoginFragment
import com.sedat.travelassistant.fragment.RegisterFragment

class ViewPagerAdapter(fragmentManager: FragmentActivity) : FragmentStateAdapter(fragmentManager) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {

        return if (position == 0)
            LoginFragment()
        else
            RegisterFragment()
    }

}
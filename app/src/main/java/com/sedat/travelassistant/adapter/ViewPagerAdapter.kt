package com.sedat.travelassistant.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sedat.travelassistant.fragment.LoginFragment
import com.sedat.travelassistant.fragment.RegisterFragment

class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
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
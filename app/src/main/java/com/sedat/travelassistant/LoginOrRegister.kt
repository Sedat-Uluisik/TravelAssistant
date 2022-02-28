package com.sedat.travelassistant

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.tabs.TabLayoutMediator
import com.sedat.travelassistant.adapter.ViewPagerAdapter
import com.sedat.travelassistant.databinding.ActivityLoginOrRegisterBinding

class LoginOrRegister : AppCompatActivity() {

    private lateinit var binding: ActivityLoginOrRegisterBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginOrRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager){ tab, position ->
            tab.text = if(position == 0)
                getString(R.string.login)
            else
                getString(R.string.register)
        }.attach()
    }
}
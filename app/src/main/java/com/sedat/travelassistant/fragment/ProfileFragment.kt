package com.sedat.travelassistant.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.sedat.travelassistant.LoginOrRegister
import com.sedat.travelassistant.R
import com.sedat.travelassistant.adapter.ViewPagerAdapter
import com.sedat.travelassistant.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var fragmentBinding: FragmentProfileBinding ?= null
    private val binding get() = fragmentBinding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        if(auth.currentUser == null){ //show login-register uı

            binding.profileDetailsLayout.visibility = View.GONE
            binding.loginOrRegisterButton.visibility = View.VISIBLE

        }else{ //show profile details
            binding.profileDetailsLayout.visibility = View.VISIBLE
            binding.loginOrRegisterButton.visibility = View.GONE
        }

        binding.loginOrRegisterButton.setOnClickListener {
            if(auth.currentUser == null){
                activity?.let {
                    val intent = Intent(activity, LoginOrRegister::class.java)
                    it.startActivity(intent)
                }
            }
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            binding.profileDetailsLayout.visibility = View.GONE
            binding.loginOrRegisterButton.visibility = View.VISIBLE
        }


    }
}
package com.sedat.travelassistant.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.sedat.travelassistant.LoginOrRegister
import com.sedat.travelassistant.R
import com.sedat.travelassistant.adapter.ViewPagerAdapter
import com.sedat.travelassistant.databinding.FragmentProfileBinding
import com.sedat.travelassistant.viewmodel.ProfileFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment @Inject constructor(
    private val auth: FirebaseAuth
) : Fragment() {

    private var fragmentBinding: FragmentProfileBinding ?= null
    private val binding get() = fragmentBinding!!

    //private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: ProfileFragmentViewModel

    private var isChange = false //güncelleme işlemi için kullanılıyor.

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(ProfileFragmentViewModel::class.java)
        //auth = Firebase.auth

        if(auth.currentUser == null){ //show login-register uı
            binding.profileDetailsLayout.visibility = View.GONE
            binding.loginOrRegisterButton.visibility = View.VISIBLE
        }else{ //show profile details
            binding.profileDetailsLayout.visibility = View.VISIBLE
            binding.loginOrRegisterButton.visibility = View.GONE

            disableUI()

            getUserInfo(auth.currentUser!!.uid)
        }

        binding.loginOrRegisterButton.setOnClickListener {
            if(auth.currentUser == null){
                activity?.let {
                    val intent = Intent(activity, LoginOrRegister::class.java)
                    it.startActivity(intent)
                }
            }
        }

        binding.logoutBtnProfile.setOnClickListener {
            auth.signOut()
            binding.profileDetailsLayout.visibility = View.GONE
            binding.loginOrRegisterButton.visibility = View.VISIBLE
        }

        binding.updateBtnProfile.setOnClickListener {
            if(auth.currentUser != null){
                if(!isChange){
                    binding.usernameProfile.isEnabled = true
                    binding.mailProfile.isEnabled = true
                    binding.passProfile.isEnabled = true
                    binding.passProfile.visibility = View.VISIBLE

                    isChange = true
                }else{
                    updateUserInfo()
                }
            }
        }
    }

    private fun getUserInfo(userId: String){
        viewModel.getUserInfo(userId){
            binding.usernameProfile.setText(it.username)
            binding.mailProfile.setText(it.mail)
        }
    }

    private fun updateUserInfo(){
        //update
        val mail = binding.mailProfile.text
        val pass = binding.passProfile.text
        val username = binding.usernameProfile.text

        disableUI()
    }

    private fun disableUI(){
        binding.usernameProfile.isEnabled = false
        binding.mailProfile.isEnabled = false
        binding.passProfile.isEnabled = false
        binding.passProfile.visibility = View.GONE
        isChange = false
    }
}
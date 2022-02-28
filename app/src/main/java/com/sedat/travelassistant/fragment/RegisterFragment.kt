package com.sedat.travelassistant.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.sedat.travelassistant.R
import com.sedat.travelassistant.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var fragmentBinding: FragmentRegisterBinding ?= null
    private val binding get() = fragmentBinding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        binding.registerButton.setOnClickListener {
            if(auth.currentUser == null)
                register()
            else
                println("önce çıkış yapmalısınız")
        }

    }

    private fun register(){

    }
}
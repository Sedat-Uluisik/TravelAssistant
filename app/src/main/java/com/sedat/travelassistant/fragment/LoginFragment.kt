package com.sedat.travelassistant.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.sedat.travelassistant.MainActivity
import com.sedat.travelassistant.R
import com.sedat.travelassistant.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var fragmentBinding: FragmentLoginBinding ?= null
    private val binding get() = fragmentBinding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        binding.loginButton.setOnClickListener {
            val mail = binding.loginEMailEdittext.text.toString()
            val pass = binding.loginPasswordEdittext.text.toString()

            if(mail.isNotEmpty() && pass.isNotEmpty()){
                login(mail, pass)
            }
        }
    }

    private fun login(mail: String, pass: String){
        auth.signInWithEmailAndPassword(mail, pass)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    checkEmailVerified()
                }else{
                    Toast.makeText(requireContext(), getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
    }

    private fun checkEmailVerified(){
        if(auth.currentUser != null){
            if(auth.currentUser!!.isEmailVerified){
                activity?.let {
                    val intent = Intent(activity, MainActivity::class.java)
                    it.startActivity(intent)
                    it.finish()
                }
            }else{
                auth.signOut()
                Toast.makeText(requireContext(), getString(R.string.e_mail_verification_required), Toast.LENGTH_LONG).show()
            }
        }
    }
}
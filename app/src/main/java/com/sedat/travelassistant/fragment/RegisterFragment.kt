package com.sedat.travelassistant.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.actionCodeSettings
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.sedat.travelassistant.MainActivity
import com.sedat.travelassistant.R
import com.sedat.travelassistant.databinding.FragmentRegisterBinding
import com.sedat.travelassistant.repo.PlaceRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@AndroidEntryPoint
class RegisterFragment: Fragment() {

    private var fragmentBinding: FragmentRegisterBinding ?= null
    private val binding get() = fragmentBinding!!

    @Inject
    lateinit var repository: PlaceRepositoryInterface

    private lateinit var auth: FirebaseAuth
    private var firestore: FirebaseFirestore ?= null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.registerButton.setOnClickListener {
            val mail = binding.registerEMailEdittext.text.toString()
            val pass = binding.registerPassEdittext.text.toString()
            val username = binding.registerUsernameEdittext.text.toString()

            if(auth.currentUser == null)
                if(mail.isNotEmpty() && pass.isNotEmpty() && username.isNotEmpty())
                    register(mail, pass, username)
                else
                    Toast.makeText(requireContext(), getString(R.string.please_fill_in_all_fields), Toast.LENGTH_LONG).show()
            else
                Toast.makeText(requireContext(), getString(R.string.logout_first), Toast.LENGTH_LONG).show()
        }
    }

    private fun register(mail: String, pass: String, username: String){
        auth.createUserWithEmailAndPassword(mail, pass)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val userId = task.result.user?.uid
                    if(userId != null){
                        saveUserInfoForFirebase(mail, username, userId)
                        //sendVerificationEmail()
                        repository.sendVerificationEmail {

                        }
                    }
                }else
                    Toast.makeText(requireContext(), getString(R.string.authentication_failed), Toast.LENGTH_LONG).show()
            }
    }

    private fun sendVerificationEmail(){
        if(auth.currentUser != null){
            auth.currentUser!!.sendEmailVerification()
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        Toast.makeText(requireContext(), "doğrulama linki gönderildi", Toast.LENGTH_LONG).show()
                        auth.signOut()
                        activity?.let { fragmentActivity ->
                            fragmentActivity.finish()
                            startActivity(fragmentActivity.intent)
                        }
                    }else{
                        auth.signOut()
                        Toast.makeText(requireContext(), "doğrulama linki gönderilemedi", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun saveUserInfoForFirebase(mail: String, username: String, userId: String){
        if(firestore != null){
            val ref = firestore!!.collection("Users")
                .document(userId)

            val user = HashMap<String, Any>()
            user["mail"] = mail
            user["userId"] = userId
            user["username"] = username

            ref.set(user)
        }
    }
}
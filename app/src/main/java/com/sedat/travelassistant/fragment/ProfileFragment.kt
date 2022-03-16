package com.sedat.travelassistant.fragment

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.provider.CalendarContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.sedat.travelassistant.LoginOrRegister
import com.sedat.travelassistant.R
import com.sedat.travelassistant.adapter.ViewPagerAdapter
import com.sedat.travelassistant.databinding.FragmentProfileBinding
import com.sedat.travelassistant.repo.PlaceRepositoryInterface
import com.sedat.travelassistant.util.firebasereferences.References.users
import com.sedat.travelassistant.viewmodel.ProfileFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment @Inject constructor(
    private val auth: FirebaseAuth,
    private val dbFirestore: FirebaseFirestore,
    private val repository: PlaceRepositoryInterface
) : Fragment() {

    private var fragmentBinding: FragmentProfileBinding ?= null
    private val binding get() = fragmentBinding!!

    private lateinit var viewModel: ProfileFragmentViewModel

    private var isUpdateUsername = false //güncelleme işlemi için kullanılıyor.
    private var isUpdateEmail = false //güncelleme işlemi için kullanılıyor.
    private var isUpdatePassword = false //güncelleme işlemi için kullanılıyor.

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

        if(auth.currentUser == null){ //show login-register uı
            binding.profileDetailsLayout.visibility = View.GONE
            binding.loginOrRegisterButton.visibility = View.VISIBLE
        }else{ //show profile details
            binding.profileDetailsLayout.visibility = View.VISIBLE
            binding.loginOrRegisterButton.visibility = View.GONE

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

        binding.updateBtnEmail.setOnClickListener {
            if(auth.currentUser != null){
                if(!isUpdateEmail){
                    binding.mailLayout.setBackgroundResource(R.drawable.line_background)
                    binding.mailCurrentProfile.alpha = 1.0f
                    binding.mailProfileNew.isEnabled = true
                    binding.passProfileForUpdateMail.isEnabled = true
                    binding.mailProfileNew.visibility = View.VISIBLE
                    binding.passProfileForUpdateMail.visibility = View.VISIBLE
                    binding.cancelEmailBtn.visibility = View.VISIBLE
                    isUpdateEmail = !isUpdateEmail
                }else
                    updateEmail()
            }
        }

        binding.cancelEmailBtn.setOnClickListener {
            binding.mailLayout.setBackgroundColor(Color.WHITE)
            binding.mailCurrentProfile.alpha = 0.9f
            binding.mailProfileNew.isEnabled = false
            binding.passProfileForUpdateMail.isEnabled = false
            binding.mailProfileNew.visibility = View.GONE
            binding.passProfileForUpdateMail.visibility = View.GONE
            binding.cancelEmailBtn.visibility = View.GONE
            isUpdateEmail = false
        }

        binding.updateBtnUsername.setOnClickListener {
            if(auth.currentUser != null){
                if(!isUpdateUsername){
                    binding.usernameProfile.isEnabled = true
                    binding.usernameProfile.alpha = 1.0f
                    binding.updateBtnUsername.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green1))
                    binding.cancelUsernameBtn.visibility = View.VISIBLE
                    isUpdateUsername = true
                }else{
                    val username = binding.usernameProfile.text.toString()
                    if(username.isNotEmpty())
                        updateUsername(username)
                    else
                        Toast.makeText(requireContext(), getString(R.string.username_cannot_be_empty), Toast.LENGTH_LONG).show()
                }
            }
        }
        binding.cancelUsernameBtn.setOnClickListener {
            binding.usernameProfile.isEnabled = false
            binding.usernameProfile.alpha = 0.9f
            binding.updateBtnUsername.colorFilter = null
            binding.cancelUsernameBtn.visibility = View.GONE
            isUpdateUsername = false
        }

        binding.updateBtnPassword.setOnClickListener { viewBtn ->
           if(auth.currentUser != null){
               if(!isUpdatePassword){
                   binding.passProfile.alpha = 1.0f
                   binding.passProfile.isEnabled = true
                   binding.updateBtnPassword.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green1))
                   binding.cancelPasswordBtn.visibility = View.VISIBLE
                   isUpdatePassword = true
               }else{
                   val password = binding.passProfile.text.toString()
                   if(password.isNotEmpty())
                       updatePassword(password)
                   else
                       Toast.makeText(requireContext(), getString(R.string.enter_your_password), Toast.LENGTH_SHORT).show()
               }
           }
        }
        binding.cancelPasswordBtn.setOnClickListener {
            binding.passProfile.alpha = 0.9f
            binding.passProfile.isEnabled = false
            binding.updateBtnPassword.colorFilter = null
            binding.cancelPasswordBtn.visibility = View.GONE
            isUpdatePassword = false
        }
    }

    private fun getUserInfo(userId: String){
        viewModel.getUserInfo(userId){
            binding.usernameProfile.setText(it.username)
            binding.mailCurrentProfile.setText(it.mail)
        }
    }

    private fun updatePassword(password: String){
        auth.currentUser!!.updatePassword(password)
            .addOnCompleteListener {
                if(it.isSuccessful){
                    binding.passProfile.alpha = 0.9f
                    binding.passProfile.isEnabled = false
                    binding.updateBtnPassword.colorFilter = null
                    binding.cancelPasswordBtn.visibility = View.GONE
                    isUpdatePassword = false

                    Toast.makeText(requireContext(), getString(R.string.password_changed), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUsername(username: String){
        val userRef = dbFirestore.collection(users).document(auth.currentUser!!.uid)
        userRef.get()
            .addOnSuccessListener { doc ->
                if(doc.data != null){
                    userRef.update(mapOf(
                        "username" to username
                    )).addOnSuccessListener {
                        binding.usernameProfile.isEnabled = false
                        binding.usernameProfile.alpha = 0.9f
                        isUpdateUsername = false
                        binding.updateBtnUsername.colorFilter = null
                        binding.cancelUsernameBtn.visibility = View.GONE
                        Toast.makeText(requireContext(), "kullanıcı adı güncellendi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun updateEmail(){
        val mailOld = binding.mailCurrentProfile.text.toString()
        val mailNew = binding.mailProfileNew.text.toString()
        val currentPassword = binding.passProfileForUpdateMail.text.toString()

        if(mailNew.isNotEmpty() && mailOld.isNotEmpty() && currentPassword.isNotEmpty()){

            val credential = EmailAuthProvider.getCredential(mailOld, currentPassword)

            auth.currentUser!!.reauthenticate(credential)
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        auth.currentUser!!.updateEmail(mailNew)
                            .addOnCompleteListener { task ->
                                if(task.isSuccessful){
                                    repository.sendVerificationEmail(){ bool ->
                                        if(bool){
                                            binding.profileDetailsLayout.visibility = View.GONE
                                            binding.loginOrRegisterButton.visibility = View.VISIBLE
                                            binding.mailProfileNew.visibility = View.GONE
                                            binding.passProfileForUpdateMail.visibility = View.GONE
                                        }
                                    }
                                    //update user info
                                    val userRef = dbFirestore.collection(users).document(auth.currentUser!!.uid)

                                    userRef.get()
                                        .addOnSuccessListener { doc ->
                                            if(doc.data != null){
                                                userRef.update(mapOf(
                                                    "mail" to mailNew
                                                ))
                                            }
                                        }
                                }
                            }
                    }else
                        Toast.makeText(requireContext(), "tüm bilgilerin doğru olduğundan emin olun", Toast.LENGTH_LONG).show()
                }
        }
    }
}
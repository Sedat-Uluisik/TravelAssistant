package com.sedat.travelassistant

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.bumptech.glide.RequestManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sedat.travelassistant.adapter.CategoriesAdapter
import com.sedat.travelassistant.adapter.CommentAdapter
import com.sedat.travelassistant.adapter.ImagesAdapter
import com.sedat.travelassistant.adapter.ViewPagerAdapter
import com.sedat.travelassistant.fragment.*
import com.sedat.travelassistant.util.SaveImageToFile
import javax.inject.Inject

class BaseFragmentFactory @Inject constructor(
    private val glide: RequestManager,
    private val imagesAdapter: ImagesAdapter,
    private val commentAdapter: CommentAdapter,
    private val ffirestore: FirebaseFirestore,
    private val auth: FirebaseAuth
): FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when(className){
            MapFragment::class.java.name -> MapFragment()
            SavedFragment::class.java.name -> SavedFragment()
            CategoriesFragment::class.java.name -> CategoriesFragment()
            DetailsFragment::class.java.name -> DetailsFragment(imagesAdapter, commentAdapter, glide, ffirestore, auth)
            SavedDetailsFragment::class.java.name -> SavedDetailsFragment(glide)
            ProfileFragment::class.java.name -> ProfileFragment()
            else -> super.instantiate(classLoader, className)
        }

    }
}
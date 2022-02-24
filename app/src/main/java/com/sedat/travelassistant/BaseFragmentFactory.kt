package com.sedat.travelassistant

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.bumptech.glide.RequestManager
import com.sedat.travelassistant.adapter.CategoriesAdapter
import com.sedat.travelassistant.adapter.CommentAdapter
import com.sedat.travelassistant.adapter.ImagesAdapter
import com.sedat.travelassistant.fragment.*
import com.sedat.travelassistant.util.SaveImageToFile
import javax.inject.Inject

class BaseFragmentFactory @Inject constructor(
    private val glide: RequestManager,
    private val imagesAdapter: ImagesAdapter,
    private val commentAdapter: CommentAdapter
): FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when(className){
            MapFragment::class.java.name -> MapFragment()
            SavedFragment::class.java.name -> SavedFragment()
            CategoriesFragment::class.java.name -> CategoriesFragment()
            DetailsFragment::class.java.name -> DetailsFragment(imagesAdapter, commentAdapter, glide)
            SavedDetailsFragment::class.java.name -> SavedDetailsFragment(glide)
            else -> super.instantiate(classLoader, className)
        }

    }
}
package com.sedat.travelassistant.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.sedat.travelassistant.R
import com.sedat.travelassistant.adapter.CategoriesAdapter
import com.sedat.travelassistant.databinding.FragmentCategoriesBinding
import com.sedat.travelassistant.model.room.Categories
import com.sedat.travelassistant.viewmodel.MapFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CategoriesFragment : Fragment() {

    private var fragmentBinding: FragmentCategoriesBinding ?= null
    private val binding get() = fragmentBinding!!

    @Inject
    lateinit var categoriesAdapter: CategoriesAdapter

    private lateinit var viewModel: MapFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(MapFragmentViewModel::class.java)
        viewModel.getCategories()

        binding.recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCategories.adapter = categoriesAdapter

        val sharedPref = activity?.getSharedPreferences("com.sedat.travelassistant", Context.MODE_PRIVATE) ?: return
        categoriesAdapter.languageCode = sharedPref.getString("TRAVEL_ASSISTANT_DEVICE_LANGUAGE", "tr").toString()

        categoriesAdapter.setOnCategoryClick {
            findNavController().navigate(CategoriesFragmentDirections.actionCategoriesFragment2ToMapFragment2(it, null))
        }

        getCategories()
    }

    private fun getCategories(){
        viewModel.categories.observe(viewLifecycleOwner, Observer {
            it?.let { list->
                val list_ = ArrayList<Categories>()
                list_.addAll(list)
                //list_.removeFirst()
                categoriesAdapter.categoryList = list_
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
    }
}
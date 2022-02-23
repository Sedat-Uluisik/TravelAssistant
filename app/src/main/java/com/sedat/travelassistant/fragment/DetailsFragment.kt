package com.sedat.travelassistant.fragment

import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firestore.v1.DocumentTransform
import com.sedat.travelassistant.R
import com.sedat.travelassistant.adapter.ImagesAdapter
import com.sedat.travelassistant.databinding.CommentItemLayoutBinding
import com.sedat.travelassistant.databinding.CommentLayoutBinding
import com.sedat.travelassistant.databinding.FragmentDetailsBinding
import com.sedat.travelassistant.model.firebase.Comment
import com.sedat.travelassistant.model.selectedroute.SelectedRoute
import com.sedat.travelassistant.viewmodel.DetailsFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DetailsFragment @Inject constructor(
        private val imagesAdapter: ImagesAdapter,
        private val glide: RequestManager
) : Fragment() {

    private var fragmentBinding: FragmentDetailsBinding ?= null
    private val binding get() = fragmentBinding!!

    private lateinit var viewModel: DetailsFragmentViewModel

    private var firestore: FirebaseFirestore ?= null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentDetailsBinding.inflate(inflater, container, false)

        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(DetailsFragmentViewModel::class.java)

        var place: com.sedat.travelassistant.model.Properties ?= null
        arguments?.let {
            place = DetailsFragmentArgs.fromBundle(it).place
        }
        if (place != null){
            bind(place!!)
            getImages(place!!.name)
            viewModel.getInfo(place!!.name)
        }

        binding.backButton.setOnClickListener {
            //val selectedRoute = SelectedRoute()
            findNavController().popBackStack()
        }

        binding.recylerImages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recylerImages.adapter = imagesAdapter

        imagesAdapter.imageClick {
            try {
                if(binding.imageviewZoom.visibility == View.GONE){
                    binding.imageviewZoom.visibility = View.VISIBLE
                    binding.scrollView.visibility = View.INVISIBLE
                    glide.load(it).into(binding.imageviewZoom)
                }
            }catch (e: Exception){
                println(e.message)
            }
        }

        binding.imageviewZoom.setOnClickListener {
            if(it.isVisible){
                it.visibility = View.GONE
                binding.scrollView.visibility = View.VISIBLE
            }
        }

        binding.fabCreateRouteDetails.setOnClickListener {
            if(place != null){
                val selectedRoute = SelectedRoute(
                        place!!.name,
                        Location("").also {
                            it.latitude = place!!.lat
                            it.longitude = place!!.lon
                        }
                )
                findNavController().navigate(DetailsFragmentDirections.actionDetailsFragmentToMapFragment("tourism.sights", selectedRoute))
            }
        }

        firestore = FirebaseFirestore.getInstance()
        binding.includedCommentLayout.toCommentButton.setOnClickListener {
            //val commentDate = DateFormat.format("dd/MM/yyyy", comment.getDate()!!)
            if(place != null){
                val comment = Comment(
                    "username",
                    "comment text",
                    System.currentTimeMillis(),
                    2,
                    1,
                    4.5f
                )

                val place_ = HashMap<String, Any>()
                place_["placeName"] = place!!.name
                place_["rating"] = 4.5f

                val ref = firestore!!.collection("Places")
                    .document(place!!.placeId)

                ref.set(place_)
                ref.collection("Comments")
                    .add(comment)
                    .addOnSuccessListener {
                        Toast.makeText(context, "yorum eklendi", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "hata: ${it.localizedMessage}}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        observe()
    }

    private fun bind(place: com.sedat.travelassistant.model.Properties) {
        binding.nameText.text = place.name
    }

    private fun observe(){
        viewModel.imageList.observe(viewLifecycleOwner, Observer {
            if(it != null)
                imagesAdapter.images = it.value
            else
                imagesAdapter.images = listOf()
        })
        viewModel.detailInfo.observe(viewLifecycleOwner, Observer {
            if(it != null && !(it.equals(""))){
                binding.detailText.text = it
            }
            else{
                binding.detailText.text = getString(R.string.not_found_info)
            }
        })
    }

    private fun getImages(query: String){
        //resimler isim ile aranır ve arama yapmak için string içindeki boşluklara + karakteri eklendi.
        val queryNew = query.replace("\\s".toRegex(), "+").toLowerCase()
        viewModel.getPlaceImages(queryNew)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
        viewModel.clearData()
    }
}
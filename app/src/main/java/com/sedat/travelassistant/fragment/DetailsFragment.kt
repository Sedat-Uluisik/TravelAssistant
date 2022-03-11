package com.sedat.travelassistant.fragment

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firestore.v1.DocumentTransform
import com.sedat.travelassistant.LoginOrRegister
import com.sedat.travelassistant.R
import com.sedat.travelassistant.adapter.CommentAdapter
import com.sedat.travelassistant.adapter.ImagesAdapter
import com.sedat.travelassistant.databinding.CommentItemLayoutBinding
import com.sedat.travelassistant.databinding.CommentLayoutBinding
import com.sedat.travelassistant.databinding.FragmentDetailsBinding
import com.sedat.travelassistant.model.Properties
import com.sedat.travelassistant.model.firebase.Comment
import com.sedat.travelassistant.model.selectedroute.SelectedRoute
import com.sedat.travelassistant.util.firebasereferences.References.users
import com.sedat.travelassistant.viewmodel.DetailsFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.reflect.Method
import javax.inject.Inject

@AndroidEntryPoint
class DetailsFragment @Inject constructor(
        private val imagesAdapter: ImagesAdapter,
        private val commentAdapter: CommentAdapter,
        private val glide: RequestManager,
        private val dbFirestore: FirebaseFirestore,
        private val auth: FirebaseAuth
) : Fragment() {

    private var fragmentBinding: FragmentDetailsBinding ?= null
    private val binding get() = fragmentBinding!!

    private lateinit var viewModel: DetailsFragmentViewModel
    private lateinit var oldCommentForUpdate: Comment

    private var isUpdateComment: Boolean = false

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
        //auth = Firebase.auth

        var place: com.sedat.travelassistant.model.Properties ?= null
        arguments?.let {
            place = DetailsFragmentArgs.fromBundle(it).place
        }
        if (place != null){
            bind(place!!)
            getImages(place!!.name)
            viewModel.getInfo(place!!.name)

            viewModel.checkLocationInDatabase(place!!)
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

        binding.includedCommentLayout.recyclerViewComment.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.includedCommentLayout.recyclerViewComment.adapter = commentAdapter

        commentAdapter.likeDislikeButton { commentId, type ->
            if(place != null && auth.currentUser != null){
                if(type == 1) //like button click
                    viewModel.likeOrDislikeButtonClick(place!!.placeId, commentId, auth.currentUser!!.uid, true)
                else if(type == 2) //dislike button click
                    viewModel.likeOrDislikeButtonClick(place!!.placeId, commentId, auth.currentUser!!.uid, false)
            }
        }
        commentAdapter.moreButtonClickListener { comment, commentView ->
            if(place != null && auth.currentUser != null && comment.userId == auth.currentUser!!.uid){
                oldCommentForUpdate = comment
                showPopupMenuForComment(place!!.placeId, comment, commentView)
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

        binding.includedCommentLayout.toCommentButton.setOnClickListener {
            if(place != null){
                if(isUpdateComment){ //update comment

                    val oldRating = oldCommentForUpdate.rating

                    //change new values for comment
                    oldCommentForUpdate.rating = binding.includedCommentLayout.ratingBarComment.rating
                    oldCommentForUpdate.Comment = binding.includedCommentLayout.commentEditText.text.toString()

                    viewModel.updateComment(place!!.placeId, oldCommentForUpdate){ bool ->
                        if(bool){
                            viewModel.updateRating(place!!.placeId, oldRating, binding.includedCommentLayout.ratingBarComment.rating)
                            clearCommentView()
                            Toast.makeText(requireContext(), getString(R.string.comment_updated), Toast.LENGTH_SHORT).show()
                        }
                        else{
                            clearCommentView()
                            Toast.makeText(requireContext(), getString(R.string.error_comment_not_updated), Toast.LENGTH_SHORT).show()
                        }
                    }
                }else //post comment
                    postComment(place!!)
            }
        }

        binding.includedCommentLayout.cancelUpdateCommentBtn.setOnClickListener {
            clearCommentView()
        }

        observe()
    }

    private fun bind(place: Properties) {
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
        //clear comment view when comment is posted
        viewModel.isDataSend.observe(viewLifecycleOwner){
            if(it)
                clearCommentView()
        }
        //get comment list
        viewModel.commentList.observe(viewLifecycleOwner){

            if(it.isNotEmpty()){
                binding.includedCommentLayout.commentNotFound.visibility = View.GONE
                commentAdapter.commentList = it
            }
            else{
                commentAdapter.commentList = listOf()
                binding.includedCommentLayout.commentNotFound.visibility = View.VISIBLE
            }
        }
        //get rating
        viewModel.rating.observe(viewLifecycleOwner){
            binding.ratingBarPlaceDetails.rating = it
        }
    }

    private fun clearCommentView(){
        binding.includedCommentLayout.commentEditText.setText("")
        binding.includedCommentLayout.ratingBarComment.rating = 0.0f
        if(isUpdateComment){
            isUpdateComment = false
            binding.includedCommentLayout.cancelUpdateCommentBtn.visibility = View.GONE
        }
    }

    private fun getImages(query: String){
        //resimler isim ile aranır ve arama yapmak için string içindeki boşluklara + karakteri eklendi.
        //val queryNew = query.replace("\\s".toRegex(), "+").toLowerCase()
        val queryNew = query.replace("\\s".toRegex(), "+").lowercase()
        viewModel.getPlaceImages(queryNew)
    }

    private fun showPopupMenuForComment(placeId: String, comment: Comment, view: View){
        val context = ContextThemeWrapper(requireContext(), R.style.PopupMenuTheme) //popup menü dizaynı eklendi
        val popup = PopupMenu(context, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.popup_menu_for_comment_item, popup.menu)

        popup.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.update_comment ->{
                    //viewModel.updateComment(placeId, commentId, userId)
                    getCommentForUpdate(comment)
                    return@setOnMenuItemClickListener true
                }
                R.id.delete_comment ->{
                    viewModel.deleteComment(placeId, comment.commentId, comment.userId)
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }
        }

        //popup menüdeki ikonların görünmesi için gerekli.
        try {
            val fields = popup.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field[popup]
                    val classPopupHelper =
                        Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons: Method = classPopupHelper.getMethod(
                        "setForceShowIcon",
                        Boolean::class.javaPrimitiveType
                    )
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }finally {
            popup.show()
        }
    }

    private fun postComment(place: Properties){
        if(auth.currentUser != null){
            val commentView = binding.includedCommentLayout
            if(commentView != null){

                dbFirestore.collection(users).document(auth.uid.toString())  //get username for comment
                    .get()
                    .addOnSuccessListener {
                        if(it != null){
                            val username = it.get("username").toString()
                            val comment_ = commentView.commentEditText.text.toString()
                            val rating = commentView.ratingBarComment.rating

                            if(comment_.isNotEmpty() && rating > 0.0){
                                val comment = Comment(
                                    comment_,
                                    "",
                                    System.currentTimeMillis(),
                                    0,
                                    0,
                                    rating,
                                    username,
                                    auth.currentUser!!.uid
                                )

                                viewModel.postComment(place, comment)
                            }else{
                                Toast.makeText(requireContext(), getString(R.string.please_fill_in_all_fields), Toast.LENGTH_LONG).show()
                            }
                        }
                    }


            }
        }else{
            Snackbar.make(
                binding.root,
                getString(R.string.error_for_account),
                Snackbar.LENGTH_INDEFINITE
            ).setAction(getString(R.string.login)){
                activity?.let {
                    val intent = Intent(activity, LoginOrRegister::class.java)
                    it.startActivity(intent)
                }
            }.show()
        }
    }

    private fun getCommentForUpdate(comment: Comment){
        isUpdateComment = true
        binding.includedCommentLayout.cancelUpdateCommentBtn.visibility = View.VISIBLE
        binding.includedCommentLayout.ratingBarComment.rating = comment.rating
        binding.includedCommentLayout.commentEditText.setText(comment.Comment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
        viewModel.clearData()
        commentAdapter.commentList = listOf()
    }
}
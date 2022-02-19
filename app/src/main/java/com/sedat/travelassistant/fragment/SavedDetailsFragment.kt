package com.sedat.travelassistant.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.sedat.travelassistant.R
import com.sedat.travelassistant.adapter.SavedImagesAdapter
import com.sedat.travelassistant.databinding.FragmentSavedDetailsBinding
import com.sedat.travelassistant.listener.SavedDetailsFragmentClickListener
import com.sedat.travelassistant.model.room.ImagePath
import com.sedat.travelassistant.model.room.SavedPlace
import com.sedat.travelassistant.model.selectedroute.SelectedRoute
import com.sedat.travelassistant.util.SaveImageToFile
import com.sedat.travelassistant.viewmodel.SavedPlacesViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SavedDetailsFragment @Inject constructor(
        private val glide: RequestManager
) : Fragment(), SavedDetailsFragmentClickListener {

    private lateinit var databinding: FragmentSavedDetailsBinding
    private lateinit var viewModel: SavedPlacesViewModel

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraPermissions: Array<String>

    @Inject
    lateinit var savedImagesAdapter: SavedImagesAdapter

    private var isUpdate: Boolean = false
    private var currentPhotoPath = "" //kameradan çekilen resmi almak için kullanılıyor.
    private var pickCamera: Boolean = false
    private var userLocation: Location ?= null //yeni yer kaydetmek için kullanılıyor.
    private var isAddNewLocation: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerLauncher()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        databinding = DataBindingUtil.inflate(inflater, R.layout.fragment_saved_details, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(SavedPlacesViewModel::class.java)

        return databinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            val savedPlace = SavedDetailsFragmentArgs.fromBundle(it).savedPlace
            isUpdate = SavedDetailsFragmentArgs.fromBundle(it).isUpdate
            isAddNewLocation = SavedDetailsFragmentArgs.fromBundle(it).isAddNewLocation
            userLocation = SavedDetailsFragmentArgs.fromBundle(it).location

            databinding.isUpdate = isUpdate
            databinding.clickListener = this

            if(savedPlace != null && !isAddNewLocation){
                viewModel.getImages(savedPlace.rowid)
                databinding.savedPlace = savedPlace
            }

            when {
                isUpdate -> {
                    databinding.fabAddImage.visibility = View.VISIBLE
                    databinding.updateButton.visibility = View.VISIBLE
                    databinding.fabCreateRouteSavedDetails.visibility = View.GONE

                    //ItemTouchHelper(swipeRecyclerItemForDelete).attachToRecyclerView(databinding.recyclerSavedPlaceImages)
                }
                isAddNewLocation -> {
                    databinding.fabCreateRouteSavedDetails.visibility = View.GONE
                    if(userLocation != null){
                        clearUIForAddNewLocation()
                        databinding.isUpdate = true  //edittext alanlarının aktif olması için tru yapıldı.
                    }else{
                        Toast.makeText(requireContext(), getString(R.string.location_not_found), Toast.LENGTH_LONG).show()
                        //Hata oluşturmak için yanlış data gönderildi.
                        val emptySavedPlace = SavedPlace(-1,"", "", "", "", "", "", "", -1.0, -1.0)
                        databinding.savedPlace = emptySavedPlace
                        databinding.isUpdate = false
                    }
                }
                else -> {
                    databinding.fabAddImage.visibility = View.GONE
                    databinding.updateButton.visibility = View.GONE
                    databinding.fabCreateRouteSavedDetails.visibility = View.VISIBLE
                }
            }
        }

        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        databinding.recyclerSavedPlaceImages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        databinding.recyclerSavedPlaceImages.adapter = savedImagesAdapter


        databinding.fabAddImage.setOnClickListener {
            chooseImageImportMethod(view)
        }

        savedImagesAdapter.onImageClickListener {
            databinding.scrollView.visibility = View.GONE
            databinding.fabCreateRouteSavedDetails.visibility = View.GONE
            databinding.imageViewZoom.visibility = View.VISIBLE

            if(isUpdate)
                databinding.fabAddImage.visibility = View.GONE

            glide.load(it).into(databinding.imageViewZoom)
        }
        savedImagesAdapter.onDeleteButtonClickListener {
            if(isUpdate){
                if(viewModel.imagesForSave.size > 0){
                    for(i in viewModel.imagesForSave){
                        if(it.image_path == i.image_path){
                            viewModel.imagesForSave.remove(i)
                            savedImagesAdapter.notifyDataSetChanged()
                            break
                        }
                    }
                }else{
                    viewModel.deleteImagesFromRoom(it.id, it.root_id) //room dan resim yolu silinir.
                    SaveImageToFile().delete(requireContext(), it.image_path)  //dosya dan resim silinir.
                }
            }else
                Toast.makeText(requireContext(), "silme işlemi güncelleme modunda yapılabilir", Toast.LENGTH_LONG).show()
        }

        databinding.imageViewZoom.setOnClickListener {
            databinding.scrollView.visibility = View.VISIBLE
            if(!isUpdate)
                databinding.fabCreateRouteSavedDetails.visibility = View.VISIBLE
            it.visibility = View.GONE

            if(isUpdate)
                databinding.fabAddImage.visibility = View.VISIBLE
        }

        observe()
    }

    private fun observe(){
        viewModel.imagesPaths.observe(viewLifecycleOwner, {
            it?.let { list->
                if(list.isNotEmpty()){

                    //println(list.size)

                    savedImagesAdapter.imageList = list
                    databinding.imagesNotFount.visibility = View.GONE
                    databinding.recyclerSavedPlaceImages.visibility = View.VISIBLE
                }
                else{
                    databinding.imagesNotFount.visibility = View.VISIBLE
                    databinding.recyclerSavedPlaceImages.visibility = View.GONE
                }
            }
        })
    }

    /*private val swipeRecyclerItemForDelete = object :ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.UP){
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.layoutPosition
            val imagePath = savedImagesAdapter.imageList[position]

            if(isUpdate){
                if(viewModel.imagesForSave.size > 0){
                    for(i in viewModel.imagesForSave){
                        if(imagePath.image_path == i.image_path){
                            viewModel.imagesForSave.remove(i)
                            savedImagesAdapter.notifyDataSetChanged()
                            break
                        }
                    }
                }else{
                    viewModel.deleteImagesFromRoom(imagePath.id, imagePath.root_id) //room dan resim yolu silinir.
                    SaveImageToFile().delete(requireContext(), imagePath.image_path)  //dosya dan resim silinir.
                    viewModel.getImages(imagePath.root_id)
                }
            }

        }

    }*/

    private fun clearUIForAddNewLocation(){
        databinding.imagesNotFount.visibility = View.VISIBLE
        databinding.recyclerSavedPlaceImages.visibility = View.GONE
        databinding.fabAddImage.visibility = View.VISIBLE
        databinding.updateButton.visibility = View.VISIBLE
        //ItemTouchHelper(swipeRecyclerItemForDelete).attachToRecyclerView(databinding.recyclerSavedPlaceImages)

        viewModel.clearData()

        if(userLocation != null){
            val emptySavedPlace = SavedPlace(0,"", "", "", "", "", "", "", userLocation!!.latitude, userLocation!!.longitude)
            databinding.savedPlace = emptySavedPlace
        }
    }

    private fun chooseImageImportMethod(view: View){
        val alertView = LayoutInflater.from(requireContext()).inflate(R.layout.alert_dialog_gallery_or_camera_for_intent, null)
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setCancelable(true)
        alertDialog.setView(alertView)

        val dialog: AlertDialog = alertDialog.create()
        if(dialog.window != null)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        dialog.show()

        alertView.findViewById<Button>(R.id.button_gallery).setOnClickListener {
            selectImageFromGallery(view)
            dialog.dismiss()
        }
        alertView.findViewById<Button>(R.id.button_camera).setOnClickListener {
            if(!checkCameraPermission()){
                //kamera izni yok (permission not granted)
                requestCameraPermission()
            }else{
                //izin var (permission already granted)
                pickFromCamera()
                dialog.dismiss()
            }
        }
    }

    private fun selectImageFromGallery(view: View){
        activity?.let {
            if(ContextCompat.checkSelfPermission(requireActivity().applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Snackbar.make(view, "izin gerekli", Snackbar.LENGTH_LONG).setAction(getString(R.string.give_permission),
                            View.OnClickListener {
                                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }).show()
                }else{
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }else{
                val intent = Intent()
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.action = Intent.ACTION_GET_CONTENT
                activityResultLauncher.launch(Intent.createChooser(intent, "select image"))
            }
        }
    }
    private fun pickFromCamera(){
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                }catch (e: IOException){
                    println(e.message)
                    null
                }

                photoFile?.also {
                    val photoUri: Uri = FileProvider.getUriForFile(
                            requireActivity(),
                            "com.sedat.travelassistant.fileprovider",
                            it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    pickCamera = true
                    activityResultLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    private fun createImageFile(): File{  //Kameradan alınan resmi kaydetmek için kullanılıyor.
        //val storageDir = File(context.getExternalFilesDir("/"), "pictures")
        val storageDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
                SaveImageToFile().randomUid(),
                ".jpg",
                storageDir
        ).apply {
            currentPhotoPath = absolutePath.toString()
        }
    }

    private fun checkCameraPermission(): Boolean{
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestCameraPermission(){
        ActivityCompat.requestPermissions(requireActivity(), cameraPermissions, 11)
    }

    private fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){ result ->
            if(result.resultCode == AppCompatActivity.RESULT_OK){
                val intentFromResult = result.data

                if(intentFromResult != null){

                    databinding.imagesNotFount.visibility = View.GONE
                    databinding.recyclerSavedPlaceImages.visibility = View.VISIBLE

                    if(intentFromResult.clipData != null){   //birden fazla resim seçildi.

                        val count = intentFromResult.clipData!!.itemCount

                        try {
                            for (i in 0 until count){
                                val uri = intentFromResult.clipData!!.getItemAt(i).uri

                                viewModel.imagesForSave.add(ImagePath(0, 0, uri.toString()))
                            }
                            if(viewModel.imagesForSave.size > 0){
                                savedImagesAdapter.imageList = viewModel.imagesForSave
                                savedImagesAdapter.notifyDataSetChanged()
                            }
                        }catch (e:Exception){
                            e.printStackTrace()
                            println(e.message)
                        }
                    }else if(intentFromResult.data != null){ //tek resim seçildi.

                        val uri = intentFromResult.data!!

                        try {

                            viewModel.imagesForSave.add(ImagePath(0, 0, uri.toString()))

                            if(viewModel.imagesForSave.size > 0){
                                savedImagesAdapter.imageList = viewModel.imagesForSave
                                savedImagesAdapter.notifyDataSetChanged()
                            }
                        }catch (e: Exception){
                            println(e.message)
                        }
                    }else if(currentPhotoPath.isNotEmpty() && pickCamera){

                        val file = File(currentPhotoPath)
                        viewModel.imagesForSave.add(ImagePath(0, 0, Uri.fromFile(file).toString()))

                        if(viewModel.imagesForSave.size > 0){
                            savedImagesAdapter.imageList = viewModel.imagesForSave
                            savedImagesAdapter.notifyDataSetChanged()
                        }
                    }
                }else if(currentPhotoPath.isNotEmpty() && currentPhotoPath != null){

                    databinding.imagesNotFount.visibility = View.GONE
                    databinding.recyclerSavedPlaceImages.visibility = View.VISIBLE

                    val file = File(currentPhotoPath)
                    viewModel.imagesForSave.add(ImagePath(0, 0, Uri.fromFile(file).toString()))

                    if(viewModel.imagesForSave.size > 0){

                        for (i in viewModel.imagesForSave)
                            println(i.image_path)

                        savedImagesAdapter.imageList = viewModel.imagesForSave
                        savedImagesAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ){
            if(it){
                //galeriye git
                //pick multiple images
                val intent = Intent()
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.action = Intent.ACTION_GET_CONTENT
                activityResultLauncher.launch(Intent.createChooser(intent, "select image"))

            }else{
                Toast.makeText(requireContext(), getString(R.string.permission_needed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun updateButtonClick(savedPlace: SavedPlace) {
        if(savedPlace.rowid != -1 && savedPlace.lat != -1.0 && savedPlace.lon != -1.0){
            val savedPlaceNew = SavedPlace(
                    savedPlace.rowid,
                    databinding.placeName.text.toString().capitalize(Locale.ROOT),  //capitalize ile ilk harf büyük yapıldı.
                    databinding.city.text.toString().capitalize(Locale.ROOT),
                    databinding.district.text.toString().capitalize(Locale.ROOT),
                    databinding.address.text.toString().capitalize(Locale.ROOT),
                    databinding.state.text.toString().capitalize(Locale.ROOT),
                    databinding.street.text.toString().capitalize(Locale.ROOT),
                    databinding.suburb.text.toString().capitalize(Locale.ROOT),
                    savedPlace.lat,
                    savedPlace.lon
            )

            if(isUpdate && !isAddNewLocation){
                viewModel.updatePlace(requireContext(), savedPlaceNew, pickCamera)
            }else if (isAddNewLocation && userLocation != null){
                viewModel.addNewPlace(requireContext(), savedPlaceNew, pickCamera)
            }
            savedImagesAdapter.imageList = listOf()
            databinding.isUpdate = false
            isUpdate = false
            databinding.fabAddImage.visibility = View.GONE
            databinding.updateButton.visibility = View.GONE
        }else
            Toast.makeText(requireContext(), getString(R.string.error_location), Toast.LENGTH_SHORT).show()

        viewModel.getImages(savedPlace.rowid)
        databinding.savedPlace = savedPlace
    }

    override fun fabCreateRouteButtonClick(savedPlace: SavedPlace) {
        val selectedRoute = SelectedRoute(
                savedPlace.name,
                Location("").also {
                    it.latitude = savedPlace.lat
                    it.longitude = savedPlace.lon
                }
        )

        findNavController().navigate(SavedDetailsFragmentDirections.actionSavedDetailsFragmentToMapFragment("tourism.sights", selectedRoute))
    }
}
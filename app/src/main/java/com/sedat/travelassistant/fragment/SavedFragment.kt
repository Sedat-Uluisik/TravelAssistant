package com.sedat.travelassistant.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.sedat.travelassistant.R
import com.sedat.travelassistant.adapter.SavedPlacesAdapter
import com.sedat.travelassistant.databinding.AlertDialogSyncToCloudBinding
import com.sedat.travelassistant.databinding.FragmentSavedBinding
import com.sedat.travelassistant.model.room.SavedPlace
import com.sedat.travelassistant.util.SaveImageToFile
import com.sedat.travelassistant.viewmodel.SavedPlacesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import javax.inject.Inject

@AndroidEntryPoint
class SavedFragment : Fragment() {

    private var fragmentBinding: FragmentSavedBinding ?= null
    private val binding get() = fragmentBinding!!

    private lateinit var viewModel: SavedPlacesViewModel

    @Inject
    lateinit var savedPlacesAdapter: SavedPlacesAdapter
    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(SavedPlacesViewModel::class.java)

        binding.recyclerSaved.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSaved.adapter = savedPlacesAdapter
        ItemTouchHelper(swipeRecyclerItemForDelete).attachToRecyclerView(binding.recyclerSaved)

        savedPlacesAdapter.onSavedItemClickListener {
            findNavController().navigate(SavedFragmentDirections.actionSavedFragmentToSavedDetailsFragment(
                    it,
                    false,
                    false,
                    null
            ))
        }

        savedPlacesAdapter.onMoreButtonClickListener {savedPlace, view_ ->
            showPopupMenu(savedPlace, view_)
        }

        var job: Job?= null
        binding.searchBar.addTextChangedListener {
            job?.cancel()
            job = lifecycleScope.launch {
                delay(400)
                viewModel.search(it)
            }
        }

        viewModel.getPlaces()
        observe()

        binding.syncBtn.setOnClickListener { //save locations to firebase db
            if(auth.currentUser != null)
                showSyncOptions()
            else
                Toast.makeText(requireContext(), "Bulut işlemleri için giriş yapmanız gerekir", Toast.LENGTH_LONG).show()
        }

        binding.refreshLayoutSaved.setOnRefreshListener {
            viewModel.getPlaces()
            binding.refreshLayoutSaved.isRefreshing = false
            savedPlacesAdapter.notifyDataSetChanged()
        }
    }

    private fun observe(){

        //herşeyi silip farklı kayıtları getirken eksik gösteriyor ama başka sayfadan gelince gösteriyor.

        viewModel.placeList.observe(viewLifecycleOwner) {
            it?.let { list ->
                if (list.isNotEmpty())
                    savedPlacesAdapter.placeList = list
                else
                    savedPlacesAdapter.placeList = listOf()
                //savedPlacesAdapter.refreshData()
            }
        }

        viewModel.imageList.observe(viewLifecycleOwner) {
            if(it.isNotEmpty()){
                savedPlacesAdapter.imageList.clear()
                savedPlacesAdapter.imageList.addAll(it)
                //savedPlacesAdapter.refreshData()
            }
        }
    }

    private val swipeRecyclerItemForDelete = object :ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT){
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.layoutPosition
            val selectedPlace = savedPlacesAdapter.placeList[position]
            viewModel.deleteSavedPlace(selectedPlace.lat, selectedPlace.lon)
            //viewModel.getPlaces()

            //Dosya içindeki tüm resimleri silmek için kullanılıyor.
            /*viewModel.getImages("${selectedPlace.lat}_${selectedPlace.lon}")
            viewModel.imagesPaths.observe(viewLifecycleOwner) {
                it?.let { list ->
                    if (list.isNotEmpty()) {
                        list.forEach { image ->
                            SaveImageToFile().delete(requireContext(), image.image_path)
                        }
                    }
                }
            }*/

            //delete all pictures in file
            SaveImageToFile().deleteOnlyPicturesOfLatLon(requireContext(), "${selectedPlace.lat}_${selectedPlace.lon}")
            viewModel.deleteAllImagesPathsWithLatLonFromRoom("${selectedPlace.lat}_${selectedPlace.lon}")

            //viewModel.clearData()


             var job: Job?= null
            job?.cancel()
            job = lifecycleScope.launch {
                delay(400)
                viewModel.getPlaces()
                job?.cancel()
            }



        }
    }

    private fun showPopupMenu(savedPlace: SavedPlace, view: View){
        val context = ContextThemeWrapper(requireContext(), R.style.PopupMenuTheme)  //popup menü text dizaynı eklendi
        val popup = PopupMenu(context, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.popup_menu, popup.menu)

        popup.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.update ->{
                    findNavController().navigate(SavedFragmentDirections.actionSavedFragmentToSavedDetailsFragment(savedPlace, true))
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }
        }

        //popup menüdeki ikonların görünmesi için kullanıldı.
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

    private fun showSyncOptions(){
        val view = AlertDialogSyncToCloudBinding.inflate(LayoutInflater.from(requireContext()))
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setCancelable(true)
        alertDialog.setView(view.root)

        val dialog: AlertDialog = alertDialog.create()
        if(dialog.window != null)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        dialog.show()

        var download = 0
        view.radioGroupDownload.setOnCheckedChangeListener { radioGroup, id ->
            when(id){
                R.id.radio_different_download ->{
                    download = 1
                }
                R.id.radio_delete_download ->{
                    download = 2
                }
            }
        }

        view.cloudDownloadBtn.setOnClickListener {
            when (download) {
                1 -> viewModel.saveDifferentUserSavedLocations(auth.currentUser!!.uid)
                2 -> viewModel.removeOldLocationsToRoomAndSaveNewLocationsFromFirebase(auth.currentUser!!.uid)
                else -> println("bir seçim yapınız")
            }
        }

        var upload = 0
        view.radioGroupUpload.setOnCheckedChangeListener { radioGroup, id ->
            when(id){
                R.id.radio_different_upload ->{
                    upload = 1
                }
                R.id.radio_delete_upload ->{
                    upload = 2
                }
            }
        }
        view.cloudUploadBtn.setOnClickListener {
            when (upload) {
                1 -> viewModel.saveDifferentLocationsToFirebase(auth.currentUser!!.uid)
                2 -> viewModel.saveLocationsToFirebaseAndDeleteOldLocations(auth.currentUser!!.uid)
                else -> println("bir seçim yapınız")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        fragmentBinding = null
    }
}
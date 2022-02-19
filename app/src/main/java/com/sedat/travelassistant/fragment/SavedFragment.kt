package com.sedat.travelassistant.fragment

import android.os.Bundle
import android.view.*
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
import com.sedat.travelassistant.R
import com.sedat.travelassistant.adapter.SavedPlacesAdapter
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

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
    }

    private fun observe(){
        viewModel.placeList.observe(viewLifecycleOwner, Observer {
            it?.let { list ->
                if(list.isNotEmpty())
                    savedPlacesAdapter.placeList = list
                else
                    savedPlacesAdapter.placeList = listOf()
                savedPlacesAdapter.refreshData()
            }
        })

        viewModel.imageList.observe(viewLifecycleOwner, Observer {
            savedPlacesAdapter.imageList.clear()
            savedPlacesAdapter.imageList.addAll(it)
            savedPlacesAdapter.refreshData()
        })
    }

    private val swipeRecyclerItemForDelete = object :ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT){
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.layoutPosition
            val selectedPlace = savedPlacesAdapter.placeList[position]
            viewModel.deleteSavedPlace(selectedPlace.lat, selectedPlace.lon)
            viewModel.getPlaces()

            //Dosya içindeki tüm resimleri silmek için kullanılıyor.
            viewModel.getImages(selectedPlace.rowid)
            viewModel.imagesPaths.observe(viewLifecycleOwner, {
                it?.let { list ->
                    if(list.isNotEmpty()){
                        list.forEach { image ->
                            SaveImageToFile().delete(requireContext(), image.image_path)
                        }
                    }
                }
            })
            viewModel.deleteAllImagesWithRootId(selectedPlace.rowid) //room dan tüm resimler silinir.
            viewModel.clearData()
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

    override fun onDestroyView() {
        super.onDestroyView()

        fragmentBinding = null
    }
}
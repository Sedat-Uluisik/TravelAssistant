package com.sedat.travelassistant.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Editable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sedat.travelassistant.model.room.ImagePath
import com.sedat.travelassistant.model.room.SavedPlace
import com.sedat.travelassistant.repo.PlaceRepositoryInterface
import com.sedat.travelassistant.util.SaveImageToFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedPlacesViewModel @Inject constructor(
        private val repository: PlaceRepositoryInterface,
        @ApplicationContext private val application: Context
): BaseViewModel(application as Application) {

    private val places = MutableLiveData<List<SavedPlace>>()
    val placeList: LiveData<List<SavedPlace>>
        get() = places
    fun getPlaces(){
        launch {
            places.value = listOf()
            val data = repository.getPlacesFromRoom()
            if(data != null && data.isNotEmpty()){
                getOneImageFromSavedPlaces(data)
            }
        }
    }

    fun deleteSavedPlace(lat: Double, lon: Double){
        launch {
            repository.deleteSavedPlaceFromRoom(lat, lon)
        }
    }

    val imagesForSave = mutableListOf<ImagePath>()
    fun updatePlace(context: Context, savedPlace: SavedPlace, pickCamera: Boolean){
        launch {
            repository.updatePlaceFromRoom(savedPlace)

            if(imagesForSave.size > 0)
                saveImagePaths(context, savedPlace.rowid, pickCamera, "${savedPlace.lat}_${savedPlace.lon}")
        }
    }

    fun addNewPlace(context: Context, savedPlace: SavedPlace, pickCamera: Boolean){
        launch {
            repository.savePlaceForRoom(savedPlace)
            val currentPlace = repository.getPlaceWithLatLonFromRoom(savedPlace.lat, savedPlace.lon)

            if(imagesForSave.size > 0)
                saveImagePaths(context, currentPlace.rowid, pickCamera, "${savedPlace.lat}_${savedPlace.lon}")
        }
    }

    private suspend fun saveImagePaths(context: Context, root_id: Int, pickCamera: Boolean, latLong: String){
        /*
            Kameradan resim seçince resim dosyasını otomatik otuşurur.
            Galeriden resim seçince resmi dosyaya manuel olarak kaydetmemiz gerekir.
            Eğer pickCamera doğru ise yeniden dosya oluşturmadan direk url si alınıp kaydedilir, aksi takdirde aynı resimden iki tane kaydedilmiş olur.
            Eğer pickCamera yanlış ise resim galeriden seçilmiş demektir ve dosya içine manuel kaydedilmesi gerekir.
             */

        //resim yolu room a kaydedilir ve resim dosya içine kaydedilir.
        for (i in imagesForSave){
            val newImagePath = ImagePath(
                0,
                root_id,
                latLong,
                if(pickCamera) i.image_path else SaveImageToFile().save(context, convertUriToBitmap(context, Uri.parse(i.image_path)), latLong).toString()
            )

            repository.saveImageForRoom(newImagePath)
        }

        imagesForSave.clear()
        getImages(latLong)
    }

    private fun convertUriToBitmap(context: Context, uri: Uri): Bitmap {
        return if(Build.VERSION.SDK_INT >= 28){
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            var imageBitmap = ImageDecoder.decodeBitmap(source)
            imageBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)  //sdk>=28 de bitmap uygulanırken hata vermemesi için.
            imageBitmap
        }else{
            val imageBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            imageBitmap
        }
    }

    fun deleteImagesFromRoom(id: Int, root_id: Int, latLong: String){
        launch {
            repository.deleteImagesFromRoom(id, root_id)
            getImages(latLong)
        }
    }

    fun deleteAllImagesWithRootId(root_id: Int){  //root_id ye sahip tüm resimleri siler.
        launch {
            repository.deleteAllImagesWithRootId(root_id)
        }
    }

    private val paths = MutableLiveData<List<ImagePath>>()
    val imagesPaths: LiveData<List<ImagePath>>
        get() = paths
    fun getImages(latLong: String){
        launch {
            val paths_ = repository.getSavedPlaceImages(latLong)
            paths.value = paths_
        }
    }

    val imageList = MutableLiveData<List<ImagePath>>()
    private suspend fun getOneImageFromSavedPlaces(savedPlaceList: List<SavedPlace>){  //kaydedilenler sayfasında eğer resim eklenmiş ise 1 tanesini göstermek için.
        val images = repository.getOneImageFromSavedPlaces(savedPlaceList.map { "${it.lat}_${it.lon}" })
        imageList.value = images
        places.value = savedPlaceList
    }

    fun search(query: Editable?){
        launch {
            if(query.isNullOrBlank())
                getPlaces()
            else{  //like ile arama yaparken %query% kullanılır, fts ile arama yapmak için *query* kullanılır.
                repository.fullTextSearch("*$query*").let {
                    places.value = it

                    for (i in it)
                        println(i.name + i.rowid + " " + i.lat + " " + i.lon)
                }
            }
        }
    }

    fun saveLocationsToFirebaseAndDeleteOldLocations(userId: String){
        if(placeList.value != null){
            if (placeList.value!!.isNotEmpty()){
                launch {
                    repository.getAllSavedPlaceImages {
                        repository.saveLocationsToFirebaseAndDeleteOldLocations(placeList.value!!, it, userId)
                    }
                }
            }
        }
    }

    fun saveDifferentLocationsToFirebase(userId: String){
        if(placeList.value != null){
            if(placeList.value!!.isNotEmpty()){
                launch {
                    repository.getAllSavedPlaceImages {
                        repository.saveDifferentLocationsToFirebase(placeList.value!!, it, userId)
                    }
                }
            }
        }
    }

    fun removeOldLocationsToRoomAndSaveNewLocationsFromFirebase(userId: String){
        if(userId.isNotEmpty()){
            repository.getUserSavedLocations(userId) {
                launch {
                    repository.removeOldLocationsToRoomAndSaveNewLocationsFromFirebase(it,userId)
                    getPlaces()
                }
                repository.saveImagesFromFirebaseToFile(userId){ path, latLong->
                    launch {

                        println(path)

                        val imagePath = ImagePath(
                            0,
                            0,
                            latLong,
                            path
                        )
                        repository.saveImageForRoom(imagePath)
                    }
                }
            }
        }
    }

    fun saveDifferentUserSavedLocations(userId: String){
        if(userId.isNotEmpty()){
            repository.getUserSavedLocations(userId){
                launch {
                    repository.saveDifferentUserSavedLocations(it)

                    getPlaces()
                }
            }
        }
    }

    fun clearData(){
        imagesForSave.clear()
        paths.value = listOf()
        imageList.value = listOf()
    }
}
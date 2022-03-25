package com.sedat.travelassistant.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.util.MutableBoolean
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.sedat.travelassistant.R
import com.sedat.travelassistant.model.Place
import com.sedat.travelassistant.model.Properties
import com.sedat.travelassistant.model.visitedlocaions.VisitedLocations
import com.sedat.travelassistant.model.room.Categories
import com.sedat.travelassistant.model.room.SavedPlace
import com.sedat.travelassistant.model.route.Route
import com.sedat.travelassistant.model.selectedroute.SelectedRoute
import com.sedat.travelassistant.repo.PlaceRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MapFragmentViewModel @Inject constructor(
        private val repository: PlaceRepositoryInterface,
        @ApplicationContext private val application: Context
): BaseViewModel(application as Application) {

    private val disposable = CompositeDisposable()

    val markersOnMap = mutableListOf<Marker>() //Seçilen rotadaki marker'a gelindiğinde renginin değişmesi için kullanıldı.
    val selectedRouteCoordinates = mutableListOf<SelectedRoute>()
    var routeStarted = MutableLiveData<Int>(0)
    val mapZoom = MutableLiveData<Float>(15f) //sayfalar arasında gezinirken zoom ayarının bozulmaması için kullanılıyor.
    val firstLocationTakenFromUser = MutableLiveData<Location>()  //Kullanıcı belirli bir mesafeyi geçince yeni verilerin alınması için kullanılıyor.

    private var placesList = MutableLiveData<Place>()
    val places: LiveData<Place>
        get() = placesList
    fun getPlaces(category: String, latLong: String, limit: Int){
       placesList.postValue(null)
        disposable.add(
                repository.getPlace(category, latLong, limit)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object: DisposableSingleObserver<Place>(){
                        override fun onSuccess(t: Place) {
                            placesList.value = t
                        }

                        override fun onError(e: Throwable) {
                            println(e.message)
                        }

                    })
        )
    }

    private var list = MutableLiveData<List<SavedPlace>>()
    val savedPlaces: LiveData<List<SavedPlace>>
        get() = list
    val savedPlaces2 = mutableListOf<SavedPlace>()  //kaydedilen ve internetten alınan marker ları birlikte göstermek için kullanıldı.
    fun getPlacesForRoom(){
        launch {
            val places = repository.getPlacesFromRoom()
            list.value = places
            savedPlaces2.addAll(places)
        }
    }

    private var categoryList = MutableLiveData<List<Categories>>()
    val categories: LiveData<List<Categories>>
        get() = categoryList
    fun getCategories(){
        launch {
            val category_List = repository.getCategories()
            categoryList.value = category_List
        }
    }

    private val coordinates = MutableLiveData<List<List<Double>>>()
    val pointListForRoute: LiveData<List<List<Double>>>
        get() = coordinates
    fun getRoute(context: Context, profile: String){
        if(selectedRouteCoordinates.size > 0){
            disposable.add(
                    repository.getRoute(selectedRouteCoordinates.map {
                        "${it.location.latitude},${it.location.longitude}"
                    }, profile)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(object :DisposableSingleObserver<Route>(){
                                override fun onSuccess(t: Route) {
                                    coordinates.value = t.paths[0].points.coordinates
                                }

                                override fun onError(e: Throwable) {
                                    println(e)
                                }

                            })
            )
        }else{
            Toast.makeText(context, context.getString(R.string.please_select_location), Toast.LENGTH_SHORT).show()
        }
    }

    //gezilen yerlerin marker rengini değiştirmek için kullanılıyor.
    fun saveLocationForVisited(location: LatLng){
        launch {
            val visitedLocations = VisitedLocations(
                0,
                location.latitude,
                location.longitude,
                System.currentTimeMillis()
            )
            repository.saveVisitedLocation(visitedLocations)
        }
    }

    val visitedLocationList = mutableListOf<VisitedLocations>()
    fun getVisitedLocationPoints(){
        launch {
            visitedLocationList.clear()
            visitedLocationList.addAll(repository.getVisitedLocations())
        }
    }

    fun savePlaceForRoom(properties: Properties){
        launch {
            val savedPlace = SavedPlace(
                0,
                if (properties.name.isNullOrEmpty()) "--" else properties.name,
                if (properties.city.isNullOrEmpty()) "--" else properties.city,
                if (properties.district.isNullOrEmpty()) "--" else properties.district,
                if (properties.formatted.isNullOrEmpty()) "--" else properties.formatted,
                if (properties.state.isNullOrEmpty()) "--" else properties.state,
                if (properties.street.isNullOrEmpty()) "--" else properties.street,
                if (properties.suburb.isNullOrEmpty()) "--" else properties.suburb,
                properties.lat,
                properties.lon,
            )
            repository.savePlaceForRoom(savedPlace)
            Toast.makeText(application, application.getString(R.string.save_successful), Toast.LENGTH_SHORT).show()
        }
    }

    fun deletePlaceFromRoom(lat: Double, lon: Double){
        launch {
            repository.deleteSavedPlaceFromRoom(lat, lon)
        }
    }

    suspend fun isFavoritePlace(lat: Double, lon: Double): Boolean{
        val place = repository.getPlaceWithLatLonFromRoom(lat, lon)

        return place != null
    }

    fun clearRoutePoints(){
        //coordinates.value = null
        coordinates.value = listOf()
        selectedRouteCoordinates.clear()
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
        clearRoutePoints()
    }
}
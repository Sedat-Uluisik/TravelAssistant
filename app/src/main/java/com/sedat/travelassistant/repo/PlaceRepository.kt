package com.sedat.travelassistant.repo

import android.content.Context
import com.sedat.travelassistant.api.PlacesApi
import com.sedat.travelassistant.model.Place
import com.sedat.travelassistant.model.visitedlocaions.VisitedLocations
import com.sedat.travelassistant.model.image.PlaceImage
import com.sedat.travelassistant.model.info.Info
import com.sedat.travelassistant.model.room.Categories
import com.sedat.travelassistant.model.room.ImagePath
import com.sedat.travelassistant.model.room.SavedPlace
import com.sedat.travelassistant.model.route.Route
import com.sedat.travelassistant.room.TravelDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Single
import javax.inject.Inject

class PlaceRepository @Inject constructor(
        private val placesApi: PlacesApi,
        private val placesApiForRoute: PlacesApi,
        @ApplicationContext private val context: Context
): PlaceRepositoryInterface {

    val dao = TravelDatabase(context).dao()

    override fun getPlace(category: String, latLong: String, limit: Int): Single<Place> {
        return placesApi.getPlace(category, latLong, limit)
    }

    override fun getImage(query: String): Single<PlaceImage> {
        return placesApi.getImage(query)
    }

    override fun getInfo(url: String, q: String): Single<Info> {
        return placesApi.getInfo(url, q)
    }

    override fun getRoute(routes: List<String>, profile: String): Single<Route> {
        return placesApiForRoute.getRoute(routes, profile)
    }

    override suspend fun getCategories(): List<Categories> {
        return dao.getCategories()
    }

    override suspend fun saveVisitedLocation(visitedLocations: VisitedLocations) {
        dao.saveVisitedLocation(visitedLocations)
    }

    override suspend fun getVisitedLocations(): List<VisitedLocations> {
        return dao.getVisitedLocations()
    }

    override suspend fun savePlaceForRoom(place: SavedPlace) {
        dao.savePlaceForRoom(place)
    }

    override suspend fun getPlaceWithLatLonFromRoom(lat: Double, lon: Double): SavedPlace {
        return dao.getPlaceWithLatLonFromRoom(lat, lon)
    }

    override suspend fun deleteSavedPlaceFromRoom(lat: Double, lon: Double) {
        dao.deleteSavedPlaceFromRoom(lat, lon)
    }

    override suspend fun getPlacesFromRoom(): List<SavedPlace> {
        return dao.getPlacesFromRoom()
    }

    override suspend fun updatePlaceFromRoom(savedPlace: SavedPlace) {
        dao.updatePlaceFromRoom(savedPlace)
    }

    override suspend fun saveImageForRoom(imagePath: ImagePath) {
        dao.saveImageForRoom(imagePath)
    }

    override suspend fun getSavedPlaceImages(root_id: Int): List<ImagePath> {
        return dao.getSavedPlaceImages(root_id)
    }

    override suspend fun getOneImageFromSavedPlaces(root_ids: List<Int>): List<ImagePath> {
        return dao.getOneImageFromSavedPlaces(root_ids)
    }

    override suspend fun deleteImagesFromRoom(id: Int, root_id: Int) {
        dao.deleteImagesFromRoom(id, root_id)
    }

    override suspend fun deleteAllImagesWithRootId(root_id: Int) {
        dao.deleteAllImagesWithRootId(root_id)
    }

    override suspend fun fullTextSearch(query: String): List<SavedPlace> {
        return dao.fullTextSearch(query)
    }
}
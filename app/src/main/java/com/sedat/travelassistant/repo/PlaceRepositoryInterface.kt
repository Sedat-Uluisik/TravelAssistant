package com.sedat.travelassistant.repo

import com.sedat.travelassistant.model.Place
import com.sedat.travelassistant.model.Properties
import com.sedat.travelassistant.model.firebase.Comment
import com.sedat.travelassistant.model.visitedlocaions.VisitedLocations
import com.sedat.travelassistant.model.image.PlaceImage
import com.sedat.travelassistant.model.info.Info
import com.sedat.travelassistant.model.room.Categories
import com.sedat.travelassistant.model.room.ImagePath
import com.sedat.travelassistant.model.room.SavedPlace
import com.sedat.travelassistant.model.route.Route
import io.reactivex.Single
import retrofit2.Call

interface PlaceRepositoryInterface {

    fun getPlace(category: String, latLong: String, limit: Int): Single<Place>
    fun getImage(query: String): Single<PlaceImage>
    fun getInfo(url: String, q: String): Single<Info>
    fun getRoute(routes: List<String>, profile: String): Single<Route>

    //room functions
    suspend fun getCategories(): List<Categories>
    suspend fun saveVisitedLocation(visitedLocations: VisitedLocations)
    suspend fun getVisitedLocations(): List<VisitedLocations>

    suspend fun savePlaceForRoom(place: SavedPlace)
    suspend fun getPlaceWithLatLonFromRoom(lat: Double, lon: Double): SavedPlace
    suspend fun deleteSavedPlaceFromRoom(lat: Double, lon: Double)
    suspend fun getPlacesFromRoom(): List<SavedPlace>
    suspend fun updatePlaceFromRoom(savedPlace: SavedPlace)
    suspend fun saveImageForRoom(imagePath: ImagePath)
    suspend fun getSavedPlaceImages(root_id: Int): List<ImagePath>
    suspend fun getOneImageFromSavedPlaces(root_ids: List<Int>): List<ImagePath>
    suspend fun deleteImagesFromRoom(id: Int, root_id: Int)
    suspend fun deleteAllImagesWithRootId(root_id: Int)
    suspend fun fullTextSearch(query: String): List<SavedPlace>

    //firebase functions
    fun checkLocationInDatabase(place: Properties, listener: (List<Comment>, error: String) -> Unit)
    fun postComment(place: Properties, comment: Comment, callBack: (Boolean) -> Unit)
    fun likeOrDislikeButtonClick(placeId: String, commentId: String, userId: String, likeOrDislike: Boolean)
    fun updateComment(placeId: String, commentId: String, userId: String)
    fun deleteComment(placeId: String, commentId: String, userId: String)
    fun getRating(placeId: String, listener: (Float) -> Unit)
}
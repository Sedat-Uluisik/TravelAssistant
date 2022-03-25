package com.sedat.travelassistant.room

import androidx.room.*
import androidx.room.Dao
import com.sedat.travelassistant.model.visitedlocaions.VisitedLocations
import com.sedat.travelassistant.model.room.Categories
import com.sedat.travelassistant.model.room.ImagePath
import com.sedat.travelassistant.model.room.SavedPlace

@Dao
interface Dao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getCategories(): List<Categories>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVisitedLocation(visitedLocations: VisitedLocations)

    @Query("SELECT * FROM visited_locations")
    suspend fun getVisitedLocations(): List<VisitedLocations>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaceForRoom(place: SavedPlace)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocationListToRoom(vararg place: SavedPlace)

    @Query("DELETE FROM saved_places")
    suspend fun deleteAllSavedLocations()

    @Query("DELETE FROM place_images_paths")
    suspend fun deleteAllImagePaths()

    @Query("SELECT * FROM saved_places WHERE lat = :lat AND lon = :lon")
    suspend fun getPlaceWithLatLonFromRoom(lat: Double, lon: Double): SavedPlace

    @Query("DELETE FROM saved_places WHERE lat =:lat AND lon =:lon")
    suspend fun deleteSavedPlaceFromRoom(lat: Double, lon: Double)

    @Query("SELECT * FROM saved_places")
    suspend fun getPlacesFromRoom(): List<SavedPlace>

    @Update
    suspend fun updatePlaceFromRoom(savedPlace: SavedPlace)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveImageForRoom(imagePath: ImagePath)

    //@Query("SELECT * FROM place_images_paths WHERE root_id =:root_id")
    //suspend fun getSavedPlaceImages(root_id: Int): List<ImagePath>
    @Query("SELECT * FROM place_images_paths WHERE latLong =:latLong")
    suspend fun getSavedPlaceImages(latLong: String): List<ImagePath>

    @Query("SELECT * FROM place_images_paths ")
    suspend fun getAllSavedPlaceImages(): List<ImagePath>

    @Query("SELECT * FROM place_images_paths WHERE latLong IN (:latLongs)")
    suspend fun getOneImageFromSavedPlaces(latLongs: List<String>): List<ImagePath>

    @Query("DELETE FROM place_images_paths WHERE id =:id AND root_id =:root_id")
    suspend fun deleteImagesFromRoom(id: Int, root_id: Int)

    @Query("DELETE FROM place_images_paths WHERE latLong =:latLong")
    suspend fun deleteAllImagesPathsWithLatLonFromRoom(latLong: String)

    //önce oluşturulan sanal tabloda hızlıca arama yapılır ve sonuçlar gerçek tabloda eşleştirilip data alınır.
    @Query("SELECT * FROM saved_places JOIN saved_places_fts ON saved_places.name = saved_places_fts.name OR saved_places.suburb = saved_places_fts.suburb WHERE saved_places_fts MATCH :query")
    suspend fun fullTextSearch(query: String): List<SavedPlace>
}
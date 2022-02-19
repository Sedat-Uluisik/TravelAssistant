package com.sedat.travelassistant.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sedat.travelassistant.converter.ConverterForImage
import com.sedat.travelassistant.model.room.Categories
import com.sedat.travelassistant.model.room.ImagePath
import com.sedat.travelassistant.model.room.SavedPlace
import com.sedat.travelassistant.model.room.SavedPlaceFTS
import com.sedat.travelassistant.model.visitedlocaions.VisitedLocations

@Database(entities = [Categories::class, VisitedLocations::class, SavedPlace::class, SavedPlaceFTS::class, ImagePath::class],exportSchema = false, version = 1)
abstract class TravelDatabase: RoomDatabase() {
    abstract fun dao(): Dao

    companion object{

        @Volatile
        private var instance: TravelDatabase ?= null
        private val lock = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(lock){
            instance ?: Room.databaseBuilder(
                context,
                TravelDatabase::class.java,
                "travel_db",
            ).createFromAsset("database/Travel_Assistant.db").build().also {
                instance = it
            }
        }
    }
}
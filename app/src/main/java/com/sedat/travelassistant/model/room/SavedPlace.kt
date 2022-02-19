package com.sedat.travelassistant.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "saved_places")
data class SavedPlace(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "rowid")
        val rowid: Int,
        val name: String,
        val city: String,
        val district: String,
        val address: String,
        val state: String,
        val street: String,
        val suburb: String,
        val lat: Double,
        val lon: Double
): Serializable

//arama/search işlemi için kullanılacak.
//gerçek tablo değil sanal tablo oluşturup onun üzerinde daha hızlı arama yapabilir.
@Entity(tableName = "saved_places_fts")
@Fts4(contentEntity = SavedPlace::class)
data class SavedPlaceFTS(
        val name: String,
        val city: String,
        val district: String,
        val address: String,
        val state: String,
        val street: String,
        val suburb: String
)

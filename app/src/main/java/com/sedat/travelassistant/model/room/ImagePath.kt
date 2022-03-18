package com.sedat.travelassistant.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "place_images_paths")
data class ImagePath(
        @PrimaryKey(autoGenerate = true)
        val id: Int,
        val root_id: Int,
        val latLong: String, //resimlerin hangi lokasyona ait olduklarını saptamak için kullanılıyor.
        val image_path: String
)

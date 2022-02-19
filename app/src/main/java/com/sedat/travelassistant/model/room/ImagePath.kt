package com.sedat.travelassistant.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "place_images_paths")
data class ImagePath(
        @PrimaryKey(autoGenerate = true)
        val id: Int,
        val root_id: Int,
        val image_path: String
)

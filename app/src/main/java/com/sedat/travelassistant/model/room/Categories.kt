package com.sedat.travelassistant.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Categories(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    val name_tr: String?,
    val name_en: String?,
    val type: Int?,
    val path: String?
)

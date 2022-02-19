package com.sedat.travelassistant.model.visitedlocaions

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visited_locations")
data class VisitedLocations(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val date: Long
)

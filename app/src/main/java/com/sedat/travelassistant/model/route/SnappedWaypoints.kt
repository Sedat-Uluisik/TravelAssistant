package com.sedat.travelassistant.model.route


import com.google.gson.annotations.SerializedName

data class SnappedWaypoints(
    val coordinates: List<List<Double>>,
    val type: String
)
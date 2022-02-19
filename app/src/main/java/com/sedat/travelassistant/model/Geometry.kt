package com.sedat.travelassistant.model


import com.google.gson.annotations.SerializedName

data class Geometry(
    val coordinates: List<Double>,
    val type: String
)
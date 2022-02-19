package com.sedat.travelassistant.model


import com.google.gson.annotations.SerializedName

data class Place(
    val features: List<Feature>,
    val type: String
)
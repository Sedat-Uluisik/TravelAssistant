package com.sedat.travelassistant.model.route


import com.google.gson.annotations.SerializedName

data class Route(
    val hints: Hints,
    val info: Info,
    val paths: List<Path>
)
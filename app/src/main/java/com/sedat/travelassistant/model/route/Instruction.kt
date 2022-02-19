package com.sedat.travelassistant.model.route


import com.google.gson.annotations.SerializedName

data class Instruction(
    val distance: Double,
    val heading: Double,
    val interval: List<Int>,
    @SerializedName("last_heading")
    val lastHeading: Double,
    val sign: Int,
    @SerializedName("street_name")
    val streetName: String,
    val text: String,
    val time: Int
)
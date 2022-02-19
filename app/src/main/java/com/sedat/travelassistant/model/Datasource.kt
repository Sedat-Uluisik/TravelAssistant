package com.sedat.travelassistant.model


import com.google.gson.annotations.SerializedName

data class Datasource(
    val attribution: String,
    val license: String,
    val sourcename: String,
    val url: String
)
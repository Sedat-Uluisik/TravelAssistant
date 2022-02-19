package com.sedat.travelassistant.model.image


import com.google.gson.annotations.SerializedName

data class QueryContext(
    val alterationDisplayQuery: String,
    val alterationMethod: String,
    val alterationOverrideQuery: String,
    val alterationType: String,
    val originalQuery: String
)
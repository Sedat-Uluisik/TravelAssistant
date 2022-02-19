package com.sedat.travelassistant.model.image


import com.google.gson.annotations.SerializedName

data class PlaceImage(
    val currentOffset: Int,
    val instrumentation: Instrumentation,
    val nextOffset: Int,
    val queryContext: QueryContext,
    val readLink: String,
    val totalEstimatedMatches: Int,
    @SerializedName("_type")
    val type: String,
    val value: List<Value>,
    val webSearchUrl: String
)
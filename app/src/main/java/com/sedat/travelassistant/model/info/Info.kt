package com.sedat.travelassistant.model.info


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Info(
    val batchcomplete: String,
    @SerializedName("query")
    val query: Query
)
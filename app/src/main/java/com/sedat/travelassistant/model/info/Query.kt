package com.sedat.travelassistant.model.info


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Query(
    @SerializedName("pages")
    val pages: Map<String, Detail>
)
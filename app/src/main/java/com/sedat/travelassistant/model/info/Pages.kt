package com.sedat.travelassistant.model.info


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Pages(
    @SerializedName("pages")
    val detail: Map<String, Detail>
)
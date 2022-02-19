package com.sedat.travelassistant.model.info


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Detail(
    @SerializedName("extract")
    val detailText: String,
    val ns: Int,
    val pageid: Int,
    val title: String
)
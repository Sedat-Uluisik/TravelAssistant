package com.sedat.travelassistant.model.route


import com.google.gson.annotations.SerializedName

data class Hints(
    @SerializedName("visited_nodes.average")
    val visitedNodesAverage: Double,
    @SerializedName("visited_nodes.sum")
    val visitedNodesSum: Int
)
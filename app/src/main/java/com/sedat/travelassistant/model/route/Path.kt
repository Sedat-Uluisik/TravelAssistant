package com.sedat.travelassistant.model.route


import com.google.gson.annotations.SerializedName

data class Path(
    val ascend: Double,
    val bbox: List<Double>,
    val descend: Double,
    val description: List<String>,
    val details: Details,
    val distance: Double,
    val instructions: List<Instruction>,
    val legs: List<Any>,
    val points: Points,
    @SerializedName("points_encoded")
    val pointsEncoded: Boolean,
    @SerializedName("snapped_waypoints")
    val snappedWaypoints: SnappedWaypoints,
    val time: Int,
    val transfers: Int,
    val weight: Double
)
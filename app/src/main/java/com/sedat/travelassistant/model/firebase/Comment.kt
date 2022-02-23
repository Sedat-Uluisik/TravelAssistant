package com.sedat.travelassistant.model.firebase

data class Comment(
    val userName: String,
    val Comment: String,
    val date: Long,
    val likeNumber: Int,
    val dislikeNumber: Int,
    val rating: Float
)

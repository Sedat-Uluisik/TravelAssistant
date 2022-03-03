package com.sedat.travelassistant.model.firebase

data class Comment(
    //firebase de kullanılacak sınıfın değerleri başlangıçta varsayılan değerleri almalı
    //almaz ise hata verir.
    var Comment: String = "",
    var commentId: String = "",
    var date: Long = 0,
    var dislikeNumber: Int = 0,
    var likeNumber: Int = 0,
    var rating: Float = 0.0f,
    var userName: String = "",
    var userId: String = ""
)

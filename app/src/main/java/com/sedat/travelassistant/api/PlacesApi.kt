package com.sedat.travelassistant.api

import com.sedat.travelassistant.model.Place
import com.sedat.travelassistant.model.image.PlaceImage
import com.sedat.travelassistant.model.info.Info
import com.sedat.travelassistant.model.route.Route
import com.sedat.travelassistant.util.API_KEY
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.*
import javax.inject.Inject

interface PlacesApi {

    //https://api.geoapify.com/v2/places?categories=building.historic&lang=tr&filter=circle:28.966028,41.012983,1500&limit=20&apiKey=8ed01562dc5f4cbb892adda7cbe205ef
    @GET("/v2/places?lang=tr")
    fun getPlace(
            @Query("categories") category: String,
            @Query("filter") latLong: String,
            @Query("limit") limit: Int,
            @Query("apiKey") apiKey: String = API_KEY
    ): Single<Place>

    //https://bing-image-search1.p.rapidapi.com/images/search?q=yerebatan+sarnıcı&count=10
    @GET("https://bing-image-search1.p.rapidapi.com/images/search?count=15")
    fun getImage(
            @Query("q") query: String,
            @Header("x-rapidapi-host") header: String = "bing-image-search1.p.rapidapi.com",
            @Header("x-rapidapi-key") key: String = "8147d444camshdb16eb5dd0020acp1f81adjsn681774089dc4"
    ): Single<PlaceImage>

    //https://tr.wikipedia.org/w/api.php?action=query&format=json&prop=extracts&exintro=&explaintext=&titles=Galata%20Kulesi&utf8=1
    @GET
    fun getInfo(
            @Url url: String,
            @Query("titles") q: String
    ): Single<Info>

    @GET("route")
    fun getRoute(
            @Query("point") routes: List<String>,
            @Query("profile") profile: String,
            //@Query("algorithm") bb: String = "alternative_route",
            //@Query("alternative_route.max_paths") cc: String = "2",
            @Query("locale") dd: String = "tr",
            @Query("points_encoded") ee: String = "false",
            @Query("key") ff: String = "8ad1c5dd-ecf7-4fcb-86a1-b9bb96627dfa"
    ): Single<Route>

}
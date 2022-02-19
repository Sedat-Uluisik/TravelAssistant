package com.sedat.travelassistant.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sedat.travelassistant.R
import com.sedat.travelassistant.api.PlacesApi
import com.sedat.travelassistant.repo.PlaceRepository
import com.sedat.travelassistant.repo.PlaceRepositoryInterface
import com.sedat.travelassistant.util.BASE_URL
import com.sedat.travelassistant.util.BASE_URL_FOR_ROUTE
import com.sedat.travelassistant.util.SaveImageToFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @ForPlaces
    @Singleton
    @Provides
    fun injectRetrofit(): PlacesApi{
        return Retrofit
                .Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(PlacesApi::class.java)
    }

    @ForRoute
    @Singleton
    @Provides
    fun injectRetrofitForRoute(): PlacesApi{
        return Retrofit
                .Builder()
                .baseUrl(BASE_URL_FOR_ROUTE)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(PlacesApi::class.java)
    }

    @Singleton
    @Provides
    fun injectRepo(@ForPlaces placesApi: PlacesApi, @ForRoute placesApiForRoute: PlacesApi, @ApplicationContext context: Context) = PlaceRepository(placesApi, placesApiForRoute, context) as PlaceRepositoryInterface

    @Singleton
    @Provides
    fun injectGlide(@ApplicationContext context: Context) =
        Glide.with(context)
            .setDefaultRequestOptions(
                RequestOptions().placeholder(R.drawable.category_24).error(R.drawable.error_32)
            )

    @Singleton
    @Provides
    fun injectSharedPref(@ApplicationContext context: Context) = context.getSharedPreferences("com.sedat.travelassistant", Context.MODE_PRIVATE)
}

//iki farklı base_url olduğu için kullanım yerine göre uygun retrofiti inject etmek için kullanıldı.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForPlaces

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForRoute

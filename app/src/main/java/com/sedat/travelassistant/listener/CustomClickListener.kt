package com.sedat.travelassistant.listener

import android.view.View
import com.sedat.travelassistant.model.Feature
import com.sedat.travelassistant.model.Properties

interface CustomClickListener {
    fun onCloseButtonClick()

    fun onDetailsButtonClick(place: Properties)

    fun onAddToRouteButtonClick(lat: Double, lon: Double, name: String?)

    fun onFavoriteButtonClick(properties: Properties)
}
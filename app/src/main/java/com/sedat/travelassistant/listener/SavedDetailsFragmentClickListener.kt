package com.sedat.travelassistant.listener

import com.sedat.travelassistant.model.room.SavedPlace

interface SavedDetailsFragmentClickListener {
    fun updateButtonClick(savedPlace: SavedPlace)

    fun fabCreateRouteButtonClick(savedPlace: SavedPlace)
}
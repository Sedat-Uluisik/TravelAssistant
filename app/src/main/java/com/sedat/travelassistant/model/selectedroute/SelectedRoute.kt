package com.sedat.travelassistant.model.selectedroute

import android.location.Location
import java.io.Serializable

data class SelectedRoute(
        val name: String,
        val location: Location
): Serializable

package com.sedat.travelassistant.model


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Properties(
    @SerializedName("address_line1")
    val addressLine1: String,
    @SerializedName("address_line2")
    val addressLine2: String,
    val categories: List<String>,
    val city: String,
    val country: String,
    @SerializedName("country_code")
    val countryCode: String,
    val county: String,
    val datasource: Datasource,
    val details: List<String>,
    val district: String,
    val formatted: String,
    val housenumber: String,
    val lat: Double,
    val lon: Double,
    val name: String,
    val neighbourhood: String,
    @SerializedName("place_id")
    val placeId: String,
    val postcode: String,
    val state: String,
    val street: String,
    val suburb: String,
    val town: String,
): Serializable
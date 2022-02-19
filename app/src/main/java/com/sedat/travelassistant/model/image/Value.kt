package com.sedat.travelassistant.model.image


import com.google.gson.annotations.SerializedName

data class Value(
    val accentColor: String,
    val contentSize: String,
    val contentUrl: String,
    val datePublished: String,
    val encodingFormat: String,
    val height: Int,
    val hostPageDisplayUrl: String,
    val hostPageDomainFriendlyName: String,
    val hostPageFavIconUrl: String,
    val hostPageUrl: String,
    val imageId: String,
    val imageInsightsToken: String,
    val insightsMetadata: InsightsMetadata,
    val isFamilyFriendly: Boolean,
    val name: String,
    val thumbnail: Thumbnail,
    val thumbnailUrl: String,
    val webSearchUrl: String,
    val width: Int
)
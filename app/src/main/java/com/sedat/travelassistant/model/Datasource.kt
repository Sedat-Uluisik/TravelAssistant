package com.sedat.travelassistant.model

import java.io.Serializable

data class Datasource(
    val attribution: String,
    val license: String,
    val sourcename: String,
    val url: String
): Serializable
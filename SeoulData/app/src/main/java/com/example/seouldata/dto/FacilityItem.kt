package com.example.seouldata.dto

import com.google.gson.annotations.SerializedName

data class FacilityItem(
    @SerializedName("SVCNM") val name: String,
    @SerializedName("PLACENM") val location: String,
    @SerializedName("X") val longitude: String,
    @SerializedName("Y") val latitude: String
)
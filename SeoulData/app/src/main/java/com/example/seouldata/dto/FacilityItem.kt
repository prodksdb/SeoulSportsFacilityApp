package com.example.seouldata.dto

import com.google.gson.annotations.SerializedName

data class FacilityItem(
    @SerializedName("x") val longitude: String,                     // 장소 x 좌표
    @SerializedName("y") val latitude: String,                      // 장소 y 좌표
    @SerializedName("placenm") val location: String,                // 장소명
    @SerializedName("minclassnm") val minClassNm: String = "",      // 소분류명(종목명)

)
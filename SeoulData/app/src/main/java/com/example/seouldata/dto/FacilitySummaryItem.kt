package com.example.seouldata.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FacilitySummaryItem(
    @SerializedName("areanm") val areaName: String = "",            // 지역명
    @SerializedName("dtlcont") val detailContent: String = "",      // 상세정보
    @SerializedName("gubun") val gubun: String = "",                 // 서비스구분
    @SerializedName("imgurl") val imgUrl: String? = null,            // 이미지경로
    @SerializedName("maxclassnm") val maxClassNm: String = "",       // 대분류명
    @SerializedName("minclassnm") val minClassNm: String = "",       // 소분류명
    @SerializedName("payatnm") val payType: String = "",             // 결제방법
    @SerializedName("placenm") val placeName: String = "",           // 장소명
    @SerializedName("rcptbgndt") val receiptBegin: Long = 0L,        // 접수시작일시 (Long)
    @SerializedName("rcptenddt") val receiptEnd: Long = 0L,          // 접수종료일시 (Long)
    @SerializedName("revstdday") val cancelRuleValue: Int = 0,       // 취소기간 기준일까지 (Int)
    @SerializedName("revstddaynm") val cancelRuleName: String = "",  // 취소기간 기준정보
    @SerializedName("svcid") val svcId: String = "",                 // 서비스ID
    @SerializedName("svcnm") val svcName: String = "",               // 서비스명
    @SerializedName("svcopnbgndt") val svcOpenBegin: Long = 0L,       // 서비스개시시작일시 (Long)
    @SerializedName("svcopnenddt") val svcOpenEnd: Long = 0L,         // 서비스개시종료일시 (Long)
    @SerializedName("svcstatnm") val svcStatus: String = "",         // 서비스상태
    @SerializedName("svcurl") val svcUrl: String = "",               // 바로가기URL
    @SerializedName("usetgtinfo") val useTargetInfo: String = "",    // 서비스대상
    @SerializedName("v_max") val vMax: String = "",                  // 서비스이용 종료시간
    @SerializedName("v_min") val vMin: String = "",                  // 서비스이용 시작시간
    @SerializedName("x") val x: String = "",                         // 장소X좌표
    @SerializedName("y") val y: String = ""                          // 장소Y좌표
) : Parcelable
 {

    override fun describeContents(): Int {
        return 0
    }
}


data class FacilityApiResponse(
    @SerializedName("list_total_count") val totalCount: Int,
    @SerializedName("RESULT") val result: ResultInfo,
    @SerializedName("row") val facilities: List<FacilityItem>
)

data class ResultInfo(
    @SerializedName("CODE") val code: String,
    @SerializedName("MESSAGE") val message: String
)


//    val svcId: String,               // 서비스 ID
//    val svcName: String,            // 서비스명
//    val areaName: String,           // 지역명
//    val svcStartDate: String,       // 서비스 개시 시작일시
//    val svcEndDate: String,         // 서비스 개시 종료일시
//    val receiptStartDate: String,   // 접수 시작일시
//    val receiptEndDate: String,     // 접수 종료일시
//    val xCoord: String,             // 장소 X좌표
//    val yCoord: String,             // 장소 Y좌표
//    val targetInfo: String,         // 서비스 대상
//    val svcStatus: String,          // 서비스 상태
//    val payType: String,            // 결제방법
//    val imageUrl: String?           // 이미지 URL
package com.example.seouldata.dto

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FacilitySummaryItem(
    @SerializedName("GUBUN") val gubun: String,
    @SerializedName("SVCID") val svcId: String,
    @SerializedName("MAXCLASSNM") val maxClassNm: String,
    @SerializedName("MINCLASSNM") val minClassNm: String,
    @SerializedName("SVCSTATNM") val svcStatus: String,
    @SerializedName("SVCNM") val svcName: String,
    @SerializedName("PAYATNM") val payType: String,
    @SerializedName("PLACENM") val placeName: String,
    @SerializedName("USETGTINFO") val useTargetInfo: String,
    @SerializedName("SVCURL") val svcUrl: String,
    @SerializedName("X") val x: String,
    @SerializedName("Y") val y: String,
    @SerializedName("SVCOPNBGNDT") val svcOpenBegin: String,
    @SerializedName("SVCOPNENDDT") val svcOpenEnd: String,
    @SerializedName("RCPTBGNDT") val receiptBegin: String,
    @SerializedName("RCPTENDDT") val receiptEnd: String,
    @SerializedName("AREANM") val areaName: String,
    @SerializedName("IMGURL") val imgUrl: String?,
    @SerializedName("DTLCONT") val detailContent: String,
    @SerializedName("TELNO") val telNo: String?,
    @SerializedName("V_MIN") val vMin: String?,
    @SerializedName("V_MAX") val vMax: String?,
    @SerializedName("REVSTDDAYNM") val cancelRuleName: String?,
    @SerializedName("REVSTDDAY") val cancelRuleValue: String?

) : Parcelable{

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
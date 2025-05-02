package com.seouldata.sport.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room이 SQLite에 저장할 때 사용하는 로컬 DB 테이블 모델
@Entity(tableName = "facility")
data class FacilityEntity(
    @PrimaryKey val svcId: String,  // 서비스 ID를 고유키로 사용 (중복 방지)

    val areaName: String,
    val detailContent: String,
    val gubun: String,
    val imgUrl: String?, // 이미지 없을 수도 있으니 nullable 유지

    val maxClassNm: String,
    val minClassNm: String,
    val payType: String,
    val placeName: String,

    val receiptBegin: Long,
    val receiptEnd: Long,

    val cancelRuleValue: Int,
    val cancelRuleName: String,

    val svcName: String,
    val svcOpenBegin: Long,
    val svcOpenEnd: Long,
    val svcStatus: String,
    val svcUrl: String,

    val telNo: String,
    val useTargetInfo: String,

    val vMax: String,
    val vMin: String,
    val x: String,
    val y: String
)

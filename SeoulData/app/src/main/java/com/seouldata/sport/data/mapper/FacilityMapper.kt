package com.seouldata.sport.data.mapper

// 위치 예시: data/mapper/FacilityMapper.kt

import com.seouldata.sport.dto.FacilitySummaryItem
import com.seouldata.sport.data.entity.FacilityEntity

// DTO -> Entity
fun FacilitySummaryItem.toEntity(): FacilityEntity {
    return FacilityEntity(
        svcId = this.svcId,
        areaName = this.areaName,
        detailContent = this.detailContent,
        gubun = this.gubun,
        imgUrl = this.imgUrl,
        maxClassNm = this.maxClassNm,
        minClassNm = this.minClassNm,
        payType = this.payType,
        placeName = this.placeName,
        receiptBegin = this.receiptBegin,
        receiptEnd = this.receiptEnd,
        cancelRuleValue = this.cancelRuleValue,
        cancelRuleName = this.cancelRuleName,
        svcName = this.svcName,
        svcOpenBegin = this.svcOpenBegin,
        svcOpenEnd = this.svcOpenEnd,
        svcStatus = this.svcStatus,
        svcUrl = this.svcUrl,
        telNo = this.telNo,
        useTargetInfo = this.useTargetInfo,
        vMax = this.vMax,
        vMin = this.vMin,
        x = this.x,
        y = this.y
    )
}

// Entity -> Dto
fun FacilityEntity.toDto(): FacilitySummaryItem {
    return FacilitySummaryItem(
        areaName = this.areaName,
        detailContent = this.detailContent,
        gubun = this.gubun,
        imgUrl = this.imgUrl,
        maxClassNm = this.maxClassNm,
        minClassNm = this.minClassNm,
        payType = this.payType,
        placeName = this.placeName,
        receiptBegin = this.receiptBegin,
        receiptEnd = this.receiptEnd,
        cancelRuleValue = this.cancelRuleValue,
        cancelRuleName = this.cancelRuleName,
        svcId = this.svcId,
        svcName = this.svcName,
        svcOpenBegin = this.svcOpenBegin,
        svcOpenEnd = this.svcOpenEnd,
        svcStatus = this.svcStatus,
        svcUrl = this.svcUrl,
        telNo = this.telNo,
        useTargetInfo = this.useTargetInfo,
        vMax = this.vMax,
        vMin = this.vMin,
        x = this.x,
        y = this.y
    )
}



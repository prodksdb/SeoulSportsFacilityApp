package com.example.seouldata.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// 권한 요청을 보내는 기능까지만
// object 키워드 사용하여 싱글톤 객체로 만들어서 인스턴스 생성 없이 바로 사용 가능하다.
object RequestPermissionUtil {
    const val LOCATION_PERMISSION_REQUEST_CODE = 1000

    // 현재 위치 권한이 있는지 검사하는 함수
    fun hasLocationPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED  // 허용된 상태인지
    }

    // 위치 권한을 요청하는 함수
    fun requestLocationPermission(activity: Activity){
        ActivityCompat.requestPermissions(  // 권한 요청 창 띄우기
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // 거절했을 때 다시 요청하거나 앱 설정으로 보내는 기능
}
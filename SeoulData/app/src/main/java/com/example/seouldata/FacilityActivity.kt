package com.example.seouldata

import android.graphics.Color
import android.graphics.Typeface
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.seouldata.databinding.ActivityFacilityBinding
import com.example.seouldata.dto.FacilitySummaryItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import okio.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TAG = "FacilityActivity"
class FacilityActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityFacilityBinding
    private var facilityItem: FacilitySummaryItem? = null
    private lateinit var map: GoogleMap

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFacilityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. facilityItem 데이터 받기
        facilityItem = intent.getParcelableExtra("facilityItem")
        facilityItem?.let { item ->
            // 2. 데이터 바인딩(이미지, 도로명 주소, 지역명, 종목명, 이용시간, 전화번호, 상세내용 등)
            bindFacilityInfo(item)
            // 3. Geocoder를 이용해 도로명 주소 얻기
            val latitude = item.y.toDoubleOrNull() ?: 0.0  // x, y가 비어있으면 기본값 0.0
            val longitude = item.x.toDoubleOrNull() ?: 0.0
            getRoadAddress(latitude, longitude)
        } ?: run {
            // facilityItem이 null일 경우 처리
            Toast.makeText(this, "Facility data is missing", Toast.LENGTH_SHORT).show()
        }

        // 날짜 선택 설정
        setupDateChips()

        // 예약 버튼 클릭 처리
        binding.btnReserve.setOnClickListener {
            Toast.makeText(this, "예약되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 뒤로 가기
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 맵 준비
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.facilityMapContainer, mapFragment)
            .commit()

        mapFragment.getMapAsync(this)

        // 탭 선택에 따라 내용 전환
        binding.facilityTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // 모든 탭 내용 숨기기
                binding.layoutTabDetail.visibility = View.GONE
                binding.layoutTabUsage.visibility = View.GONE
                binding.layoutTabReview.visibility = View.GONE

                // 선택된 탭에 따라 보이기
                when (tab.position) {
                    0 -> binding.layoutTabDetail.visibility = View.VISIBLE
                    1 -> binding.layoutTabUsage.visibility = View.VISIBLE
                    2 -> binding.layoutTabReview.visibility = View.VISIBLE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 날짜 선택-> 시간-> 예약 활성화
        // 초기 상태: 시간 선택 바, 예약 영역 숨기기
        binding.timelineContainer.visibility = View.GONE
        binding.timeSlotContainer.visibility = View.GONE
        binding.timeSelectionSection.visibility = View.GONE
        binding.btnReserve.isEnabled = false

        // 날짜 선택 이벤트
        binding.dateChipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != View.NO_ID) {
                // 날짜 선택 → 시간 선택 바 표시
                binding.timelineContainer.visibility = View.VISIBLE
                binding.timeSlotContainer.visibility = View.VISIBLE

                // 이전에 선택했던 시간 초기화
                binding.timeSelectionSection.visibility = View.GONE
                binding.btnReserve.isEnabled = false
            } else {
                // 날짜 해제 → 모두 숨김
                binding.timelineContainer.visibility = View.GONE
                binding.timeSlotContainer.visibility = View.GONE
                binding.timeSelectionSection.visibility = View.GONE
                binding.btnReserve.isEnabled = false
            }
        }

        // 시간 슬롯 클릭 처리 예시 (직접 만든 시간 슬롯 TextView들에 각각 연결 필요)
        for (i in 0 until binding.timeSlotContainer.childCount) {
            val view = binding.timeSlotContainer.getChildAt(i)
            if (view is TextView && view.text.contains("~")) {
                view.setOnClickListener {
                    // 선택 시 스타일 변경
                    resetAllTimeSlotStyles()
                    view.setBackgroundResource(R.drawable.bg_time_slot_selected)
                    view.setTextColor(Color.WHITE)
                    view.setTypeface(null, Typeface.BOLD)

                    // 시간/금액 정보 설정 (예시)
                    binding.timeSummary.text = "총 1 시간"
                    binding.priceSummary.text = "10,000 원"

                    // 예약 영역 표시
                    binding.timeSelectionSection.visibility = View.VISIBLE
                    binding.btnReserve.isEnabled = true
                }
            }
        }
    }

    // 날짜 선택을 위한 ChipGroup 설정(오늘 날짜 기준 7일)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDateChips() {
        val today = LocalDate.now()  // 현재 날짜
        val formatter = DateTimeFormatter.ofPattern("MM월 dd일")  // 날짜 포맷 설정

        // ChipGroup 초기화
        binding.dateChipGroup.removeAllViews()

        // 7일 동안의 날짜를 계산해서 Chip으로 추가
        for (i in 0 until 7) {
            val date = today.plusDays(i.toLong())  // 오늘 날짜부터 7일 후까지 날짜 생성
            val chip = Chip(this)
            chip.text = date.format(formatter)  // 날짜 포맷
            chip.isCheckable = true

            // 텍스트를 Bold(진하게)로 설정
            chip.setTypeface(null, Typeface.BOLD)

            // Chip을 ChipGroup에 추가
            binding.dateChipGroup.addView(chip)

            resetAllDateChipsStyles()  // 모든 날짜의 색상을 초기화

            // 날짜 클릭 리스너
            chip.setOnClickListener {

                resetAllDateChipsStyles()  // 모든 날짜의 색상을 초기화

                if (chip.isChecked) {
                    // 날짜가 선택되었을 때
                    chip.setChipBackgroundColorResource(R.color.red_primary)  // 배경색 변경
                    chip.setTextColor(ContextCompat.getColor(this, R.color.black_primary))  // 텍스트 색상 변경
                } else {
                    // 날짜 선택 취소 시
                    chip.setChipBackgroundColorResource(R.color.black_primary)  // 기본 배경색으로 리셋
                    chip.setTextColor(ContextCompat.getColor(this, R.color.white))  // 기본 텍스트 색상으로 리셋
                }
                showTimeSelection(date)  // 날짜 선택 시 시간 선택 UI 활성화
            }
        }
    }

    // 모든 날짜의 색상을 초기화하는 함수
    private fun resetAllDateChipsStyles() {
        for (i in 0 until binding.dateChipGroup.childCount) {
            val chip = binding.dateChipGroup.getChildAt(i) as Chip
            chip.setTextColor(ContextCompat.getColor(this,R.color.white))
            chip.setChipBackgroundColorResource(R.color.black_primary)
        }
    }

    // 날짜 클릭 시 해당 날짜에 맞는 시간 선택 바 표시
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTimeSelection(date: LocalDate?) {
        // 시간 선택 UI 표시
        binding.timelineContainer.visibility = View.VISIBLE
        binding.timeSlotContainer.visibility = View.VISIBLE

        // 선택된 날짜에 맞는 시간 정보 설정 (예시로 9 AM ~ 5 PM)
        binding.timeSummary.text = "${date?.monthValue ?: 0}/${date?.dayOfMonth ?: 0} 9 AM ~ 5 PM"

        // 예약 버튼 활성화
        binding.timeSelectionSection.visibility = View.VISIBLE
        binding.btnReserve.isEnabled = true
    }

    // 시간 슬롯 스타일 처리
    private fun resetAllTimeSlotStyles() {
        for (i in 0 until binding.timeSlotContainer.childCount) {
            val view = binding.timeSlotContainer.getChildAt(i)
            if (view is TextView && view.text.contains("~")) {
                view.setBackgroundResource(R.drawable.bg_time_slot) // 기본 배경
                view.setTextColor(Color.BLACK)
                view.setTypeface(null, Typeface.NORMAL)
            }
        }
    }


    // 시설 정보 바인딩
    private fun bindFacilityInfo(facilityItem: FacilitySummaryItem){
        // 이미지 URL 바인딩
        Glide.with(this)
            .load(facilityItem.imgUrl) // 이미지 URL
            .placeholder(R.drawable.img_loading) // 로딩 중 표시할 기본 이미지
            .error(R.drawable.img_loading) // 실패 시 표시할 기본 이미지
            .into(binding.facilityImage)

        // 시설명 (시설 이름 바인딩)
        binding.txtFacilityName.text = facilityItem.svcName

        // 지역명
        binding.tvRegion.text = facilityItem.areaName

        // 종목명
        binding.tvType.text = facilityItem.minClassNm

        // 이용시간
        val vMin = facilityItem.vMin
        val vMax = facilityItem.vMax
        binding.tvTime.text = "$vMin ~ $vMax"

        // 전화번호
        binding.tvTelephone.text = facilityItem.telNo ?: "전화번호 없음"

        // 상세정보(주요 내용)
        val rawText = facilityItem.detailContent ?: ""
        val formattedText = rawText
            .replace("2.", "\n\n2.")
            .replace("3.", "\n\n3.")
            .replace("◎", "\n\n◎")
            .replace("※", "\n※")
        binding.tvDetailInfo.text = formattedText
    }

    // 위도, 경도를 도로명 주소로 변환하는 함수
    private fun getRoadAddress(latitude: Double, longitude: Double) {
        Log.d(TAG, "latitude: $latitude, longitude: $longitude")
        val geocoder = Geocoder(this, Locale.getDefault())

        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0].getAddressLine(0)  // 도로명 주소
                binding.placeLocation.text = address  // placeLocation에 도로명 주소 바인딩
            } else {
                binding.placeLocation.text = "주소를 찾을 수 없습니다."
            }
        } catch (e: IOException) {
            e.printStackTrace()
            binding.placeLocation.text = "주소를 가져오는 데 오류가 발생했습니다."
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // 시설 위치 정보가 있을 경우 마커 추가 및 카메라 이동
        facilityItem?.let { item ->
            // x와 y 값이 null이거나 빈 문자열("")일 경우 기본값 설정
            val latitude = if (item.y.isNotBlank()) item.y.toDouble() else 0.0  // 빈 문자열일 경우 0.0을 기본값으로 설정
            val longitude = if (item.x.isNotBlank()) item.x.toDouble() else 0.0  // 빈 문자열일 경우 0.0을 기본값으로 설정

            val facilityLocation = LatLng(latitude, longitude)

            // 마커 추가
            map.addMarker(
                MarkerOptions()
                    .position(facilityLocation)
                    .title(item.placeName)
            )

            // 카메라 위치 이동
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(facilityLocation, 15f))
        } ?: run {
            // facilityItem이 null일 경우 처리
            Toast.makeText(this, "Facility location data is missing", Toast.LENGTH_SHORT).show()
        }
    }
}

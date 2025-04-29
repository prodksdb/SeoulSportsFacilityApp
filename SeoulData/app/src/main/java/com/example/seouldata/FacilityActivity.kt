package com.example.seouldata

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.example.seouldata.databinding.ActivityFacilityBinding
import com.example.seouldata.dto.FacilitySummaryItem
import com.example.seouldata.util.TouchWrapper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.tabs.TabLayout

class FacilityActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityFacilityBinding
    private var facilityItem: FacilitySummaryItem? = null
    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFacilityBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //상단 뒤로가기
        val backBtn = findViewById<ImageButton>(R.id.btnBack)
        backBtn.setOnClickListener {
            finish()
        }

        val scrollView = findViewById<NestedScrollView>(R.id.scrollView)
        val touchWrapper = findViewById<TouchWrapper>(R.id.mapTouchWrapper)

        touchWrapper.onTouch = {
            scrollView.requestDisallowInterceptTouchEvent(true)
        }

        // 맵 준비
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.facilityMapContainer, mapFragment)
            .commit()

        mapFragment.getMapAsync(this)

        //intent 받기
       facilityItem  = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("facilityItem", FacilitySummaryItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("facilityItem")
        }

        // 2. View에 바인딩하기
        facilityItem?.let {
            binding.txtFacilityName.text = it.svcName
            binding.placeLocation.text=it.placeName

            val rawText = facilityItem?.detailContent ?: ""
            val formattedText = rawText
                //.replace("1.", "\n\n1.")   // 숫자-점 패턴
                .replace("2.", "\n\n2.")
                .replace("3.", "\n\n3.")
                .replace("◎", "\n\n◎")    // 항목 구분자
                .replace("※", "\n※")     // 주의사항 강조
            binding.tvDetailInfo.text=formattedText

            // 🔥 이미지 URL을 ImageView에 Glide로 세팅
            Glide.with(this)
                .load(it.imgUrl) // URL
                .placeholder(R.drawable.img_loading) // 로딩 중 표시할 기본 이미지 (선택)
                .error(R.drawable.img_loading) // 실패 시 표시할 기본 이미지 (선택)
                .into(binding.facilityImage)

            // 필요한 만큼 추가로 바인딩 가능!
        }




        //탭 선택에 따라 내용 전환
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

        //날짜 선택-> 시간-> 예약 활성화
        // 초기 상태: 시간 선택 바, 예약 영역 숨기기
        binding.timelineContainer.visibility = View.GONE
        binding.timeSlotContainer.visibility = View.GONE
        binding.timeSelectionSection.visibility = View.GONE
        binding.btnReserve.isEnabled = false

        var timeSelected = false // 시간 선택 여부 상태

        // 날짜 선택 이벤트
        binding.dateChipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != View.NO_ID) {
                // 날짜 선택 → 시간 선택 바 표시
                binding.timelineContainer.visibility = View.VISIBLE
                binding.timeSlotContainer.visibility = View.VISIBLE

                // 이전에 선택했던 시간 초기화
                timeSelected = false
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
                    resetAllTimeSlotStyles(binding)
                    view.setBackgroundResource(R.drawable.bg_time_slot_selected)
                    view.setTextColor(Color.WHITE)
                    view.setTypeface(null, Typeface.BOLD)

                    // 시간/금액 정보 설정 (예시)
                    binding.timeSummary.text = "총 1 시간"
                    binding.priceSummary.text = "10,000 원"

                    // 예약 영역 표시
                    binding.timeSelectionSection.visibility = View.VISIBLE
                    binding.btnReserve.isEnabled = true
                    timeSelected = true
                }
            }
        }

        //예약 버튼 클릭시
        binding.btnReserve.setOnClickListener {
            Toast.makeText(this, "예약되었습니다", Toast.LENGTH_SHORT).show()
        }

    }

    //시간 선택 슬롯 강조용 함수 추가
    private fun resetAllTimeSlotStyles(binding: ActivityFacilityBinding) {
        for (i in 0 until binding.timeSlotContainer.childCount) {
            val view = binding.timeSlotContainer.getChildAt(i)
            if (view is TextView && view.text.contains("~")) {
                view.setBackgroundResource(R.drawable.bg_time_slot) // 기본 배경
                view.setTextColor(Color.BLACK)
                view.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
//
//        facilityItem?.let { item ->
//            val latitude = item.y.toDouble()
//            val longitude = item.x.toDouble()
//            val facilityLocation = LatLng(latitude, longitude)
//
//            map.addMarker(
//                MarkerOptions()
//                    .position(facilityLocation)
//                    .title(item.placeName)
//            )
//            map.moveCamera(CameraUpdateFactory.newLatLngZoom(facilityLocation, 15f))
//    }
}
}
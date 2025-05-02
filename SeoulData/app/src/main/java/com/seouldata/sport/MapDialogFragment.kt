package com.seouldata.sport

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.seouldata.sport.data.db.AppDatabaseProvider
import com.seouldata.sport.databinding.FragmentMapDialogBinding
import com.seouldata.sport.dto.FacilityItem
import com.seouldata.sport.util.getColoredMarkerBitmap
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

private const val TAG = "MapDialogFragment"
class MapDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentMapDialogBinding? = null
    private val binding get() = _binding!!
    private var currentLocationMarker: Marker? = null
    private var isCategoryVisible = false
    private val currentMarkers = mutableListOf<Marker>()
    private val allFacilityList = mutableListOf<FacilityItem>()

    private val categoryColorMap = mapOf(
        "전체" to R.color.gray_primary,         // 무채색
        "축구장" to R.color.green_primary,       // 녹색
        "풋살장" to R.color.light_green,         // 연녹
        "족구장" to R.color.teal_primary,        // 청록
        "야구장" to R.color.orange_primary,      // 주황
        "테니스장" to R.color.purple_primary,    // 보라
        "농구장" to R.color.red_primary,         // 빨강
        "배구장" to R.color.blue_primary,        // 파랑
        "다목적경기장" to R.color.brown_primary, // 갈색
        "체육관" to R.color.indigo_primary,      // 남보라
        "배드민턴장" to R.color.yellow_primary,  // 노랑
        "탁구장" to R.color.cyan_primary,        // 시안
        "교육시설" to R.color.pink_primary,      // 핑크
        "수영장" to R.color.blue_light,          // 하늘색
        "골프장" to R.color.green_dark           // 진초록
    )


    private lateinit var googleMap: GoogleMap


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // 지도를 담을 Fragment 객체(mapFragment) 생성
        val mapFragment = SupportMapFragment.newInstance()
        // @+id/map_container 영역에 mapFragment 집어 넣는 작업
        childFragmentManager.beginTransaction()
            .replace(binding.mapContainer.id, mapFragment)
            .commit()

        // 지도가 준비된 이후에 실제 GoogleMap 인스턴스를 받아서 초기 설정
        mapFragment.getMapAsync { map ->
            googleMap = map
            googleMap.uiSettings.isZoomControlsEnabled = false
            moveToCurrentLocation()

            // RoomDB에서 시설 데이터 불러오기
            lifecycleScope.launch {
                val db = AppDatabaseProvider.getDatabase(requireContext())
                val dao = db.facilityDao()
                val entityList = dao.getAll()  // Room에서 전체 데이터 가져오기

                allFacilityList.clear()
                allFacilityList.addAll(entityList.map { entity ->
                    FacilityItem(
                        latitude = entity.y,
                        longitude = entity.x,
                        location = entity.placeName,
                        minClassNm = entity.minClassNm
                    )
                })

                Log.d(TAG, "총 시설 개수(Room): ${allFacilityList.size}")
                showMarkersByCategory("전체")
            }
        }

        binding.btnCloseMap.setOnClickListener {
            dismiss() // 다이얼로그 닫기 (HomeFragment로 자연스럽게 돌아감)
        }

        binding.btnMyLocation.setOnClickListener {
            Toast.makeText(requireContext(), "현재 내 위치로 이동", Toast.LENGTH_SHORT).show()
            moveToCurrentLocation()
        }

        binding.btnZoomIn.setOnClickListener{
            googleMap.animateCamera(CameraUpdateFactory.zoomIn())
        }

        binding.btnZoomOut.setOnClickListener{
            googleMap.animateCamera(CameraUpdateFactory.zoomOut())
        }

        binding.btnToggleCategories.setOnClickListener {
            val targetView = binding.categoryScroll

            if (isCategoryVisible) {
                // 숨기기: 오른쪽 바깥으로 나감
                targetView.animate()
                    .translationX(targetView.width.toFloat())  // 실제 너비만큼 밀기
                    .setDuration(300)
                    .withEndAction {
                        targetView.visibility = View.GONE
                        isCategoryVisible = false
                    }
                    .start()
            } else {
                // 보이기: 오른쪽에서 들어오기
                targetView.translationX = targetView.width.toFloat()
                targetView.visibility = View.VISIBLE
                targetView.animate()
                    .translationX(0f)
                    .setDuration(300)
                    .withEndAction {
                        isCategoryVisible = true
                    }
                    .start()
            }
        }


        // 종목 누르면 마커로 표시
        val categoryMap = mapOf(
            binding.btnAll to "전체",
            binding.btnSoccer to "축구장",
            binding.btnFutsal to "풋살장",
            binding.btnJokgu to "족구장",
            binding.btnBaseball to "야구장",
            binding.btnTennis to "테니스장",
            binding.btnBasketball to "농구장",
            binding.btnVolleyball to "배구장",
            binding.btnMulti to "다목적경기장",
            binding.btnGym to "체육관",
            binding.btnBadminton to "배드민턴장",
            binding.btnPingpong to "탁구장",
            binding.btnEdu to "교육시설",
            binding.btnSwimming to "수영장",
            binding.btnGolf to "골프장"
        )



        categoryMap.forEach { (button, category) ->
            button.setOnClickListener {
                showMarkersByCategory(category)
            }
        }

    }

    private fun showMarkersByCategory(category: String) {
        currentMarkers.forEach{it.remove()}
        currentMarkers.clear()

        val filtered = if (category == "전체") {
            allFacilityList
        } else {
            allFacilityList.filter { it.minClassNm == category }
        }

        filtered.forEach { item ->
            val lat = item.latitude.toDoubleOrNull()
            val lng = item.longitude.toDoubleOrNull()
            if (lat != null && lng != null) {
                // 1. 종목별 색상 찾기
                val colorRes = categoryColorMap[item.minClassNm] ?: R.color.gray_primary

                // 2. 커스텀 마커 아이콘 생성
                val markerIcon = getColoredMarkerBitmap(
                    context = requireContext(),
                    drawableId = R.drawable.ic_marker_base,
                    color = ContextCompat.getColor(requireContext(), colorRes),
                    sizeDp = 40f
                )

                // 3. 마커 생성
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(lat, lng))
                        .title(item.location)
                        .icon(markerIcon)  // 여기 추가!
                )
                Log.d(TAG, "마커 좌표: lat=$lat, lng=$lng / 종목: ${item.minClassNm}")
                marker?.let { currentMarkers.add(it) }
            }
        }

        // 마커가 있을 경우 카메라 이동
        if (filtered.isNotEmpty()) {
            if (filtered.size == 1) {
                // 마커 1개일 경우: 고정 줌
                val lat = filtered[0].latitude.toDoubleOrNull()
                val lng = filtered[0].longitude.toDoubleOrNull()
                if (lat != null && lng != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
                }
            } else {
                // 여러 개일 경우: 모두 포함해서 확대
                val boundsBuilder = LatLngBounds.Builder()
                filtered.forEach { item ->
                    val lat = item.latitude.toDoubleOrNull()
                    val lng = item.longitude.toDoubleOrNull()
                    if (lat != null && lng != null) {
                        boundsBuilder.include(LatLng(lat, lng))
                    }
                }
                val bounds = boundsBuilder.build()
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.isDraggable = false //  드래그 막기
            behavior.state = BottomSheetBehavior.STATE_EXPANDED // 항상 펼쳐진 상태
        }
    }

    // 현재 위치로 이동 함수
    private fun moveToCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)

                    // 지도 카메라 이동
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                    // 이전 마커 제거
                    currentLocationMarker?.remove()


                    val markerIcon = getColoredMarkerBitmap(
                        context = requireContext(),
                        drawableId = R.drawable.ic_my_location,
                        color = ContextCompat.getColor(requireContext(), R.color.red_primary),
                        sizeDp = 40f
                    )

                    //새로운 마커 찍기
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("내 위치")
                            .icon(markerIcon)
                    )

                    googleMap.isMyLocationEnabled = false
                } else {
                    Toast.makeText(requireContext(), "위치 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

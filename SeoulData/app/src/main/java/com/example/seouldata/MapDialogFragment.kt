package com.example.seouldata

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.seouldata.databinding.FragmentMapDialogBinding
import com.example.seouldata.util.getColoredMarkerBitmap
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MapDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentMapDialogBinding? = null
    private val binding get() = _binding!!
    private var currentLocationMarker: Marker? = null

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

    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet =
            dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
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

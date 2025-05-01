package com.example.seouldata.ui.map

import android.Manifest
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.example.seouldata.databinding.FragmentMapBinding
import com.example.seouldata.util.RequestPermissionUtil
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase

class MapFragment : Fragment() {
    private lateinit var googleMap: GoogleMap
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectedLatLng: LatLng

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(binding.mapContainer.id, mapFragment)
            .commit()

        mapFragment.getMapAsync { map ->
            googleMap = map
            googleMap.uiSettings.isZoomControlsEnabled = true

            getCurrentLocationAndInitMap()

            googleMap.setOnMapClickListener { latLng ->
                selectedLatLng = latLng
                googleMap.clear()
                googleMap.addMarker(MarkerOptions().position(latLng).title("선택한 위치"))
                drawNearbyFacilityMarkers(latLng.latitude, latLng.longitude)
            }

            binding.btnSetLocation.setOnClickListener {
                val result = Bundle().apply {
                    putDouble("selected_lat", selectedLatLng.latitude)
                    putDouble("selected_lng", selectedLatLng.longitude)
                }
                setFragmentResult("map_result", result)
                parentFragmentManager.popBackStack()
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getCurrentLocationAndInitMap() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (RequestPermissionUtil.hasLocationPermission(requireActivity())) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location -> handleLocation(location) }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
        } else {
            RequestPermissionUtil.requestLocationPermission(requireActivity())
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun handleLocation(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)
        selectedLatLng = currentLatLng

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        googleMap.addMarker(
            MarkerOptions().position(currentLatLng).title("내 위치")
        )

        // 지도에 현재 위치 버튼 활성화
        if (RequestPermissionUtil.hasLocationPermission(requireActivity())) {
            googleMap.isMyLocationEnabled = true
        }

        drawNearbyFacilityMarkers(location.latitude, location.longitude)
    }

    private fun drawNearbyFacilityMarkers(lat: Double, lng: Double) {
        val database = FirebaseDatabase.getInstance().getReference("DATA")

        database.get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { child ->
                val facLat = child.child("lat").getValue(String::class.java)?.toDoubleOrNull() ?: return@forEach
                val facLng = child.child("lng").getValue(String::class.java)?.toDoubleOrNull() ?: return@forEach

                val distance = calculateDistance(lat, lng, facLat, facLng)
                if (distance <= 2000) {
                    val title = child.child("placenm").getValue(String::class.java) ?: "시설"
                    val titleWithDistance = "$title (${distance.toInt()}m)"
                    googleMap.addMarker(MarkerOptions().position(LatLng(facLat, facLng)).title(titleWithDistance))
                }
            }
        }
    }

    private fun calculateDistance(lat: Double, lng: Double, facLat: Double, facLng: Double): Double {
        val R = 6371000.0 // 지구 반지름 (미터)
        val dLat = Math.toRadians(facLat - lat)
        val dLng = Math.toRadians(facLng - lng)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(facLat)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

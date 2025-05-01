package com.example.seouldata.ui.home

import com.example.seouldata.ui.decorations.VerticalSpaceItemDecoration
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import com.google.android.gms.location.LocationRequest
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.content.Intent
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.seouldata.FacilityActivity
import com.example.seouldata.MapDialogFragment
import com.example.seouldata.R
import com.example.seouldata.databinding.FragmentHomeBinding
import com.example.seouldata.dto.FacilitySummaryItem
import com.example.seouldata.ui.adapter.FacilityAdapter
import com.example.seouldata.util.RequestPermissionUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

private const val TAG = "HomeFragment"

// Home 화면 UI 담당
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var facilityAdapter: FacilityAdapter
    private val facilityList = mutableListOf<FacilitySummaryItem>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbarHome)

        val textView: TextView = binding.textToolbarTitle
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // 1. FusedLocationClient 준비
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // 2. Adapter 준비
        facilityAdapter = FacilityAdapter(facilityList) { facilitySummaryItem ->
            Toast.makeText(
                requireContext(),
                "${facilitySummaryItem.placeName} 클릭됨",
                Toast.LENGTH_SHORT
            ).show()

            // FacilitySummaryItem을 FacilityActivity로 넘기기 전에 로그로 확인
            Log.d(TAG, "Selected Facility: ${facilitySummaryItem.placeName}, x: ${facilitySummaryItem.x}, y: ${facilitySummaryItem.y}")

            val intent = Intent(context, FacilityActivity::class.java)
            Log.d(TAG, "Sending FacilityItem with lat: ${facilitySummaryItem.y}, lon: ${facilitySummaryItem.x}")
            intent.putExtra("facilityItem", facilitySummaryItem)
            startActivity(intent)
        }

        // 3. RecyclerView 연결
        binding.recyclerFacilities.apply {
            adapter = facilityAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // 4. 위치 권한 체크하고 시설 데이터 가져오기
        checkLocationPermissionAndFetch()

        val spacingInPx = resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)
        binding.recyclerFacilities.addItemDecoration(VerticalSpaceItemDecoration(spacingInPx))

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryDropdown()

        // 현위치 갱신하는 버튼
        binding.btnLocation.setOnClickListener {
            // 위치 권한 있으면 현재 위치 가져오기
            if (RequestPermissionUtil.hasLocationPermission(requireActivity())) {
                getCurrentLocation()
            } else {
                // 위치 권한 요청
                RequestPermissionUtil.requestLocationPermission(requireActivity())
            }
        }

        setFragmentResultListener("map_result") { _, bundle ->
            if(!isAdded || _binding==null) return@setFragmentResultListener
            val lat = bundle.getDouble("selected_lat")
            val lng = bundle.getDouble("selected_lng")
            fetchNearbyFacilities(lat, lng, radiusInMeters = 2000)
        }

        // floating button 눌렀을 때
        binding.fabLocation.setOnClickListener{
            val dialog = MapDialogFragment()
            dialog.show(parentFragmentManager,"MapDialog")
        }
    }


    private fun setupCategoryDropdown() {
        val categoryList = resources.getStringArray(R.array.category).toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList)
        val dropdown = binding.categoryDropdown

        val prevSelection = dropdown.text.toString()
        dropdown.setAdapter(adapter)

        if (categoryList.contains(prevSelection)) {
            dropdown.setText(prevSelection, false)
        } else {
            dropdown.setText("전체", false)
        }

        dropdown.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            Toast.makeText(requireContext(), "$selected 선택!", Toast.LENGTH_SHORT).show()
            filterFacilities()
        }
    }

    private fun filterFacilities() {
        val selected = binding.categoryDropdown.text.toString()
        val filteredList = if (selected == "전체") {
            facilityList
        } else {
            facilityList.filter { it.minClassNm.trim() == selected }
        }

        facilityAdapter.updateItems(filteredList)

        if (filteredList.isEmpty()) {
            binding.recyclerFacilities.visibility = View.GONE
            binding.textEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerFacilities.visibility = View.VISIBLE
            binding.textEmpty.visibility = View.GONE
        }

        binding.progressBar.visibility = View.GONE
    }


    override fun onStart() {
        super.onStart()
        startLocationUpdates()  // 자동 위치 갱신 시작
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates() // 자동 위치 갱신 중지
    }

    // 위치 업데이트를 시작하는 함수
    private fun startLocationUpdates() {
        // 위치 업데이트 요청(10초마다 갱신, 5초마다 빠른 업데이트)
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10초마다 위치 갱신
                .setMinUpdateIntervalMillis(5000) // 가장 빠른 업데이트 간격 (5초)
                .build()

        // 권한 체크 후 위치 업데이트 시작
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper() // 위치 업데이트가 메인 쓰레드에서 실행되도록 설정
            )
        }
    }

    // 위치 업데이트 콜백
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            locationResult.let {
                // locations가 비어있지 않으면 처리
                if (it.locations.isNotEmpty()) {
                    val location = it.locations[0]
                    getAddressFromLocation(location.latitude, location.longitude)
                }
            }
        }
    }

    // 위치 업데이트 중지
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // 위치 권한이 있는지 확인 후 위치 가져오는 함수
    private fun checkLocationPermissionAndFetch() {

        if (RequestPermissionUtil.hasLocationPermission(requireActivity())) {
            getCurrentLocation()
        } else {
            RequestPermissionUtil.requestLocationPermission(requireActivity())
        }
    }

    // 현재 위치를 가져오는 함수
    private fun getCurrentLocation() {
        // 안전을 위해 내부에서도 권한 재확인
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 마지막 위치 가져오기
        fusedLocationClient
            .getLastLocation()
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    getAddressFromLocation(latitude, longitude)
                } else {
                    requestNewLocation()
                }

            }
    }

    // 실시간으로 위치 요청하는 함수
    private fun requestNewLocation() {

        // 권한 체크
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 위치 요청을 중간에 취소할 수 있도록 도와주는 객체
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient
            .getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    getAddressFromLocation(lat, lon)
                } else {
                }
            }
            .addOnFailureListener {
            }
    }


    // 위도, 경도로 주소를 변환하는 함수
    private fun getAddressFromLocation(lat: Double, lon: Double) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            val address = addresses[0]
            val city = address.adminArea ?: ""
            val district = address.subLocality ?: address.subAdminArea ?: ""

            // 주소 텍스트 업데이트!
            binding.textLocation.text = "현재 위치: $city $district"

            // 체육시설 다시 불러오기
            fetchFacilities(city, district)
        }

    }


    // Firebase에서 체육시설 정보를 가져오는 함수
    private fun fetchFacilities(city: String, district: String) {
        binding.progressBar.visibility = View.VISIBLE

        val database = FirebaseDatabase.getInstance()
        val testDataRef = database.getReference("DATA")

        testDataRef.get().addOnSuccessListener { snapshot ->
            if(!isAdded || _binding == null) return@addOnSuccessListener //뷰가 파괴됐을때 바로 리턴
            facilityList.clear()

            snapshot.children.forEach { child ->
                val areaName = child.child("areanm").getValue(String::class.java) ?: ""
                val placeName = child.child("placenm").getValue(String::class.java) ?: ""
                val svcName = child.child("svcnm").getValue(String::class.java) ?: ""
                val svcStatus = child.child("svcstatnm").getValue(String::class.java) ?: ""
                val payType = child.child("payatnm").getValue(String::class.java) ?: ""
                val imgUrl = child.child("imgurl").getValue(String::class.java) ?: ""
                val minClassNm = child.child("minclassnm").getValue(String::class.java) ?: ""
                val telNo = child.child("telno").getValue(String::class.java) ?: ""
                val vMin = child.child("v_min").getValue(String::class.java) ?: ""
                val vMax = child.child("v_max").getValue(String::class.java) ?: ""
                val x = child.child("x").getValue(String::class.java) ?: ""
                val y = child.child("y").getValue(String::class.java) ?: ""
                val dtlcont = child.child("dtlcont").getValue(String::class.java) ?: ""

                // 필터링 : areaName 또는 placeName 안에 district(마포구 등)가 들어가면 추가
                if (areaName.contains(district)) {
                    val facility = FacilitySummaryItem(
                        areaName = areaName,
                        placeName = placeName,
                        svcName = svcName,
                        svcStatus = svcStatus,
                        payType = payType,
                        imgUrl = imgUrl,
                        minClassNm = minClassNm,
                        telNo = telNo,
                        vMin = vMin,
                        vMax = vMax,
                        x = x,
                        y = y,
                        detailContent = dtlcont
                    )
                    facilityList.add(facility)
                }
            }

            facilityAdapter.notifyDataSetChanged()

            setupCategoryDropdown()
            filterFacilities()
        }

    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RequestPermissionUtil.LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchNearbyFacilities(lat: Double, lng: Double, radiusInMeters: Int) {
        binding.progressBar.visibility = View.VISIBLE

        val database = FirebaseDatabase.getInstance().getReference("DATA")
        facilityList.clear()

        database.get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { child ->
                val facLat = child.child("lat").getValue(String::class.java)?.toDoubleOrNull() ?: return@forEach
                val facLng = child.child("lng").getValue(String::class.java)?.toDoubleOrNull() ?: return@forEach

                val distance = calculateDistance(lat, lng, facLat, facLng)
                if (distance <= radiusInMeters) {
                    val areaName = child.child("areanm").getValue(String::class.java) ?: ""
                    val placeName = child.child("placenm").getValue(String::class.java) ?: ""
                    val svcName = child.child("svcnm").getValue(String::class.java) ?: ""
                    val svcStatus = child.child("svcstatnm").getValue(String::class.java) ?: ""
                    val payType = child.child("payatnm").getValue(String::class.java) ?: ""
                    val imgUrl = child.child("imgurl").getValue(String::class.java) ?: ""
                    val minClassNm = child.child("minclassnm").getValue(String::class.java) ?: ""

                    facilityList.add(
                        FacilitySummaryItem(
                            areaName, placeName, svcName, svcStatus, payType, imgUrl, minClassNm
                        )
                    )
                }
            }

            facilityAdapter.updateItems(facilityList)
            filterFacilities()
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
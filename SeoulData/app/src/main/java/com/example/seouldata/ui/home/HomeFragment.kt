package com.example.seouldata.ui.home

import com.example.seouldata.ui.decorations.VerticalSpaceItemDecoration
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import com.google.android.gms.location.LocationRequest
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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
            // 클릭 시 행동
            Toast.makeText(
                requireContext(),
                "${facilitySummaryItem.placeName} 클릭됨",
                Toast.LENGTH_SHORT
            ).show()
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

        binding.iconLocation.setOnClickListener {
            // 위치 권한 있으면 현재 위치 가져오기
            if (RequestPermissionUtil.hasLocationPermission(requireActivity())) {
                getCurrentLocation()
            } else {
                // 위치 권한 요청
                RequestPermissionUtil.requestLocationPermission(requireActivity())
            }
        }
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
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10초마다 위치 갱신
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

            locationResult?.let {
                // locations가 비어있지 않으면 처리
                if (it.locations.isNotEmpty()) {
                    val location = it.locations[0]
                    Log.d(TAG, "새로운 위치: ${location.latitude}, ${location.longitude}")
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
            Log.e(TAG, "❌ 위치 권한이 없습니다. getCurrentLocation() 중단")
            return
        }

        // 마지막 위치 가져오기
        fusedLocationClient
            .getLastLocation()
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "✅ 새로 요청된 현재 위치: ${location.latitude}, ${location.longitude}")
                    val latitude = location.latitude
                    val longitude = location.longitude
                    getAddressFromLocation(latitude, longitude)
                } else {
                    Log.w(TAG, "⚠️ lastLocation is null → 새로 요청")
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
            Log.e(TAG, "❌ 위치 권한 없음 - requestNewLocation() 종료")
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
                    Log.d(TAG, "✅ 새로 요청된 현재 위치: $lat, $lon")
                    getAddressFromLocation(lat, lon)
                } else {
                    Log.e(TAG, "❌ getCurrentLocation 요청 실패 (location == null)")
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "❌ 위치 요청 중 오류: ${it.message}")
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
        val database = FirebaseDatabase.getInstance()
        val testDataRef = database.getReference("DATA")

        testDataRef.get().addOnSuccessListener { snapshot ->
            facilityList.clear()


            snapshot.children.forEach { child ->
                val areaName = child.child("areanm").getValue(String::class.java) ?: ""
                val placeName = child.child("placenm").getValue(String::class.java) ?: ""
                val svcName = child.child("svcnm").getValue(String::class.java) ?: ""
                val svcStatus = child.child("svcstatnm").getValue(String::class.java) ?: ""
                val payType = child.child("payatnm").getValue(String::class.java) ?: ""
                val imgUrl= child.child("imgurl").getValue(String::class.java) ?: ""
                Log.d(TAG, "areaName: $areaName, district: $district")

                // 필터링 : areaName 또는 placeName 안에 district(마포구 등)가 들어가면 추가
                if (areaName.contains(district)) {
                    val facility = FacilitySummaryItem(
                        areaName = areaName,
                        placeName = placeName,
                        svcName = svcName,
                        svcStatus = svcStatus,
                        payType = payType,
                        imgUrl = imgUrl
                    )
                    facilityList.add(facility)
                }
            }
            facilityAdapter.notifyDataSetChanged()
            Log.d(TAG, "필터링된 체육시설 수: ${facilityList.size}")
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
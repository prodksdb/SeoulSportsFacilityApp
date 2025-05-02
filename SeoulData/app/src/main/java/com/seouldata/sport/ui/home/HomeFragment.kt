package com.seouldata.sport.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.database.FirebaseDatabase
import com.seouldata.sport.FacilityActivity
import com.seouldata.sport.MapDialogFragment
import com.seouldata.sport.data.db.AppDatabaseProvider
import com.seouldata.sport.data.mapper.toDto
import com.seouldata.sport.databinding.FragmentHomeBinding
import com.seouldata.sport.dto.FacilitySummaryItem
import com.seouldata.sport.ui.adapter.FacilityAdapter
import com.seouldata.sport.ui.decorations.VerticalSpaceItemDecoration
import com.seouldata.sport.util.RequestPermissionUtil
import kotlinx.coroutines.launch
import java.util.Locale
import com.seouldata.sport.R
import com.seouldata.sport.data.mapper.toEntity

private const val TAG = "HomeFragment"

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var facilityAdapter: FacilityAdapter
    private val facilityList = mutableListOf<FacilitySummaryItem>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentDistrict: String = ""
    private var currentCategory: String = "전체"
    private var currentFilteredList: List<FacilitySummaryItem> = emptyList()
    private var firstLoadDone = false

    // 앱 첫 실행 여부 확인
    private fun isFirstRun(): Boolean {
        val prefs =
            requireContext().getSharedPreferences("app_prefs", AppCompatActivity.MODE_PRIVATE)
        return prefs.getBoolean("is_first_run", true)
    }

    // 첫 실행 이후 다시 실행 시 false로 기록
    private fun setFirstRunComplete() {
        val prefs =
            requireContext().getSharedPreferences("app_prefs", AppCompatActivity.MODE_PRIVATE)
        prefs.edit().putBoolean("is_first_run", false).apply()
    }

    // Room DB 초기화 함수
    private fun checkAndInsertInitialData(city: String, district: String) {
        if (!isFirstRun()) return

        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("DATA")

        ref.get().addOnSuccessListener { snapshot ->
            val list = mutableListOf<FacilitySummaryItem>()
            snapshot.children.forEach { child ->
                val x = child.child("x").getValue(String::class.java) ?: return@forEach
                val y = child.child("y").getValue(String::class.java) ?: return@forEach
                val areaName = child.child("areanm").getValue(String::class.java) ?: return@forEach
                val svcId = child.child("svcid").getValue(String::class.java) ?: return@forEach

                val facility = FacilitySummaryItem(
                    svcId = svcId,
                    areaName = areaName,
                    placeName = child.child("placenm").getValue(String::class.java) ?: "",
                    svcName = child.child("svcnm").getValue(String::class.java) ?: "",
                    svcStatus = child.child("svcstatnm").getValue(String::class.java) ?: "",
                    payType = child.child("payatnm").getValue(String::class.java) ?: "",
                    imgUrl = child.child("imgurl").getValue(String::class.java) ?: "",
                    minClassNm = child.child("minclassnm").getValue(String::class.java) ?: "",
                    telNo = child.child("telno").getValue(String::class.java) ?: "",
                    vMin = child.child("v_min").getValue(String::class.java) ?: "",
                    vMax = child.child("v_max").getValue(String::class.java) ?: "",
                    x = x,
                    y = y,
                    detailContent = child.child("dtlcont").getValue(String::class.java) ?: ""
                )
                list.add(facility)
            }

            val dao = AppDatabaseProvider.getDatabase(requireContext()).facilityDao()
            lifecycleScope.launch {
                dao.deleteAll()
                dao.insertAll(list.map { it.toEntity() })
                setFirstRunComplete()

                firstLoadDone = true
                loadFacilitiesFromRoom(district)
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbarHome)

        homeViewModel.text.observe(viewLifecycleOwner) {
            binding.textToolbarTitle.text = it
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        facilityAdapter = FacilityAdapter(facilityList) { facilitySummaryItem ->
            Toast.makeText(
                requireContext(),
                "${facilitySummaryItem.placeName} 선택!",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(context, FacilityActivity::class.java)
            intent.putExtra("facilityItem", facilitySummaryItem)
            startActivity(intent)
        }

        binding.recyclerFacilities.apply {
            adapter = facilityAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val spacingInPx = resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)
        binding.recyclerFacilities.addItemDecoration(VerticalSpaceItemDecoration(spacingInPx))

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 현재 위치 불러오는 함수
        binding.btnLocation.setOnClickListener {
            if (RequestPermissionUtil.hasLocationPermission(requireActivity())) {
                getCurrentLocation()
            } else {
                RequestPermissionUtil.requestLocationPermission(requireActivity())
            }
        }

//        // Dialog -> Fragment
//        setFragmentResultListener("map_result") { _, bundle ->
//            if (!isAdded || _binding == null) return@setFragmentResultListener
//            val lat = bundle.getDouble("selected_lat")
//            val lng = bundle.getDouble("selected_lng")
//            fetchNearbyFacilities(lat, lng, radiusInMeters = 2000)
//        }

        binding.fabLocation.setOnClickListener {
            val dialog = MapDialogFragment()
            dialog.show(parentFragmentManager, "MapDialog")
        }

        checkLocationPermissionAndFetch()
    }

    // spinner를 설정해서 사용자가 category 선택할 수 있도록 함
    private fun setupCategoryDropdown(categories: List<String>) {
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        val dropdown = binding.categoryDropdown
        dropdown.setAdapter(adapter)
        dropdown.setText(currentCategory, false)

        dropdown.setOnItemClickListener { parent, _, position, _ ->
            // 데이터가 로드되기 전에는 선택 무시
            if (!firstLoadDone) return@setOnItemClickListener

            val selected = parent.getItemAtPosition(position).toString()
            if (selected != currentCategory) {
                currentCategory = selected
                filterFacilities()
            }
        }
    }

    // 위치 권한 확인(있으면 현재 위치 가져옴, 없으면 권한 요청)
    private fun checkLocationPermissionAndFetch() {
        if (RequestPermissionUtil.hasLocationPermission(requireActivity())) {
            getCurrentLocation()
        } else {
            RequestPermissionUtil.requestLocationPermission(requireActivity())
        }
    }

    // 현재 위치 불러오기
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        // 최근에 기록된 위치
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                getAddressFromLocation(location.latitude, location.longitude)
            } else {
                requestNewLocation()
            }
        }
    }

    // 좌표를 실제 주소로 변환하는 함수
    private fun getAddressFromLocation(lat: Double, lon: Double) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        if (!addresses.isNullOrEmpty()) {  // 주소 결과가 null도 아니고, 빈 리스트도 아닐 때
            val address = addresses[0]
            val city = address.adminArea ?: ""
            val district = address.subLocality ?: address.subAdminArea ?: ""
            currentDistrict = district
            binding.textLocation.text = "현재 위치: $city $district"

            if (isFirstRun()) {
                checkAndInsertInitialData(city, district)
            } else {
                loadFacilitiesFromRoom(district)
            }
        }
    }

    // 새로운 위치 불러오기
    private fun requestNewLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )
            .addOnSuccessListener { location ->
                if (location != null) {
                    getAddressFromLocation(location.latitude, location.longitude)
                }
            }
    }

    // 해당 구에 속한 체육시설만 Room에서 불러오고 RecyclerView에 표시
    private fun loadFacilitiesFromRoom(district: String) {
        // Room DB의 DAO 인스턴스 가져옴
        val dao = AppDatabaseProvider.getDatabase(requireContext()).facilityDao()

        // 코루틴 시작: UI를 멈추지 않고 DB에서 데이터를 비동기로 읽기 위해 코루틴을 사용
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.textEmpty.visibility = View.GONE
            binding.recyclerFacilities.visibility = View.GONE
            facilityList.clear()

            val roomData = dao.getByDistrict(district)
            Log.d(TAG, "roomData size = ${roomData.size}")
            facilityList.addAll(roomData.map { it.toDto() })

            val categoryList = resources.getStringArray(R.array.category).toList()
            setupCategoryDropdown(categoryList)

            firstLoadDone = true  // 데이터 로딩 완료 상태로 표시하고, UI 필터링 적용
            filterFacilities()
        }
    }

    // 현재 선택된 카테고리 기준으로 리스트 필터링 및 업데이트
    private fun filterFacilities() {
        lifecycleScope.launch {
            // ⛔ 아직 데이터 로딩 안 끝났으면 로딩 UI만
            if (!firstLoadDone) {
                binding.progressBar.visibility = View.VISIBLE
                binding.recyclerFacilities.visibility = View.GONE
                binding.textEmpty.visibility = View.GONE
                return@launch
            }

            // ✅ 필터링 시작
            val filteredList = if (currentCategory == "전체") {
                facilityList
            } else {
                facilityList.filter { it.minClassNm.trim().contains(currentCategory) }
            }

            val changed = currentFilteredList != filteredList
            currentFilteredList = filteredList
            facilityAdapter.updateItems(filteredList)

            // 👇 UI 표시 로직
            binding.progressBar.visibility = View.GONE

            if (filteredList.isEmpty()) {
                binding.recyclerFacilities.visibility = View.GONE
                binding.textEmpty.visibility = View.VISIBLE

                // ✅ 로딩 완료 후 && 바뀐 리스트일 때만 Toast
                if (changed && facilityList.isNotEmpty()) {
                    Toast.makeText(requireContext(), "해당 시설이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                binding.textEmpty.visibility = View.GONE
                binding.recyclerFacilities.visibility = View.VISIBLE
            }
        }
    }




//    private fun fetchNearbyFacilities(lat: Double, lng: Double, radiusInMeters: Int) {
//        val dao = AppDatabaseProvider.getDatabase(requireContext()).facilityDao()
//        lifecycleScope.launch {
//            binding.progressBar.visibility = View.VISIBLE
//            binding.textEmpty.visibility = View.GONE
//            binding.recyclerFacilities.visibility = View.GONE
//            facilityList.clear()
//
//            val roomData = dao.getAll()
//            facilityList.addAll(roomData.filter {
//                val facLat = it.y.toDoubleOrNull() ?: return@filter false
//                val facLng = it.x.toDoubleOrNull() ?: return@filter false
//                calculateDistance(lat, lng, facLat, facLng) <= radiusInMeters
//            }.map { it.toDto() })
//
//            val newCategories = listOf("전체") + facilityList.map { it.minClassNm }.distinct().sorted()
//            setupCategoryDropdown(newCategories)
//
//            firstLoadDone = true
//            filterFacilities()
//        }
//    }

//    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
//        val R = 6371000.0
//        val dLat = Math.toRadians(lat2 - lat1)
//        val dLon = Math.toRadians(lon2 - lon1)
//        val a = Math.sin(dLat / 2).pow(2.0) +
//                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
//                Math.sin(dLon / 2).pow(2.0)
//        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
//        return R * c
//    }

    override fun onStart() {
        super.onStart()
        startLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    /** 위치 자동 업데이트 함수 **/
// 사용자의 위치 정보를 주기적으로 갱신하기 위해 호출되는 함수
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    // 더 이상 위치 정보를 받지 않도록 위치 업데이트를 중지하는 함수
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // 위치 업데이트가 발생할 때마다 호출되는 콜백 함수
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (locationResult.locations.isNotEmpty()) {
                val location = locationResult.locations[0]
                getAddressFromLocation(location.latitude, location.longitude)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupCategoryDropdown(resources.getStringArray(R.array.category).toList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
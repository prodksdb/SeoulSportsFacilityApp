package com.example.seouldata.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.seouldata.FacilityActivity
import com.example.seouldata.R
import com.example.seouldata.api.RetrofitClient
import com.example.seouldata.databinding.FragmentHomeBinding
import com.example.seouldata.dto.FacilitySummaryItem
import com.example.seouldata.ui.adapter.FacilityAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.Locale

// Home 화면 UI 담당
private const val TAG = "HomeFragment"
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    //
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private var selectedCategory = ""
    private var isLoading = false
    private var currentPage = 1
    private val pageSize = 40

    private lateinit var facilityAdapter: FacilityAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbarHome)

        setupSpinner()

        val textView: TextView = binding.textToolbarTitle
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // fuesdLocationClient : 위치를 가져올 떄 구글 Play 서비스 기반으로 제공하는 객체
        // 기존 LocationManager보다 후러씬 정확하고, 배터리 소모도 덜한다
        // requireActivity()를 기반으로 초기화할 수 있음
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())  // Fragment는 자기 자신(Context)말고 Activity 기반으로 초기화해야 안정적임
        checkLocationPermission()

        // 버튼 클릭 리스너 추가: 위치 갱신
        val refreshButton: View = binding.iconLocation
        refreshButton.setOnClickListener {
            getCurrentLocation()  // 위치 갱신 함수 호출
        }

        setupRecycler()
        setupRecyclerScrollListener()
        requestFacilities(selectedCategory)
    }

    // 앱이 위치 권한을 가지고 있는지 확인하는 함수
    private fun checkLocationPermission() {
        // ContextCompat.checkSelfPermission : 위치 권한이 있는지 검사
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 없으면 팝업이 자동으로 뜨고 권한 요청
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // 이미 권한이 있으면 위치 가져오기
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 허용되면 다시 위치 가져오기
                getCurrentLocation()
            } else {
                // 권한 거부되면 안내
                Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        // 위치 권한이 없다면 그냥 리턴
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 위치 업데이트 강제 요청
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000  // 위치 업데이트 간격을 1000ms(1초)로 설정, 1초마다 위치 업데이트
            fastestInterval = 500 // 최고 빈도로 위치를 요청할 때 500ms(0.5초)로 설정
            numUpdates = 1  // 한 번만 업데이트 받기
        }

        // requestLocationUpdates() : 위치 업데이트 요청을 보내는 함수
        fusedLocationClient.requestLocationUpdates(
            locationRequest,  // 위치 업데이트에 대한 요구 사항 설정하는 객체
            object : LocationCallback() {  // 위치가 업데이트될 때 호출되는 콜백 함수
                // 위치 결과가 반환되면 호출되는 함수
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation  // 최신 위치 정보 받아옴
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        getAddressFromLocation(latitude, longitude)  // 새로 업데이트된 위치로 주소 가져오기
                        Log.d(TAG, "Current Location: $latitude, $longitude")
                        Toast.makeText(requireContext(), "위치 업데이트 성공 $latitude, $longitude", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "위치 업데이트 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            null // Looper 객체 , 위치 업데이트를 받을 스레드를 설정하는 매개변수,
        // 기본적으로 null이면 현재 스레드에서 콜백이 처리되지만, 다른 스레드에서 처리하고 싶으면, Looper를 사용해 다른 스레드를 지정할 수 있다
        )
    }


    @SuppressLint("SetTextI18n")
    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]

                // 시/도, 구까지만 받아오기
                val adminArea = address.adminArea ?: "Unknown"  // 서울특별시
                val subLocality = address.subLocality ?: "Unknown"  // 성동구

                // 구까지만 표시
                val fullAddress = "$adminArea $subLocality"
                binding.textLocation.text = "현재 위치: $fullAddress"

                // "구" 이름만 추출 (성동구 -> 성동)
                val subLocalityTrimmed = subLocality.replace("구", "").trim()

                // 구에 해당하는 시설만 요청
                fetchFacilitiesForArea(subLocalityTrimmed) // "성동"만 보내기
            } else {
                Toast.makeText(requireContext(), "주소를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "위치 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchFacilitiesForArea(subLocality: String) {
        val apiKey = getString(R.string.seoul_api_key)

        val start = (currentPage - 1) * pageSize + 1
        val end = currentPage * pageSize
        // 구에 해당하는 시설만 요청
        viewLifecycleOwner.lifecycleScope.launch {
            val response = RetrofitClient.instance.getFacilitiesByCategory(apiKey, start, end, selectedCategory)
            if (response.isSuccessful) {
                val rows = response.body()
                    ?.getAsJsonObject("ListPublicReservationSport")
                    ?.getAsJsonArray("row")

                // 받은 데이터에서 'areaName'이 구와 일치하는 시설만 필터링
                // 필터링된 시설 리스트
                val filteredFacilities = rows?.map { item ->
                    val facility = Gson().fromJson(item, FacilitySummaryItem::class.java)

                    // 1. 선택된 카테고리와 일치하는 시설만 필터링
                    val isCategoryMatch = if (selectedCategory.isNotEmpty()) {
                        facility.minClassNm.contains(selectedCategory, ignoreCase = true)
                        Log.d(TAG, "fetchFacilitiesForArea: $selectedCategory")
                    } else {
                        true  // 카테고리가 비어 있으면 모든 시설을 포함
                    }

                    // 2. 구 이름과 일치하는 시설만 필터링
                    val isAreaMatch = facility.areaName?.replace("구", "")?.trim() == subLocality.trim()

                    // 두 조건이 모두 맞는 경우만 반환
                    if (isCategoryMatch as Boolean && isAreaMatch) {
                        facility
                    } else {
                        null
                    }
                }?.filterNotNull() ?: emptyList()

                // 필터링된 시설 리스트 업데이트
                facilityAdapter.addItems(filteredFacilities)

                Log.d(TAG, "Total Facilities: ${rows?.size()}")
            } else {
                Toast.makeText(requireContext(), "시설 데이터를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSpinner() {
        val dropdown: MaterialAutoCompleteTextView = binding.dropdownCategory
        val categories = resources.getStringArray(R.array.category_array).toList()
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, categories)
        dropdown.setAdapter(adapter)

        dropdown.setOnItemClickListener { parent, view, position, id ->
            selectedCategory = parent.getItemAtPosition(position).toString()
            if (selectedCategory == "전체") selectedCategory = ""
            Toast.makeText(
                requireContext(),
                "선택 : ${if (selectedCategory == "") "전체" else selectedCategory}",
                Toast.LENGTH_SHORT
            ).show()
            currentPage = 1  // 페이지 리셋
            // 다시 시설 데이터를 요청
            requestFacilities(selectedCategory)

            facilityAdapter = FacilityAdapter(mutableListOf()) { selectedFacility ->
                val intent = Intent(requireContext(), FacilityActivity::class.java)
                intent.putExtra("facilityItem", selectedFacility)
                startActivity(intent)
            }
            binding.recyclerFacilities.adapter = facilityAdapter

            requestFacilities(selectedCategory)
        }
    }

    private fun setupRecycler() {
        facilityAdapter = FacilityAdapter(mutableListOf()) { selectedFacility ->
            val intent = Intent(requireContext(), FacilityActivity::class.java)
            intent.putExtra("facilityItem", selectedFacility) ////  전체 객체 넘김
            startActivity(intent)
        }
        binding.recyclerFacilities.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFacilities.adapter = facilityAdapter
    }

    private fun setupRecyclerScrollListener() {
        binding.recyclerFacilities.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= (totalItemCount * 0.9).toInt()) {
                        loadNextPage()
                    }
                }
            }
        })
    }

    // requestFacilities에서 카테고리를 요청하는 코드 수정
    private fun requestFacilities(category: String) {
        val apiKey = getString(R.string.seoul_api_key)
        val start = (currentPage - 1) * pageSize + 1
        val end = currentPage * pageSize

        // 카테고리가 비어있으면, 즉 "전체"일 경우 모든 카테고리 데이터를 요청
        viewLifecycleOwner.lifecycleScope.launch {
            val response =
                RetrofitClient.instance.getFacilitiesByCategory(apiKey, start, end, category)
            if (response.isSuccessful) {
                val rows = response.body()
                    ?.getAsJsonObject("ListPublicReservationSport")
                    ?.getAsJsonArray("row")

                val facilityList = rows?.map { item ->
                    Gson().fromJson(item, FacilitySummaryItem::class.java)
                } ?: emptyList()

                facilityAdapter.addItems(facilityList)
                currentPage++
                isLoading = false
            }
        }
    }

    private fun loadNextPage() {
        isLoading = true
        val start = (currentPage - 1) * pageSize + 1
        val end = currentPage * pageSize
        val apiKey = getString(R.string.seoul_api_key)

        viewLifecycleOwner.lifecycleScope.launch {
            val response = RetrofitClient.instance.getFacilitiesByCategory(
                apiKey,
                start,
                end,
                selectedCategory
            )
            if (response.isSuccessful) {
                val rows = response.body()
                    ?.getAsJsonObject("ListPublicReservationSport")
                    ?.getAsJsonArray("row")

                val facilityList = rows?.map { item ->
                    Gson().fromJson(item, FacilitySummaryItem::class.java)
                } ?: emptyList()

                facilityAdapter.addItems(facilityList)
                currentPage++
                isLoading = false
            }
        }
    }


}
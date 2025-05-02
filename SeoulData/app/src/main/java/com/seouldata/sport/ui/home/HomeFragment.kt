package com.seouldata.sport.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
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
import com.seouldata.sport.FacilityActivity
import com.seouldata.sport.MapDialogFragment
import com.seouldata.sport.R
import com.seouldata.sport.data.db.AppDatabaseProvider
import com.seouldata.sport.data.mapper.toDto
import com.seouldata.sport.databinding.FragmentHomeBinding
import com.seouldata.sport.dto.FacilitySummaryItem
import com.seouldata.sport.ui.adapter.FacilityAdapter
import com.seouldata.sport.ui.decorations.VerticalSpaceItemDecoration
import com.seouldata.sport.util.RequestPermissionUtil
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

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
    private var fullCategoryList: List<String> = emptyList()
    private var firstLoadDone = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbarHome)

        homeViewModel.text.observe(viewLifecycleOwner) {
            binding.textToolbarTitle.text = it
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        facilityAdapter = FacilityAdapter(facilityList) { facilitySummaryItem ->
            Toast.makeText(requireContext(), "${facilitySummaryItem.placeName} 클릭됨", Toast.LENGTH_SHORT).show()
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
        setupCategoryDropdown(resources.getStringArray(R.array.category).toList())

        binding.btnLocation.setOnClickListener {
            if (RequestPermissionUtil.hasLocationPermission(requireActivity())) {
                getCurrentLocation()
            } else {
                RequestPermissionUtil.requestLocationPermission(requireActivity())
            }
        }

        setFragmentResultListener("map_result") { _, bundle ->
            if (!isAdded || _binding == null) return@setFragmentResultListener
            val lat = bundle.getDouble("selected_lat")
            val lng = bundle.getDouble("selected_lng")
            fetchNearbyFacilities(lat, lng, radiusInMeters = 2000)
        }

        binding.fabLocation.setOnClickListener {
            val dialog = MapDialogFragment()
            dialog.show(parentFragmentManager, "MapDialog")
        }

        checkLocationPermissionAndFetch()
    }

    private fun setupCategoryDropdown(categories: List<String>) {
        fullCategoryList = categories
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, fullCategoryList)
        val dropdown = binding.categoryDropdown
        dropdown.setAdapter(adapter)
        dropdown.setText(currentCategory, false)

        dropdown.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            if (selected != currentCategory) {
                currentCategory = selected
                filterFacilities(forceToast = true)
            }
        }
    }

    private fun filterFacilities(forceToast: Boolean = false) {
        val filteredList = if (currentCategory == "전체") {
            facilityList
        } else {
            facilityList.filter { it.minClassNm.trim().contains(currentCategory) }
        }

        val changed = currentFilteredList != filteredList
        currentFilteredList = filteredList

        facilityAdapter.updateItems(filteredList)
        binding.textEmpty.visibility = if (filteredList.isEmpty() && firstLoadDone) View.VISIBLE else View.GONE
        binding.recyclerFacilities.visibility = if (filteredList.isEmpty() && firstLoadDone) View.GONE else View.VISIBLE
        binding.progressBar.visibility = if (!firstLoadDone) View.VISIBLE else View.GONE

        if (filteredList.isEmpty() && (changed || forceToast) && firstLoadDone) {
            Toast.makeText(requireContext(), "해당 시설이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFacilitiesFromRoom(district: String) {
        val dao = AppDatabaseProvider.getDatabase(requireContext()).facilityDao()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.textEmpty.visibility = View.GONE
            binding.recyclerFacilities.visibility = View.GONE
            facilityList.clear()

            val roomData = dao.getAll()
            facilityList.addAll(roomData.filter { it.areaName.contains(district) }.map { it.toDto() })

            val newCategories = listOf("전체") + facilityList.map { it.minClassNm }.distinct().sorted()
            setupCategoryDropdown(newCategories)

            firstLoadDone = true
            filterFacilities()
        }
    }

    private fun getAddressFromLocation(lat: Double, lon: Double) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val city = address.adminArea ?: ""
            val district = address.subLocality ?: address.subAdminArea ?: ""
            currentDistrict = district
            binding.textLocation.text = "현재 위치: $city $district"
            loadFacilitiesFromRoom(district)
        }
    }

    private fun fetchNearbyFacilities(lat: Double, lng: Double, radiusInMeters: Int) {
        val dao = AppDatabaseProvider.getDatabase(requireContext()).facilityDao()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.textEmpty.visibility = View.GONE
            binding.recyclerFacilities.visibility = View.GONE
            facilityList.clear()

            val roomData = dao.getAll()
            facilityList.addAll(roomData.filter {
                val facLat = it.y.toDoubleOrNull() ?: return@filter false
                val facLng = it.x.toDoubleOrNull() ?: return@filter false
                calculateDistance(lat, lng, facLat, facLng) <= radiusInMeters
            }.map { it.toDto() })

            val newCategories = listOf("전체") + facilityList.map { it.minClassNm }.distinct().sorted()
            setupCategoryDropdown(newCategories)

            firstLoadDone = true
            filterFacilities(forceToast = true)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2.0)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun checkLocationPermissionAndFetch() {
        if (RequestPermissionUtil.hasLocationPermission(requireActivity())) {
            getCurrentLocation()
        } else {
            RequestPermissionUtil.requestLocationPermission(requireActivity())
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                getAddressFromLocation(location.latitude, location.longitude)
            } else {
                requestNewLocation()
            }
        }
    }

    private fun requestNewLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    getAddressFromLocation(location.latitude, location.longitude)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        startLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (locationResult.locations.isNotEmpty()) {
                val location = locationResult.locations[0]
                getAddressFromLocation(location.latitude, location.longitude)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

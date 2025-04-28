package com.example.seouldata.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.gson.Gson
import kotlinx.coroutines.launch

// Home 화면 UI 담당
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
        setupRecycler()
        setupRecyclerScrollListener()
        requestFacilities(selectedCategory)
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
            Toast.makeText(requireContext(), "선택 : ${if (selectedCategory == "") "전체" else selectedCategory}", Toast.LENGTH_SHORT).show()
            currentPage = 1

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

    private fun requestFacilities(category: String) {
        val apiKey = getString(R.string.seoul_api_key)
        val start = (currentPage - 1) * pageSize + 1
        val end = currentPage * pageSize

        viewLifecycleOwner.lifecycleScope.launch {
            val response = RetrofitClient.instance.getFacilitiesByCategory(apiKey, start, end, category)
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
            val response = RetrofitClient.instance.getFacilitiesByCategory(apiKey, start, end, selectedCategory)
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
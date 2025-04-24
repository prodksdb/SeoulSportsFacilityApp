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

    // ViewBinding 변수 : FragmentHomeBinding은 fragment_home.xml과 연결된 자동 생성된 클래스

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // onCreateView() : 뷰가 생성됨
    override fun onCreateView(
        inflater: LayoutInflater,  // inflater : XML 레이아웃을 View 객체로 바꿔주는 도구
        container: ViewGroup?,     // container : 이 Fragment가 들어갈 부모 뷰 그룹( ex) FrameLayout)
        savedInstanceState: Bundle? // savedInstancestate : 화면 전환 등에서 이전 상태 저장한 번들(복원용)
    ): View {
        // homeviewmodel : 화면에 보여줄 데이터(텍스트, 리스트 등)를 저장하고 관리하는 역할
        // fragment가 재생성되더라도 viewmodel은 살아남기 때문에 상태 유지 좋음
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        // viewBinding을 통해 fragment_home.xml 레이아웃을 메모리에 띄우는 부분
        // 여기서 _bindingdp 레이아웃과 연결된 객체가 저장됨
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root  // root을 return해야 Fragment가 실제 화면에 보이게 된다


        // Toolbar를 ActionBar로 등록
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbarHome)

        // AutoCompleteTextView 어댑터 설정!!
        val dropdown: MaterialAutoCompleteTextView = binding.dropdownCategory

        val categories = resources.getStringArray(R.array.category_array).toList()

        // 선택된 항목 표시용 커스텀 layout (원하면 spinner_item 써도 됨)
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, categories)
        dropdown.setAdapter(adapter)

        // 항목 선택 이벤트 처리
        dropdown.setOnItemClickListener { parent, view, position, id ->
            selectedCategory = parent.getItemAtPosition(position).toString()
            if (selectedCategory == "전체") selectedCategory = ""
            Toast.makeText(requireContext(), "선택 : $selectedCategory", Toast.LENGTH_SHORT).show()
            currentPage = 1
            requestFacilities(selectedCategory)
        }


        // 원래 있던 텍스트 관찰 코드
        val textView: TextView = binding.textToolbarTitle
        // homeViewModel.text = LiveData<String>  타입, 문자열 데이터를 담고 있고, 변환하면 알려주는 알림 기능
        // observe{..} : 변화를 감시하는 코드
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it  // it은 LiveData에서 새로 전달된 값(문자열)
        }
        return binding.root  // 완성된 전체 화면 View를 반환하는 부분!!
    }

    private var selectedCategory = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestFacilities(selectedCategory)
    }


    // onDestoryView() : 뷰가 사라짐(메모리에서 정리됨)
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var isLoading = false
    private var currentPage = 1
    private val pageSize = 20

    private fun setupRecyclerScrollListener() {
        binding.recyclerFacilities.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(1) && !isLoading) {
                    loadNextPage()
                }
            }
        })
    }

    private fun requestFacilities(category: String) {
        val apiKey = getString(R.string.seoul_api_key)
        val start = (currentPage - 1) * pageSize + 1
        val end = currentPage * pageSize
        var minClassNm = category

        viewLifecycleOwner.lifecycleScope.launch {
            val response =
                RetrofitClient.instance.getFacilitiesByCategory(apiKey, start, end, minClassNm)
            if (response.isSuccessful) {
                val rows = response.body()
                    ?.getAsJsonObject("ListPublicReservationSport")
                    ?.getAsJsonArray("row")

                val facilityList = rows?.map { item ->
                    Gson().fromJson(item, FacilitySummaryItem::class.java)
                } ?: emptyList()

                //전체 객체 넘김
                val F_adapter = FacilityAdapter(facilityList.toMutableList()) { selectedFacility ->
                    val intent = Intent(requireContext(), FacilityActivity::class.java)
                    intent.putExtra(
                        "facilityItem",
                        selectedFacility
                    )
                    startActivity(intent)
                }


                binding.recyclerFacilities.layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerFacilities.adapter = F_adapter

                setupRecyclerScrollListener() // 🎯 스크롤 리스너 꼭 여기에!
                currentPage++
            }
        }
    }

    private fun loadNextPage() {
        isLoading = true
        val start = (currentPage - 1) * pageSize + 1
        val end = currentPage * pageSize
        val apiKey = getString(R.string.seoul_api_key)
        var minClassNm = selectedCategory

        viewLifecycleOwner.lifecycleScope.launch {
            val response =
                RetrofitClient.instance.getFacilitiesByCategory(apiKey, start, end, minClassNm)
            if (response.isSuccessful) {
                val rows = response.body()
                    ?.getAsJsonObject("ListPublicReservationSport")
                    ?.getAsJsonArray("row")

//                val newItems = rows?.map {
//                    it.asJsonObject.get("SVCNM").asString
//                } ?: emptyList()


                val facilityList = rows?.map { item ->
                    Gson().fromJson(item, FacilitySummaryItem::class.java)
                } ?: emptyList()

                (binding.recyclerFacilities.adapter as FacilityAdapter).addItems(facilityList)
                currentPage++
                isLoading = false
            }
        }
    }


}
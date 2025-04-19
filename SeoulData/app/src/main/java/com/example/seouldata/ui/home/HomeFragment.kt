package com.example.seouldata.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.seouldata.FacilityActivity
import com.example.seouldata.R
import com.example.seouldata.databinding.FragmentHomeBinding
import com.example.seouldata.ui.adapter.FacilityAdapter

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
        // Spinner 어댑터 설정!!
        val spinner: Spinner = binding.spinnerCategory

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.category_array,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        // Spinner 선택 이벤트
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCategory = parent.getItemAtPosition(position).toString()
                Toast.makeText(requireContext(), "선택 : $selectedCategory", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 아무것도 선택되지 않았을 때 처리
            }
        }

        /////리스트뷰 테스트용
        val facilityList = listOf("서울 체육관", "잠실 종합운동장", "한강 풋살장")
        binding.recyclerFacilities.layoutManager = LinearLayoutManager(requireContext())
        val adapter = FacilityAdapter(facilityList) { selectedFacility ->
            val intent = Intent(requireContext(), FacilityActivity::class.java)
            startActivity(intent)
        }
        binding.recyclerFacilities.adapter = adapter

        ////

        // 원래 있던 텍스트 관찰 코드
        val textView: TextView = binding.textToolbarTitle
        // homeViewModel.text = LiveData<String>  타입, 문자열 데이터를 담고 있고, 변환하면 알려주는 알림 기능
        // observe{..} : 변화를 감시하는 코드
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it  // it은 LiveData에서 새로 전달된 값(문자열)
        }
        return binding.root  // 완성된 전체 화면 View를 반환하는 부분!!
    }

    // onDestoryView() : 뷰가 사라짐(메모리에서 정리됨)
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
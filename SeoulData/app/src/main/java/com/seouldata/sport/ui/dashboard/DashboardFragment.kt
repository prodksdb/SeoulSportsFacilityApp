package com.seouldata.sport.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.seouldata.sport.ui.dashboard.DashboardViewModel
import com.seouldata.sport.databinding.FragmentDashboardBinding
import com.seouldata.sport.dto.Reservation


import com.seouldata.sport.ui.adapter.ReservationAdapter

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val vm by activityViewModels<DashboardViewModel>()
    private lateinit var adapter: ReservationAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ReservationAdapter()
        binding.recyclerReservationList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@DashboardFragment.adapter
        }

        // ViewModel의 LiveData 관찰 연
        vm.reservations.observe(viewLifecycleOwner) { list: List<Reservation> ->
            adapter.submitList(list)
           // 빈 목록일 때 안내 텍스트 보이기 등 처리 가능

        }

        // 이용 게시판 클릭
        binding.cardReviewBoard.setOnClickListener {
            Toast.makeText(requireContext(), "이용 게시판으로 이동합니다!", Toast.LENGTH_LONG).show()
//            val intent = Intent(requireContext(), ReviewBoardActivity::class.java)
//            startActivity(intent)
        }

        // Q&A 클릭
        binding.cardQnA.setOnClickListener {
            Toast.makeText(requireContext(), "Q&A로 이동합니다!", Toast.LENGTH_LONG).show()
//            val intent = Intent(requireContext(), FaqActivity::class.java)
//            startActivity(intent)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.seouldata.sport.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.seouldata.sport.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

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

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.seouldata.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.seouldata.databinding.FragmentNotificationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nickname = document.getString("nickname") ?: "이름없음"
                        val exp = document.getLong("exp") ?: 0
                        val level = document.getLong("level") ?: 1
                        val characterSkin = document.getString("characterSkin") ?: "default"

                        // 가져온 데이터 화면에 표시
                        binding.tvNickname.text = nickname
                        binding.tvExp.text = "경험치: $exp"
                        binding.tvLevel.text = "레벨: $level"
                        // 스킨은 필요하면 이미지 바꿔주기
                    }
                }
                .addOnFailureListener {
                    // 에러 처리
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
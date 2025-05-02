package com.seouldata.sport.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.seouldata.dto.Reservation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardViewModel : ViewModel() {
    private val _reservations = MutableLiveData<List<Reservation>>()
    val reservations: LiveData<List<Reservation>> = _reservations

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        loadReservations()
    }

    private fun loadReservations() {
        val uid = auth.currentUser?.uid ?: return
        // 실시간 업데이트를 위해 snapshot listener 사용
        firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                val list = (snap.get("reservations") as? List<Map<String, Any>>)
                    ?.mapNotNull { data ->
                        val place = data["placeName"] as? String ?: return@mapNotNull null
                        val dt    = data["dateTime"]  as? String ?: ""
                        val st    = data["status"]    as? String ?: ""
                        Reservation(place, dt, st)
                    } ?: emptyList()
                _reservations.value = list
            }
    }
}
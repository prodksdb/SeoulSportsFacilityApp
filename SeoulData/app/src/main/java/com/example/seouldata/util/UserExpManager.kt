package com.example.seouldata.util

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UserExpManager {
    private val levelUpThreshold = 50L // 테스트용: 50 경험치로 레벨업

    // 경험치 추가 함수
     fun addExperienceAndCheckLevelUp(expToAdd: Long,  context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()

        val userDocRef = db.collection("users").document(userId)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    var exp = document.getLong("exp") ?: 0
                    var level = document.getLong("level") ?: 1
                    val inventory = document.get("inventory") as? ArrayList<String> ?: arrayListOf()

                    exp += expToAdd
                    var rewardGiven = false

                    // 레벨업 체크
                    if (exp >= levelUpThreshold) {
                        level++
                        exp -=  levelUpThreshold // 남은 경험치는 다음 레벨로 넘김

                        // 보상 아이템 지급
                        when (level) {
                            2L -> inventory.add("축구공")
                            3L -> inventory.add("테니스채")
                            5L -> inventory.add("농구공")
                        }
                        rewardGiven = true
                    }

                    // 업데이트할 데이터
                    val updatedData = mapOf(
                        "exp" to exp,
                        "level" to level,
                        "inventory" to inventory
                    )

                    userDocRef.update(updatedData)
                        .addOnSuccessListener {
                            // 성공
                            saveUserToPrefs(context, exp, level, inventory)
                            Log.d("ExpManager", "경험치 업데이트 +${expToAdd} / 레벨업 $rewardGiven")
                        }
                        .addOnFailureListener {
                            // 실패
                            Log.e("ExpManager", "업데이트 실패: ${it.message}")
                        }
                }
            }
    }
    //앱 실행 시 (출석 보상)
    fun checkAttendanceAndGiveReward(context: Context) {
        val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val last = prefs.getString("lastAttendanceDate", null)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        if (last != today) {
            addExperienceAndCheckLevelUp(30L, context)

            prefs.edit().putString("lastAttendanceDate", today).apply()

            // 다이얼로그 띄우기
            val dialog = AlertDialog.Builder(context)
                .setTitle("출석 보상")
                .setMessage("출석 보상으로 30XP를 얻었어요!")
                .create()
            dialog.setCanceledOnTouchOutside(true) // 다이얼로그 밖 터치하면 끄기
            dialog.show()

        } else {
            Log.d("ExpManager", "이미 출석 보상 받음")
        }
    }
    //예약 성공 시 보상
    fun saveUserToPrefs(context: Context, exp: Long, level: Long, inventory: List<String>) {
        val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putLong("exp", exp)
            putLong("level", level)
            putStringSet("inventory", inventory.toSet())
            apply()
        }
    }




}
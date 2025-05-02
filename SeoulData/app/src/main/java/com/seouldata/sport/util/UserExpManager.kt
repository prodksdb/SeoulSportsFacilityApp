package com.seouldata.sport.util

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.seouldata.sport.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UserExpManager {
    private val levelUpThreshold = 50L // 테스트용: 50 경험치로 레벨업

    private val assetNameMap = mapOf(
        "축구공" to "soccer",
        "테니스채" to "tennis",
        "농구공" to "basketball",
        "수영복" to "swimming"
    )


    // 경험치 추가 함수
    fun addExperienceAndCheckLevelUp(expToAdd: Long, context: Context) {
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
                    var rewardName: String? = null


                    // 레벨업 체크
                    if (exp >= levelUpThreshold) {
                        level++
                        exp -= levelUpThreshold // 남은 경험치는 다음 레벨로 넘김

                        // 보상 아이템 지급
                        rewardName = when (level) {
                            2L -> "축구공"
                            3L -> "테니스채"
                            4L -> "농구공"
                            5L -> "수영복"
                            else -> null //추후 추가
                        }
                        rewardName?.let {
                            inventory.add(it)
                            showLevelUpDialog(context, it)  // 🔥 Context 기반 다이얼로그
                            rewardGiven = true
                        }

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

    //레벨업축하다이얼로그
    private fun showLevelUpDialog(context: Context, assetName: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_levelup_reward, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.imageReward)

        val drawableName = assetNameMap[assetName] ?: "swimming"
        val drawableId =
            context.resources.getIdentifier(drawableName, "drawable", context.packageName)
        imageView.setImageResource(drawableId)

        AlertDialog.Builder(context)
            .setTitle("레벨업!")
            .setMessage("[$assetName]을(를) 획득했어요!")
            .setView(dialogView)
            .setPositiveButton("확인", null)
            .show()
    }

    //앱 실행 시 (출석 보상)
    fun checkAttendanceAndGiveReward(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(userId)

        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val last = document.getString("lastAttendanceDate")

                    if (last != today) {
                        // 보상 지급
                        addExperienceAndCheckLevelUp(30L, context)

                        // Firestore에 출석 날짜 저장
                        userDocRef.update("lastAttendanceDate", today)

                        // SharedPreferences도 동기화 (선택사항)
                        context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            .edit().putString("lastAttendanceDate", today).apply()

                        AlertDialog.Builder(context)
                            .setTitle("🎉 출석 보상")
                            .setMessage("출석 보상으로 30XP를 얻었어요!")
                            .create()
                            .apply { setCanceledOnTouchOutside(true); show() }
                    } else {
                        Log.d("ExpManager", "이미 출석 보상 받음 (서버 기준)")
                    }
                }
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
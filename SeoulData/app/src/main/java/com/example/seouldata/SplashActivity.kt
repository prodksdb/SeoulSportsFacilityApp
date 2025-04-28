package com.example.seouldata

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

//        Handler(Looper.getMainLooper()).postDelayed({
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
//        }, 1000) // 1.5초 후 메인 이동

        // 로그인 여부 확인
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // 로그인 유지됨 → 바로 메인 화면
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // 로그인 안되어있음 → 로그인 화면
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish() // 스플래시 화면은 남기지 않고 종료
    }
}
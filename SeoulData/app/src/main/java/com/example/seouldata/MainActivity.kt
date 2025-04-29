package com.example.seouldata

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.seouldata.databinding.ActivityMainBinding
import com.example.seouldata.util.UserExpManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 시스템 창이 Window에 맞게 잘리도록 설정 (상태바 영역 피함)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // 상태바 텍스트를 흰색으로, 상태바를 항상 표시
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false // 텍스트 색상: 흰색

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 출석 체크
        UserExpManager.checkAttendanceAndGiveReward(this)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
//        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


    }
}
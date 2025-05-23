package com.seouldata.sport

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.seouldata.sport.databinding.ActivityLoginBinding
import com.seouldata.sport.util.UserExpManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.animation.AnimatorSetCompat.playTogether
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable.start
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "LoginActivity 싸피"

class LoginActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 변경된 부분: 커스텀 LinearLayout 버튼으로 변경
        val googleLoginButton = findViewById<LinearLayout>(R.id.btn_google_login)
        googleLoginButton.setOnClickListener {
            signIn()
        }

        // TextView에 스케일 애니메이션 적용
        val textScaleX = ObjectAnimator.ofFloat(binding.tvInfo, "scaleX", 1f, 1.1f, 1f)
        val textScaleY = ObjectAnimator.ofFloat(binding.tvInfo, "scaleY", 1f, 1.1f, 1f)

// 반복 설정
        textScaleX.repeatCount = ValueAnimator.INFINITE
        textScaleX.repeatMode = ValueAnimator.REVERSE
        textScaleY.repeatCount = ValueAnimator.INFINITE
        textScaleY.repeatMode = ValueAnimator.REVERSE

        AnimatorSet().apply {
            playTogether(textScaleX, textScaleY)
            duration = 1200                 // 속도 조절 가능
           // interpolator = OvershootInterpolator(2f)  // 튀는 느낌 강조
            start()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                Toast.makeText(this, "구글 로그인 연동 성공!", Toast.LENGTH_SHORT).show()
                val account = task.result
                firebaseAuthWithGoogle(account.idToken!!)
            } else {
                Toast.makeText(this, "구글 로그인 연동 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid ?: return@addOnCompleteListener
                    val userName = user.displayName ?: "이름없는유저"

                    val docRef = FirebaseFirestore.getInstance().collection("users").document(userId)
                    docRef.get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                loadUserData(document)
                            } else {
                                joinNewUser(userId, userName)
                            }
                        }
                } else {
                    Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadUserData(document: DocumentSnapshot) {
        val nickname = document.getString("nickname") ?: "이름없음"
        val exp = document.getLong("exp") ?: 0
        val level = document.getLong("level") ?: 1
        val characterSkin = document.getString("characterSkin") ?: "default"
        val inventory = document.get("inventory") as? List<String> ?: emptyList()

        UserExpManager.saveUserToPrefs(this, exp, level, inventory)

        Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
        goToMain()
    }

    private fun joinNewUser(userId: String, nickname: String) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        val newUserData = hashMapOf(
            "nickname" to nickname,
            "exp" to 0,
            "level" to 1,
            "characterSkin" to "default",
            "inventory" to arrayListOf<String>(),
            "lastAttendanceDate" to today
        )

        FirebaseFirestore.getInstance().collection("users").document(userId)
            .set(newUserData)
            .addOnSuccessListener {
                UserExpManager.saveUserToPrefs(
                    context = this,
                    exp = 0,
                    level = 1,
                    inventory = emptyList()
                )
                Toast.makeText(this, "$nickname 님 환영합니다!", Toast.LENGTH_SHORT).show()
                goToMain()
            }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
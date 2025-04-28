package com.example.seouldata

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.seouldata.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

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

        findViewById<SignInButton>(R.id.btnGoogleLogin).setOnClickListener {
            signIn()
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
                val account = task.result
                firebaseAuthWithGoogle(account.idToken!!)
            } else {
                // 실패 처리
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 로그인 성공 → userId 확보
                    val user = auth.currentUser
                    val userId = user?.uid ?: return@addOnCompleteListener
                    val userName = user.displayName ?: "이름없는유저"

                    // Firestore에 유저 정보 저장
                    val db = FirebaseFirestore.getInstance()
                    val userData = hashMapOf(
                        "nickname" to userName,
                        "exp" to 0,
                        "level" to 1,
                        "characterSkin" to "default"
                    )

                    db.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            // Firestore 저장 성공하면 메인화면 이동
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            // Firestore 저장 실패했을 때 처리 (에러 로그 출력)
                            e.printStackTrace()
                        }
                } else {
                    // 실패 처리
                }
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
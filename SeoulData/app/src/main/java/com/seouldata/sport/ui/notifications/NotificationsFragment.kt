package com.seouldata.sport.ui.notifications

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.seouldata.sport.LoginActivity
import com.seouldata.sport.databinding.FragmentNotificationsBinding
import com.seouldata.sport.dto.InventoryItem
import com.seouldata.sport.ui.adapter.InventoryAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.seouldata.sport.R

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: InventoryAdapter
    private val assetList = mutableListOf<InventoryItem>()

    private var isEditing = false
    private var selectedAssetName: String? = null

    private val assetNameMap = mapOf(
        "축구공" to "soccer",
        "테니스채" to "tennis",
        "농구공" to "basketball",
        "수영복" to "swimming"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val cachedNickname = prefs.getString("nickname", null)
        val cachedCharacter = prefs.getString("characterSkin", "default") ?: "default"
        selectedAssetName = cachedCharacter

        if (cachedNickname != null) {
            val exp = prefs.getLong("exp", 0)
            val level = prefs.getLong("level", 1)
            val maxExp = 100
            binding.tvNickname.text = cachedNickname
            binding.tvLevel.text = "Lv. $level"
            binding.tvExp.text = "$exp/$maxExp"
            binding.progressExp.max = maxExp
            binding.progressExp.progress = exp.toInt()
            val characterResId =
                resources.getIdentifier(cachedCharacter, "drawable", requireContext().packageName)
            binding.itemAsset.setImageResource(characterResId)
        } else {
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
                            selectedAssetName = characterSkin
                            binding.tvNickname.text = nickname
                            binding.tvLevel.text = "Lv. $level"
                            binding.tvExp.text = "$exp/100"
                            binding.progressExp.max = 100
                            binding.progressExp.progress = exp.toInt()
                            val characterResId = resources.getIdentifier(
                                characterSkin,
                                "drawable",
                                requireContext().packageName
                            )
                            binding.itemAsset.setImageResource(characterResId)
                            prefs.edit().apply {
                                putString("nickname", nickname)
                                putLong("exp", exp)
                                putLong("level", level)
                                putString("characterSkin", characterSkin)
                                apply()
                            }
                        }
                    }
            }
        }

        binding.btnLogout.setOnClickListener {
            logout(requireContext())
        }

        binding.btnEdit.setOnClickListener {
            val currentName = binding.tvNickname.text.toString()
            binding.etNickname.hint = currentName
            binding.etNickname.setText(currentName)
            binding.etNickname.requestFocus()
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etNickname, InputMethodManager.SHOW_IMPLICIT)
            toggleEditMode(true)
        }

        binding.btnCancel.setOnClickListener {
            toggleEditMode(false)
        }

        binding.btnSave.setOnClickListener {
            val newNickname = binding.etNickname.text.toString()
            val selectedAsset = selectedAssetName ?: return@setOnClickListener

            if (newNickname.isBlank()) {
                Toast.makeText(requireContext(), "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser
            val uid = user?.uid ?: return@setOnClickListener

            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "nickname" to newNickname,
                        "characterSkin" to selectedAsset
                    )
                )
                .addOnSuccessListener {
                    binding.tvNickname.text = newNickname
                    val newResId = resources.getIdentifier(
                        selectedAsset,
                        "drawable",
                        requireContext().packageName
                    )
                    binding.itemAsset.setImageResource(newResId)

                    val prefs =
                        requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("nickname", newNickname)
                        putString("characterSkin", selectedAsset)
                        apply()
                    }

                    toggleEditMode(false)
                }
        }

        adapter = InventoryAdapter(assetList) { selectedItem ->
            selectedAssetName = assetNameMap[selectedItem.name] ?: "swimming"
            binding.itemAsset.setImageResource(selectedItem.drawableResId)
        }
        binding.recyclerAssets.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerAssets.adapter = adapter

        fetchInventoryFromArray()

        val scaleX = ObjectAnimator.ofFloat(binding.itemAsset, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.itemAsset, "scaleY", 1f, 1.1f, 1f)
        scaleX.repeatCount = ValueAnimator.INFINITE
        scaleX.repeatMode = ValueAnimator.REVERSE
        scaleY.repeatCount = ValueAnimator.INFINITE
        scaleY.repeatMode = ValueAnimator.REVERSE
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 1000
            start()
        }


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)


        // 회원 탈퇴 버튼 클릭
        binding.btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("회원 탈퇴")
                .setMessage("정말로 탈퇴하시겠어요?\n모든 데이터가 삭제됩니다")
                .setPositiveButton("네") { _, _ -> performAccountDeletion() }
                .setNegativeButton("아니요", null)
                .show()
        }
    }

    private fun toggleEditMode(enabled: Boolean) {
        isEditing = enabled
        binding.tvNickname.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.etNickname.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.recyclerAssets.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.btnLogout.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.editButtonsLayout.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun fetchInventoryFromArray() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users")
            .document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val inventoryList =
                        document.get("inventory") as? List<String> ?: return@addOnSuccessListener
                    assetList.clear()
                    inventoryList.forEach { name ->
                        val drawableName = assetNameMap[name] ?: return@forEach
                        val drawableId = resources.getIdentifier(
                            drawableName,
                            "drawable",
                            requireContext().packageName
                        )
                        assetList.add(InventoryItem(name, drawableId))
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun logout(context: Context) {
        val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_REAUTH = 9002

    private fun performAccountDeletion() {
        // ① Silent Sign-In 시도
        googleSignInClient.silentSignIn()
            .addOnSuccessListener { account ->
                account.idToken?.let { reauthenticateWithToken(it) } ?: promptInteractiveSignIn()
            }
            .addOnFailureListener {
                // ② Silent 실패 → 인터랙티브 로그인
                promptInteractiveSignIn()
            }
    }

    private fun reauthenticateWithToken(idToken: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        user.reauthenticate(credential)
            .addOnSuccessListener { deleteAccountData(user.uid) }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "재인증에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteAccountData(uid: String) {
        val db = FirebaseFirestore.getInstance()
        // Firestore 삭제
        db.collection("users").document(uid)
            .delete()
            .addOnSuccessListener {
                // Auth 계정 삭제
                FirebaseAuth.getInstance().currentUser?.delete()
                    ?.addOnSuccessListener {
                        requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            .edit().clear().apply()
                        Intent(requireContext(), LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(this)
                        }
                        Toast.makeText(requireContext(), "회원 탈퇴가 완료되었습니다", Toast.LENGTH_SHORT).show()
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(requireContext(), "계정 삭제 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "데이터 삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
    }

    private val reauthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            task.result.idToken?.let { reauthenticateWithToken(it) }
        } else {
            Toast.makeText(requireContext(), "재인증에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }


    private fun promptInteractiveSignIn() {
        reauthLauncher.launch(googleSignInClient.signInIntent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.lendmark.ui.my

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.lendmark.databinding.DialogEditProfileBinding
import com.example.lendmark.databinding.DialogProfilePhotoOptionsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditProfileDialog(
    private val majors: List<String>,
    private val onProfileUpdated: (() -> Unit)? = null
) : DialogFragment() {

    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // 갤러리 이미지 선택
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) uploadProfileImage(uri)
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditProfileBinding.inflate(LayoutInflater.from(requireContext()))

        setupMajorDropdown()
        loadExistingProfile()

        binding.ivCameraIcon.setOnClickListener { showImagePickerMenu() }
        binding.btnSave.setOnClickListener { saveProfile() }
        binding.btnCancel.setOnClickListener { dismiss() }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        // 다이얼로그의 너비를 화면 너비의 85%로 설정
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    /** 학과 드롭다운 */
    private fun setupMajorDropdown() {
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, majors)
        binding.actvMajor.setAdapter(adapter)
    }

    /** Firestore에서 기존 정보 불러오기 */
    private fun loadExistingProfile() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->

                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.tvEmail.text = doc.getString("email") ?: ""
                val major = doc.getString("department") ?: ""
                binding.actvMajor.setText(major, false)
                binding.etPhoneNumber.setText(doc.getString("phone") ?: "")

                val imageUrl = doc.getString("profileImageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(imageUrl).into(binding.ivProfileImage)
                }
            }
    }

    /** 갤러리 선택 메뉴 */
    private fun showImagePickerMenu() {
        val dialogBinding = DialogProfilePhotoOptionsBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnChoosePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
            dialog.dismiss()
        }

        dialogBinding.btnRemovePhoto.setOnClickListener {
            removeProfileImage()
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // 사진 선택 다이얼로그의 너비를 화면 너비의 85%로 설정
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    /** Firebase Storage 업로드 */
    private fun uploadProfileImage(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$uid.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { url ->
                    saveImageUrlToFirestore(url.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Upload failed.", Toast.LENGTH_SHORT).show()
            }
    }

    /** 업로드된 URL을 Firestore에 저장 */
    private fun saveImageUrlToFirestore(url: String) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .update("profileImageUrl", url)
            .addOnSuccessListener {
                Glide.with(this).load(url).into(binding.ivProfileImage)
                onProfileUpdated?.invoke()
            }
    }

    /** 프로필 사진 삭제 */
    private fun removeProfileImage() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .update("profileImageUrl", null)
            .addOnSuccessListener {
                binding.ivProfileImage.setImageResource(android.R.color.darker_gray)
                onProfileUpdated?.invoke()
            }
    }

    /** 학과 + 전화번호 Firestore 저장 */
    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return

        val updates = mapOf(
            "department" to binding.actvMajor.text.toString(),
            "phone" to binding.etPhoneNumber.text.toString()
        )

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                onProfileUpdated?.invoke()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update profile.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.lendmark.ui.auth.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.example.lendmark.utils.Event
import com.google.firebase.auth.FirebaseAuth


class SignupViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val functions = FirebaseFunctions.getInstance()

    private val _departments = MutableLiveData<List<String>>()
    val departments: LiveData<List<String>> get() = _departments

    private val _signupResult = MutableLiveData<Event<Boolean>>()
    val signupResult: LiveData<Event<Boolean>> get() = _signupResult

    private val _emailVerified = MutableLiveData<Event<Boolean>>()
    val emailVerified: LiveData<Event<Boolean>> get() = _emailVerified

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> get() = _errorMessage


    init {
        loadDepartments()
    }

    // Firestore에서 학과 리스트 불러오기
    private fun loadDepartments() {
        db.collection("timetables")
            .document("2025-fall")
            .collection("departments")
            .get()
            .addOnSuccessListener { result ->
                val deptList = result.documents.mapNotNull { it.getString("department") }
                _departments.value = deptList
            }
            .addOnFailureListener { e ->
                _errorMessage.value = Event("Failed to load departments: ${e.message}")
            }
    }

    // 회원가입
    fun signup(name: String, email: String, phone: String, dept: String, pw: String, confirmPw: String) {
        if (name.isBlank() || email.isBlank() || phone.isBlank() || pw.isBlank() || confirmPw.isBlank()) {
            _errorMessage.value = Event("Please fill in all fields.")
            return
        }
        if (pw != confirmPw) {
            _errorMessage.value = Event("Passwords do not match.")
            return
        }
        if (pw.length < 6) {
            _errorMessage.value = Event("Password must be at least 6 characters long.")
            return
        }

        val auth = FirebaseAuth.getInstance()

        // Firebase Authentication에 계정 생성
        auth.createUserWithEmailAndPassword(email, pw)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val userData = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "department" to dept,
                    "favorites" to emptyList<String>()
                )

                db.collection("users").document(uid)
                    .set(userData)
                    .addOnSuccessListener {
                        _signupResult.value = Event(true)
                    }
                    .addOnFailureListener {
                        _errorMessage.value = Event("Failed to save user data: ${it.message}")
                    }
            }
            .addOnFailureListener {
                _errorMessage.value = Event("Sign up failed: ${it.message}")
            }
    }



    fun verifyCode(email: String, code: String) {
        functions
            .getHttpsCallable("verifyEmailCode")
            .call(hashMapOf("email" to email, "code" to code))
            .addOnSuccessListener { result ->
                val data = result.data as Map<*, *>
                val ok = data["ok"] as? Boolean ?: false
                val reason = data["reason"] as? String ?: ""

                if (ok) {
                    _emailVerified.value = Event(true)
                    _errorMessage.value = Event("Email verified successfully.")
                } else {
                    _emailVerified.value = Event(false)
                    _errorMessage.value = Event(when (reason) {
                        "INVALID" -> "The verification code is incorrect."
                        "EXPIRED" -> "The verification code has expired."
                        "NOT_FOUND" -> "No verification request found for this email."
                        else -> "Verification failed. Please try again."
                    })
                }
            }
            .addOnFailureListener {
                _errorMessage.value = Event("Error verifying code: ${it.message}")
            }
    }
}



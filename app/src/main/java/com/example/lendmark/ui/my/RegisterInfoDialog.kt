package com.example.lendmark.ui.my

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.lendmark.R
import com.example.lendmark.databinding.DialogRegisterInfoBinding
import com.google.android.material.chip.Chip

class RegisterInfoDialog(
    private val onRegister: (
        capacity: Int,
        classType: String,
        tags: List<String>,
        imageUris: List<Uri>
    ) -> Unit
) : DialogFragment() {

    private var _binding: DialogRegisterInfoBinding? = null
    private val binding get() = _binding!!

    /* 선택값 */
    private var selectedClassType = ""
    private val selectedTags = mutableListOf<String>()
    private val selectedUris = mutableListOf<Uri>()

    /* 드롭다운 옵션 */

    private val capacityOptions = listOf(
        "10명 이하",
        "20명",
        "30명",
        "40명",
        "50명 이상"
    )
    /* 강의실 타입 */
    private val classTypeList = listOf(
        "대형 강의실",
        "중형 강의실",
        "소형 강의실",
        "컴퓨터실",
        "세미나실",
        "강당 / 홀"
    )

    /* 태그 리스트 */
    private val featureTags = listOf(
        "빔프로젝터 있음", "화이트보드 있음", "칠판 있음",
        "에어컨 완비", "난방 완비", "창문 많음",
        "밝은 조명", "방음 잘됨", "와이파이 강함",
        "환기 잘됨", "책상 넓음", "의자 편함"
    )

    /* 갤러리 */
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                val addable = 5 - selectedUris.size
                selectedUris.addAll(uris.take(addable))
                updatePreviewImages()
            }
        }

    /* 카메라 */
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val uri = ImageUtils.saveBitmapToCache(requireContext(), bitmap)
                uri?.let {
                    if (selectedUris.size < 5) {
                        selectedUris.add(it)
                        updatePreviewImages()
                    }
                }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogRegisterInfoBinding.inflate(LayoutInflater.from(context))

        setupCapacityDropdown()
        setupClassTypeRadios()
        setupFeatureCheckboxes()
        setupPhotoButtons()
        updateRegisterButtonEnabled()

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnRegister.setOnClickListener {

            val capacityText = binding.dropdownCapacity.text.toString()

            val capacity = when (capacityText) {
                "10명 이하" -> 10
                "20명" -> 20
                "30명" -> 30
                "40명" -> 40
                "50명 이상" -> 50
                else -> 0
            }

            onRegister(
                capacity,
                selectedClassType,
                selectedTags,
                selectedUris
            )
            dismiss()
        }


        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    /* ============================
       수용 인원 (Dropdown)
       ============================ */

    private fun setupCapacityDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            capacityOptions
        )
        binding.dropdownCapacity.setAdapter(adapter)
        binding.dropdownCapacity.setOnItemClickListener { _, _, _, _ ->
            updateRegisterButtonEnabled()
        }
    }

    /* ============================
       강의실 타입 (RadioGroup)
       ============================ */
    private fun setupClassTypeRadios() {
        classTypeList.forEach { type ->
            val rb = RadioButton(requireContext()).apply {
                text = type
                textSize = 16f
            }

            rb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedClassType = type
                    updateRegisterButtonEnabled()
                }
            }

            binding.radioGroupClassType.addView(rb)
        }
    }

    /* ============================
       장비/환경 체크박스
       ============================ */
    private fun setupFeatureCheckboxes() {
        featureTags.forEach { tag ->
            val cb = CheckBox(requireContext()).apply {
                text = tag
                textSize = 15f
            }

            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedTags.add(tag)
                else selectedTags.remove(tag)
                updateTagChips()
            }

            binding.llEquipmentContainer.addView(cb)
        }
    }

    /* ============================
       선택된 태그 Chips
       ============================ */
    private fun updateTagChips() {
        if (selectedTags.isEmpty()) {
            binding.llSelectedInfo.visibility = View.GONE
        } else {
            binding.llSelectedInfo.visibility = View.VISIBLE
            binding.tvSelectedCount.text = "선택된 태그 (${selectedTags.size})"

            binding.chipGroupSelected.removeAllViews()

            selectedTags.forEach { tag ->
                val chip = layoutInflater.inflate(
                    R.layout.item_selected_feature_chip,
                    binding.chipGroupSelected,
                    false
                ) as Chip

                chip.text = tag
                binding.chipGroupSelected.addView(chip)
            }
        }

        updateRegisterButtonEnabled()
    }

    /* ============================
       📸 사진 버튼 (갤러리 + 촬영)
       ============================ */
    private fun setupPhotoButtons() {
        binding.btnAddPhoto.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnTakePhoto.setOnClickListener {
            cameraLauncher.launch(null)
        }
    }

    /* ============================
       📸 선택된 이미지 미리보기
       ============================ */
    private fun updatePreviewImages() {
        binding.llPhotoPreviewContainer.removeAllViews()

        selectedUris.forEach { uri ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(180, 180).apply {
                    setMargins(10, 0, 10, 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
            }

            binding.llPhotoPreviewContainer.addView(imageView)
        }

        binding.photoScrollView.visibility =
            if (selectedUris.isEmpty()) View.GONE else View.VISIBLE

        updateRegisterButtonEnabled()
    }

    /* ============================
       등록 버튼 활성화 조건
       ============================ */
    private fun updateRegisterButtonEnabled() {
        binding.btnRegister.isEnabled =
            selectedClassType.isNotEmpty() &&
                    !binding.dropdownCapacity.text.isNullOrEmpty() &&
                    selectedTags.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

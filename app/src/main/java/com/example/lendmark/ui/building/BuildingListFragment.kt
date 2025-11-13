package com.example.lendmark.ui.building

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lendmark.R
import com.example.lendmark.data.model.Building
import com.example.lendmark.databinding.FragmentBuildingListBinding
import com.google.firebase.firestore.FirebaseFirestore

class BuildingListFragment : Fragment() {

    private var _binding: FragmentBuildingListBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val buildingList = mutableListOf<Building>()
    private lateinit var adapter: BuildingListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuildingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BuildingListAdapter(buildingList) { building ->
            val bundle = Bundle().apply {
                putString("buildingName", building.name)
                putInt("buildingCode", building.code)
                putDouble("lat", building.naverMapLat)
                putDouble("lng", building.naverMapLng)
            }
            findNavController().navigate(
                R.id.action_buildingList_to_roomList,
                bundle
            )
        }

        binding.rvBuildingList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBuildingList.adapter = adapter

        loadBuildings()
    }

    private fun loadBuildings() {
        db.collection("buildings")
            .orderBy("code")   // 🔥 건물 번호 순으로 정렬
            .get()
            .addOnSuccessListener { result ->
                buildingList.clear()

                for (doc in result) {
                    val building = doc.toObject(Building::class.java)

                    // 🔥 essential 필드 null 방지 — 앱 크래시 방지용
                    if (building.name.isNotEmpty() && building.code != 0) {
                        buildingList.add(building)
                    }
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "건물 목록 불러오기 실패: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

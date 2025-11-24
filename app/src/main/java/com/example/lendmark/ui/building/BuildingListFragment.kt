package com.example.lendmark.ui.building

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lendmark.data.model.Building
import com.example.lendmark.databinding.FragmentBuildingListBinding
import com.example.lendmark.ui.main.MainActivity
import com.example.lendmark.ui.room.RoomListFragment
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

        adapter = BuildingListAdapter(requireActivity() as AppCompatActivity, buildingList) { building ->

            val bundle = Bundle().apply {
                putString("buildingId", building.code.toString())  // Firestore 문서 ID
                putString("buildingName", building.name)           // UI용
            }

            (requireActivity() as MainActivity).replaceFragment(
                RoomListFragment().apply { arguments = bundle },
                building.name
            )
        }

        binding.rvBuildingList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBuildingList.adapter = adapter

        loadBuildings()
    }

    private fun loadBuildings() {
        db.collection("buildings")
            .orderBy("code")
            .get()
            .addOnSuccessListener { result ->
                buildingList.clear()

                for (doc in result) {
                    val building = doc.toObject(Building::class.java)

                    // Firestore 문서 ID 저장
                    building.id = doc.id

                    if (building.name.isNotEmpty()) {
                        buildingList.add(building)
                    }
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "건물 목록 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

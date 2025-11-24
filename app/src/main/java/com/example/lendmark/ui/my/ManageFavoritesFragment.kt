package com.example.lendmark.ui.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.lendmark.R
import com.example.lendmark.data.model.Building // Assuming Building model exists
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ManageFavoritesFragment : Fragment() {

    private lateinit var favoriteContainer: LinearLayout
    private lateinit var allContainer: LinearLayout
    private lateinit var btnAddBuilding: MaterialButton

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val allBuildings = mutableListOf<Building>()
    private val favoriteBuildings = mutableListOf<Building>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_manage_favorites, container, false)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbarFavorites)
        favoriteContainer = view.findViewById(R.id.layoutFavoriteBuildings)
        allContainer = view.findViewById(R.id.layoutAllBuildings)
        btnAddBuilding = view.findViewById(R.id.btnAddBuilding)

        toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        loadDataFromFirestore() // Replace mock data with Firestore data

        btnAddBuilding.setOnClickListener {
            showAddBuildingDialog()
        }

        return view
    }

    private fun loadDataFromFirestore() {
        if (userId == null) return

        val userRef = db.collection("users").document(userId)
        val buildingsRef = db.collection("buildings")

        buildingsRef.get().addOnSuccessListener { buildingsSnapshot ->
            allBuildings.clear()
            val buildings = buildingsSnapshot.toObjects(Building::class.java)
            allBuildings.addAll(buildings.mapIndexed { index, building -> building.apply { id = buildingsSnapshot.documents[index].id } })
            allBuildings.sortBy { it.name }

            userRef.get().addOnSuccessListener { userDoc ->
                val favoriteBuildingIds = userDoc.get("favorites") as? List<String> ?: emptyList()
                
                favoriteBuildings.clear()
                favoriteBuildings.addAll(allBuildings.filter { favoriteBuildingIds.contains(it.id) })

                renderFavorites()
                renderAllBuildings()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to load buildings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderFavorites() {
        favoriteContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        if (favoriteBuildings.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "즐겨찾는 건물을 추가해 주세요."
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 14f
            }
            favoriteContainer.addView(tv)
            return
        }

        favoriteBuildings.forEach { building ->
            val itemView = inflater.inflate(R.layout.item_favorite_building, favoriteContainer, false)
            itemView.findViewById<TextView>(R.id.tvFavoriteName).text = building.name
            itemView.findViewById<TextView>(R.id.tvFavoriteRooms).text = "${building.roomCount}개 강의실"
            // You might need an 'X' or 'remove' button here to remove a favorite
            favoriteContainer.addView(itemView)
        }
    }

    private fun renderAllBuildings() {
        allContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        allBuildings.forEach { building ->
            val itemView = inflater.inflate(R.layout.item_all_building, allContainer, false)

            itemView.findViewById<TextView>(R.id.tvAllName).text = building.name
            itemView.findViewById<TextView>(R.id.tvAllRooms).text = "${building.roomCount}개 강의실"

            val tvStar = itemView.findViewById<TextView>(R.id.tvAllStar)
            tvStar.visibility = if (favoriteBuildings.any { it.id == building.id }) View.VISIBLE else View.INVISIBLE

            allContainer.addView(itemView)
        }
    }

    private fun showAddBuildingDialog() {
        val dialog = AddBuildingDialogFragment()

        val candidates = allBuildings.filter { b -> favoriteBuildings.none { it.id == b.id } }

        dialog.setCandidateBuildings(candidates)
        dialog.onBuildingSelected = { selected ->
            if (userId != null) {
                db.collection("users").document(userId).update("favorites", FieldValue.arrayUnion(selected.id))
                    .addOnSuccessListener { loadDataFromFirestore() } // Reload on success
            }
        }

        dialog.show(parentFragmentManager, "AddBuildingDialog")
    }
}

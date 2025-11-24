package com.example.lendmark.ui.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.lendmark.R
import com.example.lendmark.data.model.Building
import com.example.lendmark.ui.main.MainActivity
import com.example.lendmark.ui.room.RoomListFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyFavoriteFragment : Fragment() {

    private lateinit var favoritesContainer: LinearLayout
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_favorite, container, false)
        favoritesContainer = view.findViewById(R.id.my_favorites_container)

        val btnManage = view.findViewById<Button>(R.id.btnManageFavorites)
        btnManage.setOnClickListener {
            (activity as? MainActivity)?.openManageFavorites()
        }

        loadFavoriteBuildings()
        return view
    }

    private fun loadFavoriteBuildings() {
        if (userId == null) return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->

                if (!isAdded) return@addOnSuccessListener

                val favoriteBuildingIds = userDoc.get("favorites") as? List<String> ?: emptyList()

                if (favoriteBuildingIds.isEmpty()) {
                    favoritesContainer.removeAllViews()
                    val tv = TextView(requireContext()).apply {
                        text = "No favorite buildings yet."
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                    favoritesContainer.addView(tv)
                    return@addOnSuccessListener
                }

                // Fetch building details
                db.collection("buildings").whereIn("id", favoriteBuildingIds).get()
                    .addOnSuccessListener { buildingsSnapshot ->

                        if (!isAdded) return@addOnSuccessListener

                        val buildings = buildingsSnapshot.toObjects(Building::class.java)
                        displayFavorites(buildings)
                    }
                    .addOnFailureListener {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Failed to load building details", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to load favorites", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun displayFavorites(buildings: List<Building>) {

        if (!isAdded) return
        favoritesContainer.removeAllViews()

        val safeContext = context ?: return
        val inflater = LayoutInflater.from(safeContext)

        if (buildings.isEmpty()) {
            val tv = TextView(safeContext).apply {
                text = "Your favorite buildings will appear here."
                setTextColor(ContextCompat.getColor(safeContext, R.color.text_secondary))
            }
            favoritesContainer.addView(tv)
            return
        }

        for (building in buildings) {
            val cardView = inflater.inflate(R.layout.item_favorite_building, favoritesContainer, false)

            val buildingNameTextView = cardView.findViewById<TextView>(R.id.tvFavoriteName)
            val roomCountTextView = cardView.findViewById<TextView>(R.id.tvFavoriteRooms)

            buildingNameTextView.text = building.name
            roomCountTextView.text = "${building.roomCount} classrooms"

            cardView.setOnClickListener {
                if (!isAdded) return@setOnClickListener

                val bundle = Bundle().apply {
                    putString("buildingId", building.id)
                    putString("buildingName", building.name)
                }

                (requireActivity() as MainActivity).replaceFragment(
                    RoomListFragment().apply { arguments = bundle },
                    building.name
                )
            }

            favoritesContainer.addView(cardView)
        }
    }
}

package com.example.lendmark.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.lendmark.ui.my.ConfirmCancelDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lendmark.R
import com.example.lendmark.databinding.FragmentHomeBinding
import com.example.lendmark.ui.chatbot.ChatBotActivity
import com.example.lendmark.ui.home.adapter.AnnouncementAdapter
import com.example.lendmark.ui.home.adapter.FrequentlyUsedRoomsAdapter
import com.example.lendmark.ui.home.adapter.RecentlyViewedRoomsAdapter
import com.example.lendmark.ui.home.adapter.SearchResultsAdapter
import com.example.lendmark.ui.my.ReservationDetailDialogFS
import com.example.lendmark.ui.my.ReservationFS
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupSearch()
        observeViewModel()
        homeViewModel.loadHomeData()
    }

    override fun onResume() {
        super.onResume()
        binding.searchBar.text.clear()
        binding.searchBar.clearFocus()
        homeViewModel.clearSearchResults()
        binding.recyclerSearchResults.visibility = View.GONE
    }

    private fun setupUI() {

        binding.recyclerFrequentlyUsed.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.recyclerRecentlyViewed.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.recyclerBuildingList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.recyclerSearchResults.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.tvSeeAllBuildings.setOnClickListener {
            navigateToBookingTab(null)
        }

        binding.ivChatbot.setOnClickListener {
            startActivity(Intent(requireContext(), ChatBotActivity::class.java))
        }

        binding.contentLayout.setOnClickListener {
            hideKeyboard()
            binding.searchBar.clearFocus()
            homeViewModel.clearSearchResults()
            binding.recyclerSearchResults.visibility = View.GONE
        }
    }

    private fun setupSearch() {
        binding.searchBar.setOnEditorActionListener { v, actionId, event ->
            val isEnterKey =
                event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                        event.action == android.view.KeyEvent.ACTION_DOWN

            val isSearchAction =
                actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE

            if (isEnterKey || isSearchAction) {
                homeViewModel.searchBuilding(v.text.toString())
                hideKeyboard()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun observeViewModel() {

        // 공지 슬라이드
        homeViewModel.announcementSlides.observe(viewLifecycleOwner) { items ->
            binding.viewPagerAnnouncements.adapter =
                AnnouncementAdapter(
                    items,
                    onReviewClick = { navigateToReviewPage() }
                )
        }





        homeViewModel.frequentlyUsedRooms.observe(viewLifecycleOwner) {
            binding.recyclerFrequentlyUsed.adapter = FrequentlyUsedRoomsAdapter(it)
        }

        homeViewModel.upcomingReservation.observe(viewLifecycleOwner) { info ->
            if (info == null) {
                binding.includedUpcomingReservation.root.visibility = View.GONE
                return@observe
            }

            binding.includedUpcomingReservation.root.visibility = View.VISIBLE
            binding.includedUpcomingReservation.tvUpcomingReservationDetails.text =
                "${info.roomName} • ${info.time}"

            binding.includedUpcomingReservation.tvSeeDetails.setOnClickListener {
                loadReservationAndShowDialog(info.reservationId)
            }
        }

        homeViewModel.recentViewedRooms.observe(viewLifecycleOwner) { rooms ->
            binding.recyclerRecentlyViewed.adapter = RecentlyViewedRoomsAdapter(rooms)
            binding.recyclerRecentlyViewed.visibility =
                if (rooms.isEmpty()) View.GONE else View.VISIBLE
        }

        homeViewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (results.isEmpty()) {
                binding.recyclerSearchResults.visibility = View.GONE
                return@observe
            }

            binding.recyclerSearchResults.visibility = View.VISIBLE
            binding.recyclerSearchResults.adapter =
                SearchResultsAdapter(results) { buildingName ->
                    navigateToBookingTab(buildingName)
                }
        }
    }

    private fun navigateToBookingTab(buildingName: String?) {

        if (buildingName != null) {
            parentFragmentManager.setFragmentResult(
                "search_request",
                bundleOf("selectedBuilding" to buildingName)
            )
        }

        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav?.selectedItemId = R.id.nav_book
    }

    private fun loadReservationAndShowDialog(reservationId: String) {
        db.collection("reservations").document(reservationId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val reservation = ReservationFS(
                    id = doc.id,
                    buildingId = doc.getString("buildingId") ?: "",
                    roomId = doc.getString("roomId") ?: "",
                    date = doc.getString("date") ?: "",
                    day = doc.getString("day") ?: "",
                    periodStart = doc.getLong("periodStart")?.toInt() ?: 0,
                    periodEnd = doc.getLong("periodEnd")?.toInt() ?: 0,
                    attendees = doc.getLong("people")?.toInt() ?: 0,
                    purpose = doc.getString("purpose") ?: "",
                    status = doc.getString("status") ?: "approved"
                )

                ReservationDetailDialogFS(
                    reservation = reservation,
                    onCancelClick = { showCancelConfirmationDialog(reservation.id) }, // ⭐ 여기 변경됨
                    onRegisterClick = { updateStatus(reservation.id, "finished") }
                ).show(parentFragmentManager, "ReservationDetailDialogFS")
            }
    }

    // 2. 취소 확인 팝업 함수 (MyReservationFragment에 있는 것과 동일)
    private fun showCancelConfirmationDialog(reservationId: String) {
        val dialog = ConfirmCancelDialog {
            // 'Yes' 눌렀을 때 실행될 로직
            updateStatus(reservationId, "canceled")
        }
        dialog.show(parentFragmentManager, "ConfirmCancelDialog")
    }

    private fun updateStatus(id: String, status: String) {
        db.collection("reservations").document(id)
            .update("status", status)
    }

    private fun navigateToReviewPage() {
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav?.selectedItemId = R.id.nav_my

        parentFragmentManager.setFragmentResult(
            "review_request",
            bundleOf("openReview" to true)
        )
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchBar.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

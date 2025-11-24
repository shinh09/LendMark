package com.example.lendmark.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import android.widget.ImageButton
import android.widget.TextView
import com.example.lendmark.R
import com.example.lendmark.ui.home.HomeFragment
import com.example.lendmark.ui.my.MyPageFragment
import com.example.lendmark.ui.notification.NotificationListFragment
import com.example.lendmark.ui.building.BuildingListFragment
import com.example.lendmark.ui.reservation.ReservationMapFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.lendmark.ui.my.ManageFavoritesFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnMenu: ImageButton
    private lateinit var btnNotification: ImageButton
    private lateinit var tvHeaderTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnMenu = findViewById(R.id.btnMenu)
        btnNotification = findViewById(R.id.btnNotification)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        bottomNav = findViewById(R.id.bottomNav)

        supportFragmentManager.addOnBackStackChangedListener {
            updateUiAfterNavigation()
        }

        if (savedInstanceState == null) {
            // Home fragment is the base, don't add to back stack
            replaceFragment(HomeFragment(), "LendMark", addToBackStack = false)
        }

        bottomNav.setOnItemSelectedListener { item ->
            // Don't do anything if the selected tab is already active at the root level.
            val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
            if (supportFragmentManager.backStackEntryCount == 0) {
                 val isSameTab = when(item.itemId) {
                    R.id.nav_home -> currentFragment is HomeFragment
                    R.id.nav_book -> currentFragment is BuildingListFragment
                    R.id.nav_map -> currentFragment is ReservationMapFragment
                    R.id.nav_my -> currentFragment is MyPageFragment
                    else -> false
                }
                if (isSameTab) return@setOnItemSelectedListener false
            }
            
            // When switching to a new tab, clear the entire back stack.
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            val (fragment, title) = when (item.itemId) {
                R.id.nav_home -> HomeFragment() to "LendMark"
                R.id.nav_book -> BuildingListFragment() to "Classroom Reservation"
                R.id.nav_map -> ReservationMapFragment() to "Map View"
                R.id.nav_my -> MyPageFragment() to "My Page"
                else -> return@setOnItemSelectedListener false
            }
            // Root fragments of tabs are not added to the back stack.
            replaceFragment(fragment, title, addToBackStack = false)
            true
        }

        btnNotification.setOnClickListener {
            replaceFragment(NotificationListFragment(), "Notifications")
        }
    }

    fun replaceFragment(fragment: Fragment, title: String, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(title) // Use title as the name for the entry
        }

        transaction.commit()
    }

    private fun updateUiAfterNavigation() {
        val isSubPage = supportFragmentManager.backStackEntryCount > 0
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)

        if (isSubPage) {
            btnMenu.setImageResource(R.drawable.ic_arrow_back)
            btnMenu.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            tvHeaderTitle.text = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1).name
        } else {
            btnMenu.setImageResource(R.drawable.ic_menu)
            btnMenu.setOnClickListener { /* TODO: Drawer menu */ }
            tvHeaderTitle.text = when (currentFragment) {
                is BuildingListFragment -> "Classroom Reservation"
                is ReservationMapFragment -> "Map View"
                is MyPageFragment -> "My Page"
                else -> "LendMark"
            }
        }
        
        // Sync bottom nav with the current root fragment
        val selectedItem = when (currentFragment) {
            is HomeFragment -> R.id.nav_home
            is BuildingListFragment -> R.id.nav_book
            is ReservationMapFragment -> R.id.nav_map
            is MyPageFragment -> R.id.nav_my
            else -> bottomNav.selectedItemId // Keep current selection for sub-pages
        }
        if (bottomNav.selectedItemId != selectedItem) {
            bottomNav.selectedItemId = selectedItem
        }
    }

    fun openManageFavorites() {
        replaceFragment(ManageFavoritesFragment(), "Manage Favorites")
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
        
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressedDispatcher.onBackPressed()
        } else if (currentFragment !is HomeFragment) {
            bottomNav.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressedDispatcher.onBackPressed()
        }
    }
}

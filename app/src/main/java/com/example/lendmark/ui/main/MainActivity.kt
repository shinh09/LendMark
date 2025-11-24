package com.example.lendmark.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment(), "LendMark", isHome = true)
        }

        bottomNav.setOnItemSelectedListener { item ->
            // Do not reselect the same item
            if (bottomNav.selectedItemId == item.itemId && supportFragmentManager.findFragmentById(R.id.main_container) !is ManageFavoritesFragment) {
                return@setOnItemSelectedListener false
            }

            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment(), "LendMark", isHome = true)
                    true
                }
                R.id.nav_book -> {
                    replaceFragment(BuildingListFragment(), "Classroom Reservation")
                    true
                }
                R.id.nav_map -> {
                    replaceFragment(ReservationMapFragment(), "Map View")
                    true
                }
                R.id.nav_my -> {
                    replaceFragment(MyPageFragment(), "My Page")
                    true
                }
                else -> false
            }
        }

        btnNotification.setOnClickListener {
            replaceFragment(NotificationListFragment(), "Notifications")
        }

        // The default behaviour of the menu button is handled in replaceFragment
    }

    fun replaceFragment(fragment: Fragment, title: String, isHome: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
        
        // Add to back stack only if it's not the home fragment being added initially
        if (!isHome || supportFragmentManager.backStackEntryCount > 0) {
             transaction.addToBackStack(title)
        }

        transaction.commit()
        tvHeaderTitle.text = title
        
        if (isHome) {
            btnMenu.setImageResource(R.drawable.ic_menu)
            btnMenu.setOnClickListener {
                // TODO: Drawer menu connection (future implementation)
            }
        } else {
            btnMenu.setImageResource(R.drawable.ic_arrow_back)
            btnMenu.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }

    fun openManageFavorites() {
        replaceFragment(ManageFavoritesFragment(), "Manage Favorites")
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            super.onBackPressed()
            // After popping, update header to match the new top of the stack
            val newTitle = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 2).name
            val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
            tvHeaderTitle.text = newTitle

            // Special handling for My Page navigation
            if (currentFragment is MyPageFragment) {
                 bottomNav.selectedItemId = R.id.nav_my
                 btnMenu.setImageResource(R.drawable.ic_menu) // Revert to hamburger on MyPage
            } else if (currentFragment is HomeFragment) {
                 bottomNav.selectedItemId = R.id.nav_home
                 btnMenu.setImageResource(R.drawable.ic_menu)
            }

        } else {
            finish() // Exit the app if only one entry is on the back stack
        }
    }
}

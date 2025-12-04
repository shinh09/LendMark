package com.example.lendmark.ui.main

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.lendmark.R
import com.example.lendmark.ui.building.BuildingListFragment
import com.example.lendmark.ui.home.HomeFragment
import com.example.lendmark.ui.my.ManageFavoritesFragment
import com.example.lendmark.ui.my.MyPageFragment
import com.example.lendmark.ui.notification.NotificationListFragment
import com.example.lendmark.ui.reservation.ReservationMapFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

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

        // 백스택 변경(뒤로가기 등) 감지 리스너
        supportFragmentManager.addOnBackStackChangedListener {
            updateUiAfterNavigation()
        }

        if (savedInstanceState == null) {
            // 초기 화면 설정
            replaceFragment(HomeFragment(), "LendMark", addToBackStack = false)
        }

        bottomNav.setOnItemSelectedListener { item ->
            val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)

            // 현재 백스택이 없고(메인 탭 화면이고), 이미 선택된 탭을 다시 누른 경우 리로드 방지
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

            // 탭 이동 시에는 쌓여있는 스택을 모두 비움
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            val (fragment, title) = when (item.itemId) {
                R.id.nav_home -> HomeFragment() to "LendMark"
                R.id.nav_book -> BuildingListFragment() to "Classroom Reservation"
                R.id.nav_map -> ReservationMapFragment() to "Map View"
                R.id.nav_my -> MyPageFragment() to "My Page"
                else -> return@setOnItemSelectedListener false
            }

            // 탭 전환은 백스택에 추가하지 않음 (루트 프래그먼트)
            replaceFragment(fragment, title, addToBackStack = false)
            true
        }

        btnNotification.setOnClickListener {
            replaceFragment(NotificationListFragment(), "Notifications")
        }
    }

    /**
     * 프래그먼트 교체 함수
     * @param fragment 이동할 프래그먼트
     * @param title 헤더에 표시할 제목 (백스택 태그로도 사용됨)
     * @param addToBackStack 뒤로가기 시 돌아올 수 있는지 여부 (상세 페이지는 true, 탭 전환은 false)
     */
    fun replaceFragment(fragment: Fragment, title: String, addToBackStack: Boolean = true) {
        // 1. 화면 전환 명령
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(title) // 제목을 백스택의 이름(Name)으로 저장
        }

        transaction.commit()

        // 2. 제목 즉시 변경 (중요: 화면이 바뀌기 전에 제목부터 설정하여 딜레이/고정 현상 방지)
        tvHeaderTitle.text = title

        // 3. 버튼 상태 즉시 업데이트 (선택 사항, updateUiAfterNavigation에서도 처리하지만 반응성을 위해)
        if (addToBackStack) {
            btnMenu.setImageResource(R.drawable.ic_arrow_back)
            btnMenu.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        } else {
            btnMenu.setImageResource(R.drawable.ic_menu)
            btnMenu.setOnClickListener { /* TODO: Drawer open */ }
        }
    }

    /**
     * 뒤로가기(popBackStack) 등이 일어난 후 UI(제목, 버튼)를 동기화하는 함수
     */
    private fun updateUiAfterNavigation() {
        val count = supportFragmentManager.backStackEntryCount
        val isSubPage = count > 0

        if (isSubPage) {
            // 서브 페이지인 경우 (상세 화면)
            btnMenu.setImageResource(R.drawable.ic_arrow_back)
            btnMenu.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

            // 백스택의 가장 위에 있는 항목(현재 화면)의 이름을 가져와서 제목으로 설정
            val currentEntry = supportFragmentManager.getBackStackEntryAt(count - 1)
            tvHeaderTitle.text = currentEntry.name
        } else {
            // 메인 탭 화면인 경우 (루트 화면)
            btnMenu.setImageResource(R.drawable.ic_menu)
            btnMenu.setOnClickListener { /* TODO: Drawer menu */ }

            // 현재 떠있는 프래그먼트를 찾아서 제목 복구
            val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
            tvHeaderTitle.text = when (currentFragment) {
                is BuildingListFragment -> "Classroom Reservation"
                is ReservationMapFragment -> "Map View"
                is MyPageFragment -> "My Page"
                is NotificationListFragment -> "Notifications"
                else -> "LendMark" // HomeFragment 포함
            }

            // 바텀 네비게이션 아이콘 상태 동기화
            val selectedItem = when (currentFragment) {
                is HomeFragment -> R.id.nav_home
                is BuildingListFragment -> R.id.nav_book
                is ReservationMapFragment -> R.id.nav_map
                is MyPageFragment -> R.id.nav_my
                else -> bottomNav.selectedItemId
            }
            if (bottomNav.selectedItemId != selectedItem) {
                bottomNav.selectedItemId = selectedItem
            }
        }
    }

    fun openManageFavorites() {
        replaceFragment(ManageFavoritesFragment(), "Manage Favorites")
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)

        if (supportFragmentManager.backStackEntryCount > 0) {
            // 상세 페이지면 뒤로가기
            super.onBackPressedDispatcher.onBackPressed()
        } else if (currentFragment !is HomeFragment) {
            // 메인 탭인데 홈이 아니면 홈으로 이동
            bottomNav.selectedItemId = R.id.nav_home
        } else {
            // 홈이면 앱 종료
            super.onBackPressedDispatcher.onBackPressed()
        }
    }
}
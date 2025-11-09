com.lendmark
 ├─ LendMarkApp.kt                 // Application (필요 시)
 ├─ navigation/
 │    └─ nav_graph.xml             // 전체 네비게이션
 │
 ├─ ui/
 │   ├─ auth/                      // 로그인 관련
 │   │    ├─ LoginActivity.kt
 │   │    ├─ LoginViewModel.kt
 │   │    └─ AuthRepository(선택)  // data로 빼도 됨
 │   │
 │   ├─ main/                      // 메인 프레임 (BottomNav)
 │   │    ├─ MainActivity.kt
 │   │    └─ MainViewModel.kt      // 공통 상태 필요 시
 │   │
 │   ├─ home/                      // 메인 페이지 (지금 보여준 UI)
 │   │    ├─ HomeFragment.kt
 │   │    ├─ HomeViewModel.kt
 │   │    ├─ adapter/
 │   │    │    ├─ FrequentlyUsedRoomsAdapter.kt
 │   │    │    └─ BuildingListAdapter.kt
 │   │    └─ view/
 │   │         ├─ AnnouncementView.kt
 │   │         └─ UpcomingReservationView.kt
 │   │
 │   ├─ reservation/               // 예약 플로우 전부
 │   │    ├─ ReservationRootFragment.kt     // 예약 탭 진입, 상단에서 지도/검색 탭 전환
 │   │    ├─ ReservationMapFragment.kt      // 지도형
 │   │    ├─ ReservationSearchFragment.kt   // 서치형
 │   │    ├─ BuildingListFragment.kt        // 건물 클릭 후 (필요시 분리)
 │   │    ├─ RoomListFragment.kt            // 해당 건물의 강의실 리스트
 │   │    ├─ TimeTableFragment.kt           // 강의실 타임테이블 + 예약 버튼
 │   │    ├─ ReservationConfirmFragment.kt  // 예약 최종 확인 (선택)
 │   │    ├─ adapter/
 │   │    │    ├─ RoomListAdapter.kt
 │   │    │    └─ TimeTableSlotAdapter.kt
 │   │    └─ ReservationViewModel.kt        // 예약 상태 공통 관리
 │   │
 │   ├─ my/
 │   │    ├─ MyPageFragment.kt              // 마이페이지 메인
 │   │    ├─ MyReservationsFragment.kt      // 나의 예약 목록
 │   │    ├─ ProfileEditFragment.kt         // 프로필 수정 (필요 시)
 │   │    └─ MyPageViewModel.kt
 │   │
 │   ├─ notification/
 │   │    ├─ NotificationListFragment.kt    // 알림 목록 페이지
 │   │    ├─ NotificationDetailFragment.kt  // 알림 상세 (선택)
 │   │    └─ NotificationViewModel.kt
 │   │
 │   └─ common/                             // 공통 UI 컴포넌트
 │        ├─ BaseFragment.kt
 │        ├─ BindingFragment.kt
 │        ├─ view/
 │        │    ├─ LendMarkToolbar.kt
 │        │    └─ CommonDialog.kt
 │        └─ extension/
 │             ├─ ViewExt.kt
 │             └─ FragmentExt.kt
 │
 ├─ data/
 │   ├─ model/
 │   │    ├─ User.kt
 │   │    ├─ Building.kt
 │   │    ├─ Room.kt
 │   │    ├─ TimeSlot.kt
 │   │    ├─ Reservation.kt
 │   │    └─ Notification.kt
 │   ├─ repository/
 │   │    ├─ ReservationRepository.kt
 │   │    ├─ BuildingRepository.kt
 │   │    └─ UserRepository.kt
 │   ├─ local/
 │   │    ├─ LendMarkDatabase.kt
 │   │    └─ dao/...
 │   └─ remote/
 │        └─ ApiService.kt
 │
 ├─ util/
 │   ├─ DateTimeExt.kt
 │   ├─ Resource.kt / Result.kt
 │   └─ Logger.kt
 └─ di/                                 // Hilt 쓸 경우

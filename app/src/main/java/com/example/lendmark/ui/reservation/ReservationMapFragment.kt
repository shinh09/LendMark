package com.example.lendmark.ui.reservation

import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.lendmark.R
import com.example.lendmark.data.model.Building
import com.example.lendmark.data.model.ClassSchedule
import com.example.lendmark.data.model.RoomSchedule
import com.example.lendmark.ui.main.MainActivity
import com.example.lendmark.ui.room.RoomListFragment
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.vectormap.*
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import java.text.SimpleDateFormat
import java.util.*

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.kakao.vectormap.camera.CameraUpdateFactory

import android.widget.ImageButton




class ReservationMapFragment : Fragment() {

    private var mapView: MapView? = null
    private var kakaoMap: KakaoMap? = null
    private val db = FirebaseFirestore.getInstance()

    private var buildingLayer: LabelLayer? = null

    private fun isGooglePlayServicesAvailable(): Boolean {
        return try {
            Class.forName("com.google.android.gms.common.GooglePlayServicesUtil")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    //내위치
    private fun isEmulator(): Boolean {
        val model = android.os.Build.MODEL.lowercase()
        val product = android.os.Build.PRODUCT.lowercase()
        val brand = android.os.Build.BRAND.lowercase()

        return (model.contains("emulator")
                || model.contains("sdk")
                || model.contains("genymotion")
                || product.contains("sdk_gphone")
                || brand.contains("generic"))
    }

    private fun moveToMyLocation() {

        // 에뮬레이터인지 체크
        if (isEmulator()) {
            // 안전 fallback
            val fallbackPos = LatLng.from(37.632632, 127.078056) // 다산관
            kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(fallbackPos))
            return
        }

        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
        if (requireContext().checkSelfPermission(permission)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(permission), 4001)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val myPos = LatLng.from(loc.latitude, loc.longitude)
                kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(myPos))
            }
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_reservation_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val myLocationButton = view.findViewById<ImageButton>(R.id.btnMyLocation)

        myLocationButton.setOnClickListener {
            moveToMyLocation()
        }

        mapView = view.findViewById(R.id.map_view)

        mapView?.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {}
                override fun onMapError(error: Exception) {
                    Log.e("KAKAO_MAP", "Map error: ${error.message}", error)
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    // ❶ 지도 먼저 표시
                    loadBuildingsWithOccupancy()

                    // ⭐ 구글 플레이가 있는 기기에서만 현재 위치 기능 활성화
                    if (isGooglePlayServicesAvailable()) {
                        initLocationProvider()
                        requestMyLocation()
                    } else {
                        Log.w("LOCATION", "Google Play Services not available - skipping location")
                    }

                    val labelManager = map.labelManager ?: return     // ⭐ 수정 부분
                    buildingLayer = labelManager.layer
                    buildingLayer?.setVisible(true)
                    buildingLayer?.setClickable(true)

                    map.setOnLabelClickListener { kakaoMap, layer, label ->
                        if (layer !== buildingLayer) return@setOnLabelClickListener false

                        val building = label.tag as? Building ?: return@setOnLabelClickListener false

                        val bundle = Bundle().apply {
                            putString("buildingId", building.code.toString())
                            putString("buildingName", building.name)
                        }

                        val fragment = RoomListFragment().apply {
                            arguments = bundle
                        }

                        (requireActivity() as MainActivity).replaceFragment(
                            fragment,
                            building.name
                        )
                        true
                    }

                    // ⭐ 예약률 포함하여 마커 찍기
                    loadBuildingsWithOccupancy()
                }

                override fun getPosition(): LatLng =
                    LatLng.from(37.632632, 127.078056)

                override fun getZoomLevel() = 16

                override fun isVisible() = true

                override fun getMapViewInfo(): MapViewInfo =
                    MapViewInfo.from("openmap", MapType.NORMAL)
            }
        )
    }
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private fun initLocationProvider() {
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private fun requestMyLocation() {
        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION

        if (requireContext().checkSelfPermission(permission)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(permission), 3001)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val myLat = loc.latitude
                val myLng = loc.longitude

                val myPos = LatLng.from(myLat, myLng)

                // 지도 중심 이동
                kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(myPos))

                addMyLocationMarker(myPos)
            }
        }
    }

    private fun addMyLocationMarker(pos: LatLng) {
        val map = kakaoMap ?: return
        val labelManager = map.labelManager ?: return
        val layer = buildingLayer ?: return

        val markerBitmap = createMyLocationMarker()

        val styles = labelManager.addLabelStyles(
            LabelStyles.from(LabelStyle.from(markerBitmap))
        ) ?: return

        val options = LabelOptions.from(pos)
            .setClickable(false)
            .setStyles(styles)

        layer.addLabel(options)
    }

    private fun createMyLocationMarker(): Bitmap {
        val size = 50
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4285F4") // 파란 원
            style = Paint.Style.FILL
        }

        canvas.drawCircle(
            size / 2f,
            size / 2f,
            size / 2.5f,
            paint
        )

        return bitmap
    }



    // ------------------------------------------------------------
    // ⭐ 오늘 날짜 문자열 구하기 ("2025-11-28")
    // ------------------------------------------------------------
    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        return sdf.format(Date())
    }

    // ⭐ 오늘 요일 구하기 ("Mon", "Tue", ...)
    private fun getTodayDayString(): String {
        val sdf = SimpleDateFormat("EEE", Locale.ENGLISH)
        return sdf.format(Date())
    }

    // ------------------------------------------------------------
    // ⭐ Buildings + Reservations 불러와서 예약률 계산 후 마커 배치
    // ------------------------------------------------------------
    private fun loadBuildingsWithOccupancy() {
        val today = getTodayDateString()

        // 1) 오늘 예약된 것들 먼저 가져오기
        db.collection("reservations")
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { reservationSnap ->

                val todayReservations = reservationSnap.documents

                // 2) 건물 목록 가져오기
                db.collection("buildings")
                    .orderBy("code")
                    .get()
                    .addOnSuccessListener { buildingSnap ->

                        for (doc in buildingSnap) {
                            val building = doc.toObject(Building::class.java)
                            building.id = doc.id

                            // 이 건물 예약만 필터링
                            val myReservations = todayReservations.filter {
                                it.getString("buildingId") == building.code.toString()
                            }

                            // ⭐ 예약률 계산
                            val percent = calculateOccupancyForBuilding(building, myReservations)

                            if (building.naverMapLat != 0.0 && building.naverMapLng != 0.0) {
                                val pos = LatLng.from(
                                    building.naverMapLat,
                                    building.naverMapLng
                                )
                                addMarkerForBuilding(building, pos, percent)
                            }
                        }
                    }
            }
    }

    // ------------------------------------------------------------
    // ⭐ 예약률 계산 함수
    // ------------------------------------------------------------
    private fun calculateOccupancyForBuilding(
        building: Building,
        reservations: List<DocumentSnapshot>
    ): Int {
        var totalSlots = 0
        var reservedSlots = 0

        val todayDay = getTodayDayString() // "Wed"

        // 1) timetable 기반 전체 슬롯 계산
        building.timetable.forEach { (_, roomSchedule: RoomSchedule) ->
            roomSchedule.schedule.forEach { sch: ClassSchedule ->
                if (sch.day == todayDay) {
                    totalSlots += (sch.periodEnd - sch.periodStart + 1)
                }
            }
        }

        // 2) 오늘 예약된 슬롯 계산
        reservations.forEach { res ->
            val ps = res.getLong("periodStart")?.toInt() ?: 0
            val pe = res.getLong("periodEnd")?.toInt() ?: 0
            reservedSlots += (pe - ps + 1)
        }

        if (totalSlots == 0) return 0
        return ((reservedSlots.toDouble() / totalSlots) * 100).toInt()
    }

    // ------------------------------------------------------------
    // ⭐ 예약률 기반 마커 추가
    // ------------------------------------------------------------
    private fun addMarkerForBuilding(
        building: Building,
        position: LatLng,
        percent: Int
    ) {
        val map = kakaoMap ?: return
        val labelManager = map.labelManager ?: return
        val layer = buildingLayer ?: return

        val markerBitmap = createBuildingMarkerBitmap(percent)

        val styles: LabelStyles = labelManager.addLabelStyles(
            LabelStyles.from(LabelStyle.from(markerBitmap))
        ) ?: return

        val options = LabelOptions.from(position)
            .setStyles(styles)
            .setTag(building)
            .setClickable(true)

        layer.addLabel(options)
    }

    // ------------------------------------------------------------
    // ⭐ 예약률 원형 마커 비트맵 생성
    // ------------------------------------------------------------
    private fun createBuildingMarkerBitmap(percent: Int): Bitmap {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = when {
            percent < 40 -> Color.parseColor("#4CAF50")
            percent < 60 -> Color.parseColor("#FFC107")
            percent < 80 -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#F44336")
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }

        val cx = size / 2f
        val cy = size / 2f
        val r = size / 2.4f

        canvas.drawCircle(cx, cy, r, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 35f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val text = "$percent%"
        val yPos = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, cx, yPos, textPaint)

        return bitmap
    }

    override fun onResume() {
        super.onResume()
        mapView?.resume()
    }

    override fun onPause() {
        mapView?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        mapView = null
        kakaoMap = null
        buildingLayer = null
        super.onDestroyView()
    }

}

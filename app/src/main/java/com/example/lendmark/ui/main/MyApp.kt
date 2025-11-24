package com.example.lendmark.ui.main

import android.app.Application
import android.os.Build
import com.kakao.vectormap.KakaoMapSdk
import com.example.lendmark.R

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 에뮬레이터에서는 KakaoMap SDK 초기화하지 않음
        if (!isEmulator()) {
            KakaoMapSdk.init(this, getString(R.string.kakao_native_app_key))
        }
    }

    // 에뮬레이터 감지 함수
    private fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        val model = Build.MODEL
        val product = Build.PRODUCT
        val hardware = Build.HARDWARE
        val manufacturer = Build.MANUFACTURER
        val brand = Build.BRAND
        val device = Build.DEVICE

        return (fingerprint.startsWith("generic")
                || fingerprint.contains("emulator")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || hardware.contains("ranchu")
                || hardware.contains("goldfish")
                || manufacturer.contains("Google")
                || brand.startsWith("generic")
                || device.startsWith("generic"))
    }
}

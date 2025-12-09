package com.example.lendmark.fcm // 패키지명 확인!

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.lendmark.R
import com.example.lendmark.ui.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // 토큰 갱신 시 호출
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM Token: $token")
        // 필요 시 서버로 토큰 다시 전송하는 로직 추가 가능
    }

    // ⭐ 메시지 수신 시 호출 (앱이 켜져있거나 백그라운드일 때 동작)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. 알림(Notification) 페이로드가 있을 때
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }

        // 2. 데이터(Data) 페이로드가 있을 때 (백그라운드 처리에 유리)
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "알림"
            val body = remoteMessage.data["body"] ?: "새로운 알림이 도착했습니다."
            // 데이터만 온 경우에도 알림을 띄우고 싶다면:
            // showNotification(title, body)
        }
    }

    private fun showNotification(title: String?, message: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "lendmark_alert_channel"
        val channelName = "LendMark Alerts"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // 앱 아이콘 (없으면 기본 안드로이드 아이콘 뜸)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 중요도 높음 (헤드업 알림)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 안드로이드 8.0 (Oreo) 이상 대응: 채널 생성 필수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
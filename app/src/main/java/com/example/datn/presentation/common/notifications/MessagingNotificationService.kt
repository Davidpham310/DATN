package com.example.datn.presentation.common.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.datn.app.MainActivity
import com.example.datn.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint

/**
 * Firebase Cloud Messaging Service for handling push notifications
 * Receives notifications when:
 * - New message arrives
 * - Conversation is updated
 * - Read receipts are received
 */
@AndroidEntryPoint
class MessagingNotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MessagingNotifService"
        private const val CHANNEL_ID = "messaging_channel"
        private const val CHANNEL_NAME = "Tin nhắn"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Called when a new FCM token is generated
     * Save this token to Firestore for sending notifications
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d(TAG, "New FCM token: $token")
        
        // TODO: Save token to Firestore
        // Update user's FCM token in their profile document
        // This allows backend to send notifications to this device
    }

    /**
     * Called when a notification is received
     * Parse the message and show appropriate notification
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        android.util.Log.d(TAG, "Message received from: ${message.from}")
        
        // Extract notification data
        val title = message.data["title"] ?: message.notification?.title ?: "Tin nhắn mới"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val conversationId = message.data["conversationId"] ?: ""
        val senderId = message.data["senderId"] ?: ""
        val senderName = message.data["senderName"] ?: "Người dùng"
        
        // Show notification
        showNotification(
            title = title,
            message = body,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName
        )
    }

    /**
     * Create notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Thông báo tin nhắn mới"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show notification with deep link to conversation
     */
    private fun showNotification(
        title: String,
        message: String,
        conversationId: String,
        senderId: String,
        senderName: String
    ) {
        // Create intent to open the conversation when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversationId", conversationId)
            putExtra("senderId", senderId)
            putExtra("senderName", senderName)
            putExtra("openChat", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_24) // TODO: Create this icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        android.util.Log.d(TAG, "Notification shown: $title")
    }
}

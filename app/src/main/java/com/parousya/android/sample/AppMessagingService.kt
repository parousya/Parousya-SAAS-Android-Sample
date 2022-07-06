package com.parousya.android.sample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.parousya.android.sdk.PRSNotificationListener
import com.parousya.android.sdk.ParousyaSAASSDK
import com.parousya.android.sdk.events.SessionStartedNotification
import com.parousya.android.sdk.events.SessionStoppedNotification
import com.parousya.android.sdk.events.ZonePairingRequestNotification
import com.parousya.android.sdk.fcm.SAASNotificationData
import com.parousya.android.sdk.fcm.SAASNotificationsManager
import com.parousya.android.sdk.model.SessionDetails
import com.parousya.android.sdk.util.extensions.isInBackground

/**
 * Created by Tuong (Alan) on 2019-08-30.
 * Copyright (c) 2019 Buuuk. All rights reserved.
 */

class AppMessagingService : FirebaseMessagingService() {

    companion object {
        const val NOTIFICATION_PAYLOAD_EXTRA: String = "saas_notification_payload_extra"
    }

    private val CHANNEL_ID = "PSAAS Channel Id"

    override fun onNewToken(token: String) {
        ParousyaSAASSDK.getInstance().registerPushToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        ParousyaSAASSDK.getInstance().processPushPayload(this, remoteMessage.data, object :
            PRSNotificationListener {

            override fun onPRSNotify(data: SAASNotificationData) {
                if (baseContext.isInBackground()) {
                    sendNotification(this@AppMessagingService, data)
                }
            }
        })
    }

    private fun sendNotification(context: Context, notificationData: SAASNotificationData) {
        createNotificationChannel(context)
        val pm = context.packageManager
        var nid: Int = 123
        val intent = pm.getLaunchIntentForPackage(context.applicationContext.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

            notificationData.payload?.let {
                val beaconUUID = it.getString(SAASNotificationsManager.SERVER_PAYLOAD_BEACON_UUID)
                    ?: ""
                when (notificationData.type) {
                    SAASNotificationsManager.TYPE_PAIRING_REQUEST -> {
                        try {
                            nid = it.getInt(SAASNotificationsManager.SERVER_PAYLOAD_PAIRING_ID)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        val extraData = ZonePairingRequestNotification(
                            beaconUUID = beaconUUID,
                            pairingId = it.getLong(SAASNotificationsManager.SERVER_PAYLOAD_PAIRING_ID)
                        )
                        putExtra(NOTIFICATION_PAYLOAD_EXTRA, extraData)
                    }
                    SAASNotificationsManager.TYPE_SESSION_STARTED -> {
                        val sessionId =
                            it.getString(SAASNotificationsManager.SERVER_PAYLOAD_SESSION_ID, "")
                        val extraData = SessionStartedNotification(
                            sessionId = sessionId,
                            data = SessionDetails(id = sessionId, beaconUuid = "")
                        )
                        putExtra(NOTIFICATION_PAYLOAD_EXTRA, extraData)
                    }
                    SAASNotificationsManager.TYPE_SESSION_ENDED -> {
                        val sessionId =
                            it.getString(SAASNotificationsManager.SERVER_PAYLOAD_SESSION_ID, "")
                        val extraData = SessionStoppedNotification(
                            sessionId = sessionId,
                            data = SessionDetails(id = sessionId, beaconUuid = "")
                        )
                        putExtra(NOTIFICATION_PAYLOAD_EXTRA, extraData)
                    }
                    else -> {

                    }
                }
            }
        }

        var contentIntent: PendingIntent? = null
        if (intent != null)
            contentIntent = PendingIntent.getActivity(
                context.applicationContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )

        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.resources.getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(notificationData.message)
                .setStyle(
                    NotificationCompat.BigTextStyle() // make notification expandable
                        .bigText(notificationData.message)
                )
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(defaultSoundUri)

        val notificationManager: NotificationManagerCompat =
            NotificationManagerCompat.from(context.applicationContext)
        notificationManager.notify(nid, notificationBuilder.build())
    }

    private fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Normal"
            val description = "All Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val defaultSoundUri: Uri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            channel.description = description
            channel.enableVibration(true)
            channel.setSound(defaultSoundUri, attributes)

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }
}
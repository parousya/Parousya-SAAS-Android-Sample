package com.parousya.android.sample.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity

var progressDialog: ProgressDialog? = null

const val CHANNEL_ID = "Sample Channel Id"

fun AppCompatActivity.showLoading() {
    if (progressDialog == null)
        progressDialog = ProgressDialog.show(this, null, "Loading")
    else
        progressDialog?.show()
}

fun AppCompatActivity.hideLoading() {
    progressDialog?.dismiss()
    progressDialog = null
}


fun AppCompatActivity.createNotificationChannel() {
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

        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        channel.description = description
        channel.enableVibration(true)
        channel.setSound(defaultSoundUri, attributes)

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager!!.createNotificationChannel(channel)
    }
}
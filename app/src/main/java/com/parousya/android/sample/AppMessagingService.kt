package com.parousya.android.sample

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.parousya.android.sdk.ParousyaSAASSDK

/**
 * Created by Tuong (Alan) on 2019-08-30.
 * Copyright (c) 2019 Buuuk. All rights reserved.
 */

class AppMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String?) {
        token?.let {
            ParousyaSAASSDK.getInstance().registerPushToken(it)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        ParousyaSAASSDK.getInstance().processPushPayload(this, remoteMessage.data)
    }
}
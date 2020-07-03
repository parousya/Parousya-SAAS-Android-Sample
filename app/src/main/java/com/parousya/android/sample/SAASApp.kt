package com.parousya.android.sample

import android.app.Application
import com.parousya.android.sdk.ParousyaSAASSDK
import com.parousya.android.sdk.api.model.Variant

/**
 * Created by Tuong (Alan) on 2019-09-05.
 * Copyright (c) 2019 Buuuk. All rights reserved.
 */

class SAASApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ParousyaSAASSDK.init(
            this.applicationContext,
            BuildConfig.SAMPLE_CLIENT_ID,
            BuildConfig.SAMPLE_CLIENT_SECRET,
            Variant.UAT
        )
    }
}
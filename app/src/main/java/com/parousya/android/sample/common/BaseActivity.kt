package com.parousya.android.sample.common

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Tuong (Alan) on 2019-08-26.
 * Copyright (c) 2019 Buuuk. All rights reserved.
 */

abstract class BaseActivity : AppCompatActivity() {
    protected val pref: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    protected val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    protected val dfDisplay = SimpleDateFormat("dd MMM yyyy 'at' hh:mm aaa", Locale.getDefault())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        df.timeZone = TimeZone.getTimeZone("UTC")
    }
}
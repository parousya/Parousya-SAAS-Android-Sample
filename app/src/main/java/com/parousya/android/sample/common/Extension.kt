package com.parousya.android.sample.common

import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Tuong (Alan) on 2019-06-25.
 * Copyright (c) 2019 Buuuk. All rights reserved.
 */


fun Long.timeString(): String {
    var millisUntilFinished: Long = this * 1000

    val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
    millisUntilFinished -= TimeUnit.HOURS.toMillis(hours)

    val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
    millisUntilFinished -= TimeUnit.MINUTES.toMillis(minutes)

    val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)

    // Format the string
    return String.format(
            Locale.getDefault(),
            "%02d : %02d : %02d", hours, minutes, seconds
    )
}

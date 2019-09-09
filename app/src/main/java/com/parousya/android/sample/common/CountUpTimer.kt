package com.parousya.android.sample.common

import android.os.CountDownTimer


/**
 * Created by Tuong (Alan) on 2019-06-25.
 * Copyright (c) 2019 Buuuk. All rights reserved.
 */

abstract class CountUpTimer constructor(private val duration: Long, private val initValue: Long = 0) : CountDownTimer(duration, 1000) {

    abstract fun onTicky(second: Long)

    override fun onTick(millisUntilFinished: Long) {
        val second = ((duration - millisUntilFinished) / 1000) + initValue
        onTicky(second)
    }

    override fun onFinish() {
        onTick(duration / 1000)
    }
}
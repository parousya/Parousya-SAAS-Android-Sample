package com.parousya.android.sample.common

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity

var progressDialog: ProgressDialog? = null


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


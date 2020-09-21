package com.parousya.android.sample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.iid.FirebaseInstanceId
import com.parousya.android.sample.BuildConfig.SAMPLE_CLIENT_ID
import com.parousya.android.sample.common.Constants
import com.parousya.android.sample.common.hideLoading
import com.parousya.android.sample.common.showLoading
import com.parousya.android.sdk.PRSCallback
import com.parousya.android.sdk.PRSCustomer
import com.parousya.android.sdk.PRSHost
import com.parousya.android.sdk.ParousyaSAASSDK
import com.parousya.android.sdk.exceptions.SAASException
import com.parousya.android.sdk.model.UserDetails
import kotlinx.android.synthetic.main.activity_home.*
import java.util.*

class HomeActivity : AppCompatActivity() {

    val pref: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        getLocationUpdates()

        text_user_id.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                bt_client_login.isEnabled = s?.isNotEmpty() ?: false
                bt_host_login.isEnabled = s?.isNotEmpty() ?: false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        tv_client_id.text = String.format(Locale.getDefault(), "App Key: %s", SAMPLE_CLIENT_ID)
        when {
            pref.getBoolean(Constants.KEY_IS_HOST_LOGGED_IN, false) -> {
                val intent = Intent(this@HomeActivity, HostActivity::class.java)
                intent.putExtras(getIntent())
                startActivity(intent)
                finish()
            }
            pref.getBoolean(Constants.KEY_IS_CUSTOMER_LOGGED_IN, false) -> {
                val intent = Intent(this@HomeActivity, ClientActivity::class.java)
                intent.putExtras(getIntent())
                startActivity(intent)
                finish()
            }
            else -> {
                findViewById<Button>(R.id.bt_client_login)
                    .setOnClickListener {
                        ParousyaSAASSDK.getInstance().permissionsWizard(this,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            onSuccess = {
                                val userId: String = text_user_id.text.toString()
                                showLoading()
                                PRSCustomer.getInstance().signIn(this, userId,
                                    object : PRSCallback<String> {
                                        override
                                        fun onSuccess(result: String) {
                                            hideLoading()
                                            pref.edit().putBoolean(Constants.KEY_IS_CUSTOMER_LOGGED_IN, true).apply()

                                            startActivity(Intent(this@HomeActivity, ClientActivity::class.java))
                                            finish()
                                        }

                                        override
                                        fun onError(error: SAASException) {
                                            hideLoading()
                                            Snackbar.make(container, error.localizedMessage, Snackbar.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }, onError = { error ->
                                Snackbar.make(container, error.localizedMessage, Snackbar.LENGTH_SHORT).show()
                            })
                    }

                findViewById<Button>(R.id.bt_host_login)
                    .setOnClickListener {
                        ParousyaSAASSDK.getInstance().permissionsWizard(this,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            onSuccess = {
                                val userId: String = text_user_id.text.toString()
                                showLoading()
                                PRSHost.getInstance().signIn(this, userId,
                                    object : PRSCallback<UserDetails> {
                                        override
                                        fun onSuccess(result: UserDetails) {
                                            hideLoading()
                                            pref.edit().putBoolean(Constants.KEY_IS_HOST_LOGGED_IN, true).apply()

                                            startActivity(Intent(this@HomeActivity, HostActivity::class.java))
                                            finish()
                                        }

                                        override
                                        fun onError(error: SAASException) {
                                            hideLoading()
                                            Snackbar.make(container, error.localizedMessage, Snackbar.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }, onError = { error ->
                                Snackbar.make(container, error.localizedMessage, Snackbar.LENGTH_SHORT).show()
                            })
                    }
            }
        }

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
                // Get new Instance ID token
                task.result?.token?.let { token ->
                    ParousyaSAASSDK.getInstance().registerPushToken(token)
                }
            })
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun getLocationUpdates() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 50000
        locationRequest.fastestInterval = 50000
        locationRequest.smallestDisplacement = 170f // 170 m = 0.1 mile
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.lastLocation
                    println("lastLocation: ${location}")
                } else {
                    println("lastLocation not available")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        ParousyaSAASSDK.getInstance().permissionsWizard(this,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            onSuccess = {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                )

            }, onError = { error ->
                Snackbar.make(container, error.localizedMessage, Snackbar.LENGTH_SHORT).show()
            })
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

}
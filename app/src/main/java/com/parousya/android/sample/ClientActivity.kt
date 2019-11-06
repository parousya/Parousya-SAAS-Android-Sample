package com.parousya.android.sample

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.parousya.android.sample.common.*
import com.parousya.android.sdk.*
import com.parousya.android.sdk.exceptions.SAASException
import com.parousya.android.sdk.model.SessionDetails
import com.parousya.android.sdk.model.UserDetails
import kotlinx.android.synthetic.main.activity_client.*
import java.util.*

class ClientActivity : BaseActivity() {

    private lateinit var statusItemAdapter: StatusItemAdapter

    var timer: CountUpTimer = object : CountUpTimer(System.currentTimeMillis() + (60 * 60 * 24)) {
        override fun onTicky(second: Long) {
            tvTimeInfo.text = second.timeString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Customer"

        tv_ver.text = String.format(getString(R.string.lbl_version), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

        renderUI()
        sdkRegister()

        bt_sign_out.setOnClickListener {
            showLoading()
            PRSCustomer.getInstance().signOut(this, object : PRSCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    hideLoading()
                    pref.edit().clear().apply()
                    startActivity(Intent(this@ClientActivity, HomeActivity::class.java))
                    finish()
                }

                override fun onError(error: SAASException) {
                    hideLoading()
                    pref.edit().clear().apply()
                    startActivity(Intent(this@ClientActivity, HomeActivity::class.java))
                    finish()
                    error.printStackTrace()
                }
            })
        }
    }

    private fun sdkRegister() {
        PRSCustomer.getInstance().start(this)
        ParousyaSAASSDK.getInstance().registerEventListener(prsEventListener)
        PRSCustomer.getInstance().getClientDetails(object : PRSCallback<UserDetails> {
            override fun onSuccess(result: UserDetails) {
                tv_client.text = String.format(Locale.getDefault(), "Customer ID: %s", result.genericId)
            }

            override fun onError(error: SAASException) {
            }

        })
        statusItemAdapter.addItem(Status(System.currentTimeMillis(), "Register Event Listener"))
    }

    private fun renderUI() {
        tv_tag.text = String.format(Locale.getDefault(), "Tag: %s", ParousyaSAASSDK.getInstance().getZoneName())

        statusItemAdapter = StatusItemAdapter()
        statusItemAdapter.set(ArrayList())
        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        rvStatus.layoutManager = layoutManager
        rvStatus.adapter = statusItemAdapter
    }

    private val prsEventListener = object : PRSEventListener {
        override fun onEvent(prsEvent: PRSEvent, data: Bundle) {
            if (isFinishing)
                return

            when (prsEvent) {
                PRSEvent.BEACON_RANGED -> {
                    tv_ranged_beacons.text = data.getString(ParousyaSAASSDK.BROADCAST_MESSAGE)
                    statusItemAdapter.addItem(Status(System.currentTimeMillis(), prsEvent.name))
                }
                PRSEvent.BEACON_PAIRED -> {
                    statusItemAdapter.addItem(Status(System.currentTimeMillis(), prsEvent.name + ": " + data.getString(ParousyaSAASSDK.BROADCAST_MESSAGE)))
                }
                PRSEvent.BEACON_UNPAIRED -> {
                    timer.cancel()
                    statusItemAdapter.addItem(Status(System.currentTimeMillis(), prsEvent.name + ": " + data.getString(ParousyaSAASSDK.BROADCAST_MESSAGE)))
                }
                PRSEvent.ZONE_PAIRING -> {
                    statusItemAdapter.addItem(Status(System.currentTimeMillis(), prsEvent.name + ": " + data.getString(ParousyaSAASSDK.BROADCAST_MESSAGE)))
                }
                PRSEvent.SESSION_STARTED, PRSEvent.SESSIONS_RESUMED -> {
                    var sessionStartedObject: SessionDetails? = null
                    (data.getParcelableArray(ParousyaSAASSDK.BROADCAST_DATA) as Array<SessionDetails>?)?.let {
                        sessionStartedObject = it.last()
                    }
                    (data.getParcelable(ParousyaSAASSDK.BROADCAST_DATA) as SessionDetails?)?.let {
                        sessionStartedObject = it
                    }
                    updateCurrentSessionDetailsFromEvent(prsEvent.name, data, sessionStartedObject)
                }
                PRSEvent.SESSION_ENDED_BY_HOST, PRSEvent.SESSION_ENDED_BY_CUSTOMER,
                PRSEvent.SESSION_ENDED_MANUALLY, PRSEvent.ALL_SESSIONS_ENDED_MANUALLY,
                PRSEvent.SESSION_ENDED_DUE_TO_RANGE -> {
                    var sessionEndedObject: SessionDetails? = null
                    (data.getParcelableArray(ParousyaSAASSDK.BROADCAST_DATA) as Array<SessionDetails>?)?.let {
                        sessionEndedObject = it.last()
                    }
                    (data.getParcelable(ParousyaSAASSDK.BROADCAST_DATA) as SessionDetails?)?.let { session ->
                        sessionEndedObject = session
                    }
                    updateCurrentSessionDetailsFromEvent(prsEvent.name, data, sessionEndedObject)
                }
                PRSEvent.SESSION_END_ERROR, PRSEvent.SESSION_NOT_FOUND -> {
                    timer.cancel()
                    statusItemAdapter.addItem(Status(System.currentTimeMillis(), prsEvent.name))
                }
                PRSEvent.SESSION_START_FAILED -> {
                    statusItemAdapter.addItem(Status(System.currentTimeMillis(), prsEvent.name + ": " + data.getString(ParousyaSAASSDK.BROADCAST_MESSAGE)))
                }
                else ->
                    statusItemAdapter.addItem(Status(System.currentTimeMillis(), prsEvent.name))
            }
        }

    }

    private fun updateCurrentSessionDetailsFromEvent(eventName: String, data: Bundle, sessionDetails: SessionDetails?) {
        viewSessionInfo.visibility = View.VISIBLE

        tvStartInfo.text = ""
        tvEndInfo.text = ""

        sessionDetails?.let { currentSession ->
            println("updateCurrentSessionDetailsFromEvent: $currentSession")
            val sessionStart = df.parse(currentSession.startedAt?.substring(0, 19))
            val startTimeInfo: String = dfDisplay.format(sessionStart.time)

            tvStartInfo.text = String.format("Start time: %s \nStart location: %s", startTimeInfo, currentSession.startLocation)

            try {
                val currentTimeInUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
                timer.cancel()
                timer = object : CountUpTimer(System.currentTimeMillis() + (60 * 60 * 24), (currentTimeInUTC - df.parse(currentSession.startedAt).time) / 1000) {
                    override fun onTicky(second: Long) {
                        tvTimeInfo.text = second.timeString()
                    }
                }
                timer.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            currentSession.endedAt?.let { endedAt ->
                if (endedAt.isNotEmpty()) {
                    timer.cancel()
                    val sessionEnd = df.parse(endedAt)
                    val endTimeInfo: String = dfDisplay.format(sessionEnd.time)
                    tvEndInfo.text = String.format("End time: %s \nEnd location: %s", endTimeInfo, currentSession.endLocation)
                } else {
                    tvEndInfo.text = ""
                }
            }
        }

        var text = String.format("%s: %s", eventName, data.getString(ParousyaSAASSDK.BROADCAST_MESSAGE))
        statusItemAdapter.addItem(Status(System.currentTimeMillis(), text))
    }
}
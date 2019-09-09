package com.parousya.android.sample

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.parousya.android.sample.common.*
import com.parousya.android.sdk.*
import com.parousya.android.sdk.events.ZonePairingRequestNotification
import com.parousya.android.sdk.exceptions.SAASException
import com.parousya.android.sdk.fcm.SAASNotificationsManager
import com.parousya.android.sdk.model.SessionDetails
import com.parousya.android.sdk.model.UserDetails
import kotlinx.android.synthetic.main.activity_host.*
import java.util.*
import kotlin.collections.ArrayList


class HostActivity : BaseActivity() {

    private lateinit var statusItemAdapter: StatusItemAdapter

    var timer: CountUpTimer = object : CountUpTimer(System.currentTimeMillis() + (60 * 60 * 24)) {
        override fun onTicky(second: Long) {
            tvTimeInfo.text = second.timeString()
        }
    }

    var activeSessions: ArrayList<SessionDetails> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Host"

        tv_ver.text = String.format(getString(R.string.lbl_version), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

        renderUI()
        sdkRegister()

        bt_sign_out.setOnClickListener {
            showLoading()
            PRSHost.getInstance().signOut(this, object : PRSCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    hideLoading()
                    pref.edit().clear().apply()
                    startActivity(Intent(this@HostActivity, HomeActivity::class.java))
                    finish()
                }

                override fun onError(error: SAASException) {
                    hideLoading()
                    Snackbar.make(container, error.localizedMessage, Snackbar.LENGTH_SHORT).show()
                    pref.edit().clear().apply()
                    startActivity(Intent(this@HostActivity, HomeActivity::class.java))
                    finish()
                }
            })
        }

        btnEndSession.setOnClickListener {
            PRSHost.getInstance().cancelCurrentSession()
        }

        if (intent != null && intent.hasExtra(SAASNotificationsManager.NOTIFICATION_PAYLOAD_EXTRA)) {
            Log.e("HostActivity", "hasExtra: ${SAASNotificationsManager.NOTIFICATION_PAYLOAD_EXTRA}")
            val notificationData = intent.getParcelableExtra(SAASNotificationsManager.NOTIFICATION_PAYLOAD_EXTRA) as Parcelable
            if (notificationData is ZonePairingRequestNotification) {
                Log.e("HostActivity", "showPairingRequest: ${notificationData.pairingId}")
                showPairingRequest(notificationData.beaconUUID, notificationData.pairingId)
            }
        } else {

        }
    }

    private fun sdkRegister() {
        ParousyaSAASSDK.getInstance().registerEventListener(prsEventListener)
        PRSHost.getInstance().start(this)
        PRSHost.getInstance().getHostDetails(object : PRSCallback<UserDetails> {
            override fun onSuccess(result: UserDetails) {
                tv_host.text = String.format(Locale.getDefault(), "Host ID: %s", result.genericId)
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

    private fun showPairingRequest(beaconUUID: String, pairingId: Long) {
        AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("Pairing request")
                .setMessage("${pairingId}")
                .setNeutralButton("Accept") { p0, p1 ->
                    p0.dismiss()
                    PRSHost.getInstance().acceptPairingRequest(beaconUUID = beaconUUID, pairingId = pairingId)
                }.show()
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
                PRSEvent.ZONE_ENTRY -> {
                    statusItemAdapter.addItem(Status(System.currentTimeMillis(), prsEvent.name))
                }
                PRSEvent.ZONE_EXIT -> {
                    tv_ranged_beacons.text = ""
                    statusItemAdapter.addItem(Status(System.currentTimeMillis(), prsEvent.name))
                }
                PRSEvent.ZONE_PAIRING -> {
                    statusItemAdapter.addItem(Status(System.currentTimeMillis(), prsEvent.name + ": " + data.getString(ParousyaSAASSDK.BROADCAST_MESSAGE)))
                }
                PRSEvent.SESSION_STARTED, PRSEvent.SESSIONS_RESUMED -> {
                    var sessionStartedObject: SessionDetails? = null
                    (data.getParcelableArray(ParousyaSAASSDK.BROADCAST_DATA) as Array<SessionDetails>?)?.let {
                        sessionStartedObject = it.last()
                        activeSessions.addAll(it)
                    }
                    (data.getParcelable(ParousyaSAASSDK.BROADCAST_DATA) as SessionDetails?)?.let {
                        sessionStartedObject = it
                        activeSessions.add(it)
                    }
                    updateCurrentSessionDetailsFromEvent(prsEvent.name, data, sessionStartedObject)
                }
                PRSEvent.SESSION_ENDED_BY_HOST, PRSEvent.SESSION_ENDED_BY_CUSTOMER,
                PRSEvent.SESSION_ENDED_MANUALLY, PRSEvent.ALL_SESSIONS_ENDED_MANUALLY,
                PRSEvent.SESSION_ENDED_DUE_TO_RANGE -> {
                    var sessionEndedObject: SessionDetails? = null
                    (data.getParcelableArray(ParousyaSAASSDK.BROADCAST_DATA) as Array<SessionDetails>?)?.let {
                        sessionEndedObject = it.last()
                        if (it.isEmpty()) {
                            activeSessions = arrayListOf()
                        } else {
                            for (sessionEnded in it) {
                                val index = activeSessions.indexOfFirst { it.id == sessionEnded.id }
                                if (index != -1) {
                                    activeSessions.removeAt(index)
                                }
                            }
                        }
                    }
                    (data.getParcelable(ParousyaSAASSDK.BROADCAST_DATA) as SessionDetails?)?.let { session ->
                        sessionEndedObject = session
                        val index = activeSessions.indexOfFirst { it.id == session.id }
                        if (index != -1) {
                            activeSessions.removeAt(index)
                        }
                    }
                    updateCurrentSessionDetailsFromEvent(prsEvent.name, data, sessionEndedObject)
                }
                PRSEvent.SESSION_END_ERROR, PRSEvent.SESSION_NOT_FOUND -> {
                    timer.cancel()
                    btnEndSession.visibility = View.GONE
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

        if (activeSessions.isEmpty()) {
            btnEndSession.visibility = View.GONE

            sessionDetails?.let { currentSession ->
                val sessionStart = df.parse(currentSession.startedAt)
                val startTimeInfo: String = dfDisplay.format(sessionStart.time)
                tvStartInfo.text = String.format("Start time: %s \nStart location: %s", startTimeInfo, currentSession.startLocation)

                timer.cancel()

                currentSession.endedAt?.let { endedAt ->
                    if (endedAt.isNotEmpty()) {
                        val sessionEnd = df.parse(endedAt)
                        val endTimeInfo: String = dfDisplay.format(sessionEnd.time)
                        tvEndInfo.text = String.format("End time: %s \nEnd location: %s", endTimeInfo, currentSession.endLocation)
                    } else {
                        tvEndInfo.text = ""
                    }
                }
            }

        } else {
            btnEndSession.visibility = View.VISIBLE

            activeSessions.last().let { currentSession ->
                val sessionStart = df.parse(currentSession.startedAt)
                val startTimeInfo: String = dfDisplay.format(sessionStart.time)
                tvStartInfo.text = String.format("Start time: %s \nStart location: %s", startTimeInfo, currentSession.startLocation)

                try {
                    val currentTimeInUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
                    timer.cancel()
                    timer = object : CountUpTimer(System.currentTimeMillis() + (60 * 60 * 24), (currentTimeInUTC - sessionStart.time) / 1000) {
                        override fun onTicky(second: Long) {
                            tvTimeInfo.text = second.timeString()
                        }
                    }
                    timer.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        var text = String.format("%s: %s", eventName, data.getString(ParousyaSAASSDK.BROADCAST_MESSAGE))
        statusItemAdapter.addItem(Status(System.currentTimeMillis(), text))
    }
}

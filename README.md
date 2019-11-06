# Parousya-SAAS-Android-Sample
Parousya SAAS SDK is used for Parousya Automatic Check-In System (ACIS) integration. Parousya ACIS will check in the user as soon as he/she enters the place of business, among other use cases.

## Overview
Parousya SAAS SDK is a framework for Parousya ACIS integration.  This repository contains the sample project implementation on how to use the Android SDK.

## Installation 
Parousya SAAS SDK is distributed as a compiled bundle, and can be easily integrated into a new app or an existing codebase with standard tooling.

```ruby
implementation 'com.parousya.saas:sdk:0.0.8'
```

### Requirements

The Android SDK requires Android API Level >= 21. The Android SDK version requirements for each release are tightly coupled.

## Initializing
To initialize Parousya SAAS SDK, you will need to obtain the `CLIENT_ID` and `CLIENT_SECRET` values from Parousya. Please [contact us](https://www.parousya.com/contact) for access.

`this` which is the first parameter is the application context.

You will need to choose between `Variant.TESTING` and `Variant.PRODUCTION` depending on the environment to use. Please use `Variant.TESTING` for non production app.

```kotlin
class SAASApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ParousyaSAASSDK.init(this, CLIENT_ID, CLIENT_SECRET, Variant.TESTING)
    }
}
```

You will have 2 choice in starting Parousya SAAS client:
1. As a Host
2. As a Customer

## Starting as a Host
`this` is the activity object where the call is made.

```kotlin
   PRSHost.getInstance().signIn(this, userId, "",
                                        object : PRSCallback<UserDetails> {
                                            override
                                            fun onSuccess(result: UserDetails) {
                                                // Sign In Successful
                                            }

                                            override
                                            fun onError(error: SAASException) {
                                                // Sign In Failed
                                            }
                                        }
                                )
```
You will need to register for the broadcast events that you want to receive from the SDK.

This is an example on how you should register for intents: `this` is the context

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

		ParousyaSAASSDK.getInstance().registerEventListener(prsEventListener)
		
		// Starting the SDK as a Host.
		PRSHost.getInstance().start(this)
}
```
The SDK will automatically range for beacons (when starting as a Host or Customer) and will associate with any detected Parousya beacon.

This is an example on how we can listen to Parousya SAAS events:
```kotlin
    private val prsEventListener = object : PRSEventListener {
        override fun onEvent(prsEvent: PRSEvent, data: Bundle) {
            if (isFinishing)
                return

            when (prsEvent) {
                PRSEvent.BEACON_RANGED -> {
                }
                PRSEvent.BEACON_PAIRED -> {
                }
                PRSEvent.BEACON_UNPAIRED -> {
                }
                PRSEvent.ZONE_ENTRY -> {
                }
                PRSEvent.ZONE_EXIT -> {
                }
                PRSEvent.ZONE_PAIRING -> {
                }
                PRSEvent.SESSION_STARTED, PRSEvent.SESSIONS_RESUMED -> {
                }
                PRSEvent.SESSION_ENDED_BY_HOST, PRSEvent.SESSION_ENDED_BY_CUSTOMER,
                PRSEvent.SESSION_ENDED_MANUALLY, PRSEvent.ALL_SESSIONS_ENDED_MANUALLY,
                PRSEvent.SESSION_ENDED_DUE_TO_RANGE -> {
                }
                PRSEvent.SESSION_END_ERROR, PRSEvent.SESSION_NOT_FOUND -> {
                }
                PRSEvent.SESSION_START_FAILED -> {
                }
                else -> {
					// You might want to handle other intent action
				}
            }
        }
    }
```

### Ending Session
```kotlin
PRSHost.getInstance().cancelCurrentSession()
```

### Host Logout
```kotlin
PRSHost.getInstance().signOut(this, object : PRSCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
					// successful signout
                }

                override fun onError(error: SAASException) {
					// signout failed
                }
            })
```        
## Starting as a Customer

`this` is the activity object where the call is made.

```kotlin
PRSCustomer.getInstance().signIn(this, userId,
                                        object : PRSCallback<String> {
                                            override
                                            fun onSuccess(result: String) {
                                                // Sign In Successful
                                            }

                                            override
                                            fun onError(error: SAASException) {
                                                // Sign In Failed
                                            }
                                        }
                                )
```
This is an example on how you should register for intents: `this` is the context

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

		ParousyaSAASSDK.getInstance().registerEventListener(prsEventListener)
		
		// Starting the SDK as a Customer.
		PRSCustomer.getInstance().start(this)
}
```

### Customer Logout
```kotlin
PRSCustomer.getInstance().signOut(this, object : PRSCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                }

                override fun onError(error: SAASException) {
                }
            })
```


## Push Notification
Please implement the the following method for [push notifications](https://firebase.google.com/docs/cloud-messaging/android/client "push notifications").  Parousya SAAS SDK will only handle push notifications that comes for the SDK ignoring the rest. Any payload received that contains the `parousya` key should be sent to the SDK for handling.

```kotlin
class AppMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String?) {
        token?.let {
            ParousyaSAASSDK.getInstance().registerPushToken(it)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        ParousyaSAASSDK.getInstance().processPushPayload(this, remoteMessage.data, object :
            PRSNotificationListener {

            override fun onPRSNotify(data: SAASNotificationData) {
                if (baseContext.isInBackground()) {
                    sendNotification(this@AppMessagingService, data)
                }
            }
        })
    }
}
```

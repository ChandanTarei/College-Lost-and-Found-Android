
package com.example.collegelostandfound

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object ContactNotificationMonitor {

    private const val DATABASE_URL =
        "https://collegelostandfound-2007-default-rtdb.asia-southeast1.firebasedatabase.app"

    private const val PREFS_NAME = "contact_notification_history"

    private val auth = FirebaseAuth.getInstance()

    private val database by lazy {
        FirebaseDatabase.getInstance(DATABASE_URL)
    }

    private var appContext: Context? = null

    private var authListener: FirebaseAuth.AuthStateListener? = null

    private var activeUserId: String? = null

    private var itemsRef: DatabaseReference? = null
    private var itemsListener: ValueEventListener? = null

    private data class Watcher(
        val reference: DatabaseReference,
        val listener: ValueEventListener
    )

    private val watchers = mutableMapOf<String, Watcher>()

    fun start(context: Context) {

        appContext = context.applicationContext

        // Avoid registering duplicate listeners.
        if (authListener != null) {
            return
        }

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->

            val uid = firebaseAuth.currentUser?.uid

            if (uid != activeUserId) {

                removeDatabaseListeners()

                activeUserId = uid

                if (uid != null) {
                    observeItems(uid)
                }
            }
        }

        authListener = listener

        auth.addAuthStateListener(listener)
    }

    private fun observeItems(uid: String) {

        val reference = database.getReference("items")

        val listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (activeUserId != uid) {
                    return
                }

                // Keep track of the listeners still needed.
                val requiredKeys = mutableSetOf<String>()

                for (child in snapshot.children) {

                    val reportId = child.key ?: continue

                    val ownerId = child.child("userId")
                        .getValue(String::class.java)
                        ?: continue

                    val itemName = child.child("itemName")
                        .getValue(String::class.java)
                        ?: "Item"

                    if (ownerId == uid) {

                        // User owns this item.
                        // Watch requests sent by other students.
                        val key = "owner/$reportId"

                        requiredKeys.add(key)

                        if (!watchers.containsKey(key)) {
                            observeOwnerRequests(
                                uid,
                                reportId,
                                itemName,
                                key
                            )
                        }

                    } else {

                        // User does not own this item.
                        // Watch their own contact request.
                        val key = "requester/$reportId"

                        requiredKeys.add(key)

                        if (!watchers.containsKey(key)) {
                            observeRequesterStatus(
                                uid,
                                reportId,
                                itemName,
                                key
                            )
                        }
                    }
                }

                // Remove listeners for deleted or removed items.
                val obsoleteKeys = watchers.keys
                    .filter { it !in requiredKeys }

                for (key in obsoleteKeys) {
                    removeWatcher(key)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // The monitor does not interrupt normal app usage.
            }
        }

        itemsRef = reference
        itemsListener = listener

        reference.addValueEventListener(listener)
    }

    private fun observeOwnerRequests(
        uid: String,
        reportId: String,
        itemName: String,
        key: String
    ) {

        val reference = database
            .getReference("contactRequests")
            .child(reportId)

        val listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (activeUserId != uid) {
                    return
                }

                for (child in snapshot.children) {

                    val requesterId = child.key ?: continue

                    val status = child.child("status")
                        .getValue(String::class.java)
                        ?: continue

                    if (status != "Pending") {
                        continue
                    }

                    val eventKey =
                        "new_request|$uid|$reportId|$requesterId"

                    notifyOnce(
                        eventKey = eventKey,
                        title = "New Contact Request",
                        message = "Someone requested contact for $itemName",
                        reportId = reportId
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignore unavailable reports.
            }
        }

        watchers[key] = Watcher(reference, listener)

        reference.addValueEventListener(listener)
    }

    private fun observeRequesterStatus(
        uid: String,
        reportId: String,
        itemName: String,
        key: String
    ) {

        val reference = database
            .getReference("contactRequests")
            .child(reportId)
            .child(uid)

        val listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (activeUserId != uid) {
                    return
                }

                if (!snapshot.exists()) {
                    return
                }

                val status = snapshot.child("status")
                    .getValue(String::class.java)
                    ?: return

                when (status) {

                    "Approved" -> {

                        val eventKey =
                            "approved|$uid|$reportId"

                        notifyOnce(
                            eventKey = eventKey,
                            title = "Contact Request Approved",
                            message = "Your request for $itemName was approved",
                            reportId = reportId
                        )
                    }

                    "Rejected" -> {

                        val eventKey =
                            "rejected|$uid|$reportId"

                        notifyOnce(
                            eventKey = eventKey,
                            title = "Contact Request Rejected",
                            message = "Your request for $itemName was rejected",
                            reportId = reportId
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignore unavailable requests.
            }
        }

        watchers[key] = Watcher(reference, listener)

        reference.addValueEventListener(listener)
    }

    private fun notifyOnce(
        eventKey: String,
        title: String,
        message: String,
        reportId: String
    ) {

        val context = appContext ?: return

        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        // Do not notify again for the same event.
        if (preferences.getBoolean(eventKey, false)) {
            return
        }

        val shown = NotificationHelper.showNotification(
            context = context,
            title = title,
            message = message,
            reportId = reportId
        )

        if (shown) {

            preferences.edit()
                .putBoolean(eventKey, true)
                .apply()
        }
    }

    private fun removeWatcher(key: String) {

        val watcher = watchers.remove(key) ?: return

        watcher.reference.removeEventListener(
            watcher.listener
        )
    }

    private fun removeDatabaseListeners() {

        val oldRef = itemsRef
        val oldListener = itemsListener

        if (oldRef != null && oldListener != null) {
            oldRef.removeEventListener(oldListener)
        }

        itemsRef = null
        itemsListener = null

        val keys = watchers.keys.toList()

        for (key in keys) {
            removeWatcher(key)
        }
    }

    fun stop() {

        removeDatabaseListeners()

        authListener?.let {
            auth.removeAuthStateListener(it)
        }

        authListener = null
        activeUserId = null
        appContext = null
    }
}

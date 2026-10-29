
package com.example.collegelostandfound

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object NotificationHelper {

    private const val CHANNEL_ID = "contact_requests"
    private const val CHANNEL_NAME = "Contact Requests"

    private const val PERMISSION_REQUEST_CODE = 2001

    fun requestNotificationPermission(activity: Activity) {

        if (Build.VERSION.SDK_INT >= 33) {

            val permissionGranted =
                activity.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                activity.requestPermissions(
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        reportId: String
    ): Boolean {

        if (Build.VERSION.SDK_INT >= 33) {

            val permissionGranted =
                context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                return false
            }
        }

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Lost and Found contact request updates"
            enableVibration(true)
        }

        manager.createNotificationChannel(channel)

        val intent = Intent(
            context,
            ItemDetailsActivity::class.java
        ).apply {
            putExtra("reportId", reportId)

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val notificationId =
            (System.currentTimeMillis() and 0x7FFFFFFF)
                .toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_dialog_info
            )
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                Notification.BigTextStyle()
                    .bigText(message)
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(
                Notification.VISIBILITY_PRIVATE
            )
            .build()

        return try {

            manager.notify(
                notificationId,
                notification
            )

            true

        } catch (e: SecurityException) {

            false
        }
    }
}

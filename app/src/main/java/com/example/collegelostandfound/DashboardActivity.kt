
package com.example.collegelostandfound

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dashboard layout
        setContentView(R.layout.activity_dashboard)

        // Apply modern UI styling
        UiPolish.apply(this)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Check whether the user is logged in
        if (auth.currentUser == null) {
            goToLogin()
            return
        }

        // Keep the existing notification system working
        NotificationHelper.requestNotificationPermission(this)
        ContactNotificationMonitor.start(this)

        // Initialize Dashboard buttons
        val btnMyReports =
            findViewById<Button>(R.id.btnMyReports)

        val btnLostItems =
            findViewById<Button>(R.id.btnLostItems)

        val btnFoundItems =
            findViewById<Button>(R.id.btnFoundItems)

        val btnReportItem =
            findViewById<Button>(R.id.btnReportItem)

        val btnProfile =
            findViewById<Button>(R.id.btnProfile)

        val btnLogout =
            findViewById<Button>(R.id.btnLogout)

        // RED: Lost Items and Logout
        val redColor = ColorStateList.valueOf(
            Color.parseColor("#D32F2F")
        )

        btnLostItems.backgroundTintList = redColor
        btnLogout.backgroundTintList = redColor

        btnLostItems.setTextColor(Color.WHITE)
        btnLogout.setTextColor(Color.WHITE)

        // GREEN: Found Items
        val greenColor = ColorStateList.valueOf(
            Color.parseColor("#00897B")
        )

        btnFoundItems.backgroundTintList = greenColor
        btnFoundItems.setTextColor(Color.WHITE)

        // My Reports
        btnMyReports.setOnClickListener {
            val intent = Intent(
                this,
                MyReportsActivity::class.java
            )
            startActivity(intent)
        }

        // Lost Items
        btnLostItems.setOnClickListener {
            val intent = Intent(
                this,
                LostItemsActivity::class.java
            )
            startActivity(intent)
        }

        // Found Items
        btnFoundItems.setOnClickListener {
            val intent = Intent(
                this,
                FoundItemsActivity::class.java
            )
            startActivity(intent)
        }

        // Report Lost / Found Item
        btnReportItem.setOnClickListener {
            val intent = Intent(
                this,
                ReportItemActivity::class.java
            )
            startActivity(intent)
        }

        // My Profile
        btnProfile.setOnClickListener {
            val intent = Intent(
                this,
                ProfileActivity::class.java
            )
            startActivity(intent)
        }

        // Logout
        btnLogout.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}

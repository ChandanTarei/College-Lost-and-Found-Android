
package com.example.collegelostandfound

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileUserId: TextView
    private lateinit var etProfilePhone: EditText
    private lateinit var btnSavePhone: Button
    private lateinit var btnProfileBack: Button

    private val logTag = "ProfileFirebase"

    // Explicitly use the project's Realtime Database.
    private val databaseUrl =
        "https://collegelostandfound-2007-default-rtdb.asia-southeast1.firebasedatabase.app"

    private lateinit var database: FirebaseDatabase
    private var phoneRef: DatabaseReference? = null

    private var connectionListener: ValueEventListener? = null
    private var connectionRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)

        // Preserve modern styling and camera-cutout fix.
        UiPolish.apply(this)
        ScreenInsets.apply(this)

        // Initialize views.
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfileUserId = findViewById(R.id.tvProfileUserId)
        etProfilePhone = findViewById(R.id.etProfilePhone)
        btnSavePhone = findViewById(R.id.btnSavePhone)
        btnProfileBack = findViewById(R.id.btnProfileBack)

        // Connect to the correct Firebase database.
        database = FirebaseDatabase.getInstance(databaseUrl)

        // Check the Firebase connection.
        checkFirebaseConnection()

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {

            tvProfileName.text =
                user.displayName?.takeIf { it.isNotBlank() }
                    ?: "Student"

            tvProfileEmail.text = user.email ?: "No Email"
            tvProfileUserId.text = user.uid

            phoneRef = database
                .getReference("profiles")
                .child(user.uid)
                .child("phoneNumber")

            Log.d(
                logTag,
                "Phone reference: ${phoneRef?.toString()}"
            )

            // Load a previously saved phone number.
            loadPhoneNumber()

            // Save or update the phone number.
            btnSavePhone.setOnClickListener {
                savePhoneNumber()
            }

        } else {

            tvProfileName.text = "Guest"
            tvProfileEmail.text = "Not Logged In"
            tvProfileUserId.text = "N/A"

            etProfilePhone.isEnabled = false
            btnSavePhone.isEnabled = false

            Toast.makeText(
                this,
                "Please log in to manage your profile",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Return to Dashboard.
        btnProfileBack.setOnClickListener {
            finish()
        }
    }

    private fun checkFirebaseConnection() {

        connectionRef = database.getReference(".info/connected")

        connectionListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val connected =
                    snapshot.getValue(Boolean::class.java) ?: false

                Log.d(
                    logTag,
                    "Firebase connected: $connected"
                )
            }

            override fun onCancelled(error: DatabaseError) {

                Log.e(
                    logTag,
                    "Connection check failed: ${error.message}"
                )
            }
        }

        connectionRef?.addValueEventListener(connectionListener!!)
    }

    private fun loadPhoneNumber() {

        val reference = phoneRef ?: return

        reference.get()
            .addOnSuccessListener { snapshot ->

                val savedPhone =
                    snapshot.getValue(String::class.java)

                if (!savedPhone.isNullOrBlank()) {
                    etProfilePhone.setText(savedPhone)
                }

                Log.d(
                    logTag,
                    "Phone number read completed. Exists: ${snapshot.exists()}"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    logTag,
                    "Failed to load phone number",
                    error
                )

                Toast.makeText(
                    this,
                    "Could not load phone number",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun savePhoneNumber() {

        val reference = phoneRef ?: run {

            Toast.makeText(
                this,
                "Please log in first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val phone = etProfilePhone.text
            .toString()
            .trim()

        // Validate a 10-digit phone number.
        if (!phone.matches(Regex("[0-9]{10}"))) {

            etProfilePhone.error =
                "Enter a valid 10-digit mobile number"

            etProfilePhone.requestFocus()
            return
        }

        // Prevent duplicate submissions.
        btnSavePhone.isEnabled = false
        btnSavePhone.text = "Saving..."

        Log.d(
            logTag,
            "Starting phone-number write"
        )

        Log.d(
            logTag,
            "Writing to: ${reference}"
        )

        // The Firebase write is asynchronous.
        // Its completion callback confirms a server result.
        reference.setValue(phone)
            .addOnCompleteListener { task ->

                Log.d(
                    logTag,
                    "Write completed: ${task.isComplete}, " +
                            "successful: ${task.isSuccessful}"
                )

                if (task.isSuccessful) {

                    btnSavePhone.isEnabled = true
                    btnSavePhone.text = "Save Phone Number"

                    Toast.makeText(
                        this,
                        "Phone number saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d(
                        logTag,
                        "Phone number successfully committed"
                    )

                } else {

                    btnSavePhone.isEnabled = true
                    btnSavePhone.text = "Save Phone Number"

                    Log.e(
                        logTag,
                        "Phone number write failed",
                        task.exception
                    )

                    Toast.makeText(
                        this,
                        "Failed to save: " +
                                (task.exception?.localizedMessage
                                    ?: "Unknown Firebase error"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onDestroy() {

        // Remove the connection listener to prevent leaks.
        val reference = connectionRef
        val listener = connectionListener

        if (reference != null && listener != null) {
            reference.removeEventListener(listener)
        }

        super.onDestroy()
    }
}

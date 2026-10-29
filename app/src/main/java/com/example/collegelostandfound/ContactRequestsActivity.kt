
package com.example.collegelostandfound

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ContactRequestsActivity : AppCompatActivity() {

    private val databaseUrl =
        "https://collegelostandfound-2007-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val database by lazy {
        FirebaseDatabase.getInstance(databaseUrl)
    }

    private lateinit var requestsContainer: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var tvItemName: TextView

    private lateinit var requestsRef: DatabaseReference

    private var requestsListener: ValueEventListener? = null

    private var reportId = ""

    private val primaryColor =
        Color.parseColor("#3048A5")

    private val approveColor =
        Color.parseColor("#008577")

    private val rejectColor =
        Color.parseColor("#C62828")

    private val textColor =
        Color.parseColor("#202A44")

    private val mutedColor =
        Color.parseColor("#667085")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Existing Contact Requests layout
        setContentView(R.layout.activity_contact_requests)

        // Step 21: Modern UI styling
        UiPolish.apply(this)

        requestsContainer =
            findViewById(R.id.requestsContainer)

        tvEmpty =
            findViewById(R.id.tvRequestsEmpty)

        tvItemName =
            findViewById(R.id.tvRequestsItemName)

        val btnBack =
            findViewById<Button>(R.id.btnBackRequests)

        btnBack.setOnClickListener {
            finish()
        }

        reportId =
            intent.getStringExtra("reportId").orEmpty()

        val uid =
            FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null || reportId.isBlank()) {

            showMessage("Invalid session or report")

            finish()
            return
        }

        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = "Loading contact requests..."

        // Verify that the logged-in student owns this report
        database.getReference("items")
            .child(reportId)
            .get()
            .addOnSuccessListener { snapshot ->

                val ownerId =
                    snapshot.child("userId")
                        .getValue(String::class.java)

                if (ownerId != uid) {

                    showMessage(
                        "Only the report owner can view requests"
                    )

                    finish()
                    return@addOnSuccessListener
                }

                tvItemName.text =
                    snapshot.child("itemName")
                        .getValue(String::class.java)
                        ?: "Item"

                listenForRequests()
            }
            .addOnFailureListener { error ->

                showMessage(
                    "Unable to load report: ${error.message}"
                )

                finish()
            }
    }

    private fun listenForRequests() {

        requestsRef = database
            .getReference("contactRequests")
            .child(reportId)

        requestsListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                // Remove old request cards
                requestsContainer.removeAllViews()

                var count = 0

                for (child in snapshot.children) {

                    val request =
                        child.getValue(
                            ContactRequest::class.java
                        ) ?: continue

                    // Use the real Firebase request key
                    request.requesterId =
                        child.key ?: continue

                    addRequestCard(request)

                    count++
                }

                if (count == 0) {

                    tvEmpty.visibility = View.VISIBLE

                    tvEmpty.text =
                        "No contact requests yet.\n" +
                                "Requests from other students " +
                                "will appear here."

                } else {

                    tvEmpty.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {

                tvEmpty.visibility = View.VISIBLE

                tvEmpty.text =
                    "Unable to load requests: ${error.message}"
            }
        }

        requestsRef.addValueEventListener(
            requestsListener!!
        )
    }

    private fun addRequestCard(request: ContactRequest) {

        // Modern rounded request card
        val card = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(18),
                dp(16),
                dp(18),
                dp(16)
            )

            setBackgroundResource(
                R.drawable.bg_lf_card
            )

            elevation = dp(3).toFloat()
        }

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {

            bottomMargin = dp(14)
        }

        // Card heading
        val heading = TextView(this).apply {

            text = "Student Contact Request"

            textSize = 18f

            setTextColor(primaryColor)

            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )

            setPadding(
                0,
                0,
                0,
                dp(10)
            )
        }

        // Requesting student UID
        val studentText = TextView(this).apply {

            text = "Student ID: ${request.requesterId}"

            textSize = 14f

            setTextColor(mutedColor)

            setPadding(
                0,
                0,
                0,
                dp(12)
            )
        }

        // Request status
        val statusText = TextView(this).apply {

            text = "Status: ${request.status}"

            textSize = 16f

            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )

            setTextColor(
                when (request.status) {

                    "Approved" ->
                        Color.parseColor("#2E7D32")

                    "Rejected" ->
                        rejectColor

                    else ->
                        Color.parseColor("#F57C00")
                }
            )

            setPadding(
                0,
                0,
                0,
                dp(12)
            )
        }

        card.addView(heading)
        card.addView(studentText)
        card.addView(statusText)

        // Show buttons only while request is pending
        if (request.status == "Pending") {

            val buttonRow = LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL
            }

            val approveButton = Button(this).apply {

                text = "Approve"
                isAllCaps = false

                setTextColor(Color.WHITE)

                backgroundTintList =
                    ColorStateList.valueOf(approveColor)
            }

            val rejectButton = Button(this).apply {

                text = "Reject"
                isAllCaps = false

                setTextColor(Color.WHITE)

                backgroundTintList =
                    ColorStateList.valueOf(rejectColor)
            }

            val approveParams =
                LinearLayout.LayoutParams(
                    0,
                    dp(52),
                    1f
                ).apply {
                    rightMargin = dp(6)
                }

            val rejectParams =
                LinearLayout.LayoutParams(
                    0,
                    dp(52),
                    1f
                ).apply {
                    leftMargin = dp(6)
                }

            approveButton.setOnClickListener {

                confirmDecision(
                    request,
                    "Approved"
                )
            }

            rejectButton.setOnClickListener {

                confirmDecision(
                    request,
                    "Rejected"
                )
            }

            buttonRow.addView(
                approveButton,
                approveParams
            )

            buttonRow.addView(
                rejectButton,
                rejectParams
            )

            card.addView(buttonRow)
        }

        requestsContainer.addView(
            card,
            cardParams
        )
    }

    private fun confirmDecision(
        request: ContactRequest,
        decision: String
    ) {

        AlertDialog.Builder(this)
            .setTitle("$decision Contact Request")
            .setMessage(
                "Are you sure you want to " +
                        "${decision.lowercase()} this request?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->

                updateRequestStatus(
                    request,
                    decision
                )
            }
            .show()
    }

    private fun updateRequestStatus(
        request: ContactRequest,
        decision: String
    ) {

        val uid =
            FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null || uid != request.ownerId) {

            showMessage(
                "Only the report owner can update requests"
            )

            return
        }

        if (
            decision != "Approved" &&
            decision != "Rejected"
        ) {

            showMessage("Invalid request decision")
            return
        }

        val requestRef = database
            .getReference("contactRequests")
            .child(reportId)
            .child(request.requesterId)

        // Get latest request state before updating
        requestRef.get()
            .addOnSuccessListener { snapshot ->

                val latestRequest =
                    snapshot.getValue(
                        ContactRequest::class.java
                    )

                if (
                    latestRequest == null ||
                    latestRequest.status != "Pending"
                ) {

                    showMessage(
                        "This request is no longer pending"
                    )

                    return@addOnSuccessListener
                }

                if (
                    latestRequest.ownerId != uid ||
                    latestRequest.reportId != reportId ||
                    latestRequest.requesterId != request.requesterId
                ) {

                    showMessage("Request information mismatch")
                    return@addOnSuccessListener
                }

                // IMPORTANT:
                // Update the entire request object.
                // This matches the Step 19 Firebase rules.
                val updatedRequest =
                    latestRequest.copy(
                        status = decision
                    )

                requestRef.setValue(updatedRequest)
                    .addOnSuccessListener {

                        showMessage(
                            "Contact Request $decision"
                        )
                    }
                    .addOnFailureListener { error ->

                        showMessage(
                            error.message
                                ?: "Failed to update request"
                        )
                    }
            }
            .addOnFailureListener { error ->

                showMessage(
                    error.message
                        ?: "Unable to check request"
                )
            }
    }

    private fun dp(value: Int): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()
    }

    private fun showMessage(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {

        if (::requestsRef.isInitialized) {

            requestsListener?.let { listener ->

                requestsRef.removeEventListener(
                    listener
                )
            }
        }

        super.onDestroy()
    }
}

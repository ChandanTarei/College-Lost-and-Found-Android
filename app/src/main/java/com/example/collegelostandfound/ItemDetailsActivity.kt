
package com.example.collegelostandfound

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

import io.github.jan.supabase.storage.storage

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ItemDetailsActivity : AppCompatActivity() {

    private val databaseUrl =
        "https://collegelostandfound-2007-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val database by lazy {
        FirebaseDatabase.getInstance(databaseUrl)
    }

    private lateinit var ivDetailPhoto: ImageView
    private lateinit var tvNoPhoto: TextView

    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailType: TextView
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailLocation: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailDescription: TextView
    private lateinit var tvDetailStatus: TextView

    private lateinit var tvContactRequestStatus: TextView
    private lateinit var tvApprovedContactNumber: TextView

    private lateinit var btnRequestContact: Button
    private lateinit var btnManageRequests: Button
    private lateinit var btnBackDetails: Button

    private var reportId = ""
    private var currentItem: LostItem? = null

    // Prevent older contact responses from changing a newer state
    private var contactCheckVersion = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_item_details)

        UiPolish.apply(this)
        ScreenInsets.apply(this)

        ivDetailPhoto = findViewById(R.id.ivDetailPhoto)
        tvNoPhoto = findViewById(R.id.tvNoPhoto)

        tvDetailName = findViewById(R.id.tvDetailName)
        tvDetailType = findViewById(R.id.tvDetailType)
        tvDetailCategory = findViewById(R.id.tvDetailCategory)
        tvDetailLocation = findViewById(R.id.tvDetailLocation)
        tvDetailDate = findViewById(R.id.tvDetailDate)
        tvDetailDescription =
            findViewById(R.id.tvDetailDescription)
        tvDetailStatus = findViewById(R.id.tvDetailStatus)

        tvContactRequestStatus =
            findViewById(R.id.tvContactRequestStatus)

        tvApprovedContactNumber =
            findViewById(R.id.tvApprovedContactNumber)

        btnRequestContact =
            findViewById(R.id.btnRequestContact)

        btnManageRequests =
            findViewById(R.id.btnManageRequests)

        btnBackDetails =
            findViewById(R.id.btnBackDetails)

        btnBackDetails.setOnClickListener {
            finish()
        }

        reportId =
            intent.getStringExtra("reportId").orEmpty()

        if (reportId.isBlank()) {
            showToast("Invalid report ID")
            finish()
            return
        }

        if (FirebaseAuth.getInstance().currentUser == null) {
            showToast("Please login first")
            finish()
            return
        }

        btnRequestContact.setOnClickListener {
            currentItem?.let { item ->
                showRequestConfirmation(item)
            }
        }

        btnManageRequests.setOnClickListener {
            val intent = Intent(
                this,
                ContactRequestsActivity::class.java
            )

            intent.putExtra("reportId", reportId)
            startActivity(intent)
        }

        loadItemDetails()
    }

    private fun loadItemDetails() {

        database.getReference("items")
            .child(reportId)
            .get()
            .addOnSuccessListener { snapshot ->

                val item =
                    snapshot.getValue(LostItem::class.java)

                if (item == null) {
                    showToast("Item not found")
                    finish()
                    return@addOnSuccessListener
                }

                currentItem = item

                tvDetailName.text = item.itemName
                tvDetailType.text = "Type: ${item.type}"
                tvDetailCategory.text =
                    "Category: ${item.category}"

                tvDetailLocation.text =
                    "Location: ${item.location}"

                tvDetailDate.text =
                    "Date: ${item.date}"

                tvDetailDescription.text =
                    "Description: ${item.description}"

                tvDetailStatus.text =
                    "Status: ${item.status}"

                val completed =
                    item.status == "Recovered" ||
                            item.status == "Returned"

                tvDetailStatus.setTextColor(
                    Color.parseColor(
                        if (completed) "#2E7D32"
                        else "#F57C00"
                    )
                )

                loadItemPhoto(item.photoPath)
                setupContactRequest(item)
            }
            .addOnFailureListener { error ->
                showToast(
                    "Failed to load item: ${error.message}"
                )
            }
    }

    private fun setupContactRequest(item: LostItem) {

        val currentUid =
            FirebaseAuth.getInstance().currentUser?.uid
                ?: return

        contactCheckVersion++
        val thisCheck = contactCheckVersion

        btnRequestContact.visibility = View.GONE
        btnManageRequests.visibility = View.GONE
        tvContactRequestStatus.visibility = View.GONE

        // Never leave a previous contact number visible
        tvApprovedContactNumber.visibility = View.GONE
        tvApprovedContactNumber.text = ""

        // Report owner can manage requests
        if (currentUid == item.userId) {
            btnManageRequests.visibility = View.VISIBLE
            return
        }

        tvContactRequestStatus.visibility = View.VISIBLE
        tvContactRequestStatus.text =
            "Checking contact request..."

        val requestRef = database
            .getReference("contactRequests")
            .child(reportId)
            .child(currentUid)

        requestRef.get()
            .addOnSuccessListener { snapshot ->

                if (thisCheck != contactCheckVersion) {
                    return@addOnSuccessListener
                }

                if (!snapshot.exists()) {

                    tvContactRequestStatus.visibility =
                        View.GONE

                    btnRequestContact.visibility =
                        View.VISIBLE

                    btnRequestContact.isEnabled = true
                    return@addOnSuccessListener
                }

                val status =
                    snapshot.child("status")
                        .getValue(String::class.java)
                        ?: "Pending"

                btnRequestContact.visibility = View.GONE
                tvContactRequestStatus.visibility = View.VISIBLE

                when (status) {

                    "Pending" -> {
                        tvContactRequestStatus.text =
                            "Contact Request: Pending"

                        tvContactRequestStatus.setTextColor(
                            Color.parseColor("#F57C00")
                        )
                    }

                    "Approved" -> {
                        tvContactRequestStatus.text =
                            "Contact Request: Approved"

                        tvContactRequestStatus.setTextColor(
                            Color.parseColor("#2E7D32")
                        )

                        loadApprovedContactNumber(
                            ownerId = item.userId,
                            checkVersion = thisCheck
                        )
                    }

                    "Rejected" -> {
                        tvContactRequestStatus.text =
                            "Contact Request: Rejected"

                        tvContactRequestStatus.setTextColor(
                            Color.RED
                        )
                    }

                    else -> {
                        tvContactRequestStatus.text =
                            "Contact Request: $status"
                    }
                }
            }
            .addOnFailureListener { error ->

                if (thisCheck != contactCheckVersion) {
                    return@addOnFailureListener
                }

                tvContactRequestStatus.visibility =
                    View.VISIBLE

                tvContactRequestStatus.text =
                    "Unable to check request: ${error.message}"
            }
    }

    // Only called after the request status is Approved
    private fun loadApprovedContactNumber(
        ownerId: String,
        checkVersion: Int
    ) {

        tvApprovedContactNumber.visibility = View.VISIBLE
        tvApprovedContactNumber.text =
            "Loading owner's contact number..."

        val contactRef = database
            .getReference("privateContacts")
            .child(ownerId)
            .child(reportId)
            .child("contactNumber")

        contactRef.get()
            .addOnSuccessListener { snapshot ->

                if (checkVersion != contactCheckVersion) {
                    return@addOnSuccessListener
                }

                val phoneNumber =
                    snapshot.getValue(String::class.java)

                tvApprovedContactNumber.text =
                    if (phoneNumber.isNullOrBlank()) {
                        "No contact number provided for this report"
                    } else {
                        "Owner's Contact Number\n$phoneNumber"
                    }
            }
            .addOnFailureListener { error ->

                if (checkVersion != contactCheckVersion) {
                    return@addOnFailureListener
                }

                tvApprovedContactNumber.text =
                    "Unable to load contact number: ${error.message}"
            }
    }

    private fun showRequestConfirmation(item: LostItem) {

        AlertDialog.Builder(this)
            .setTitle("Request Contact")
            .setMessage(
                "Do you want to send a contact request for ${item.itemName}?"
            )
            .setPositiveButton("Send Request") { _, _ ->
                sendContactRequest(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendContactRequest(item: LostItem) {

        val currentUid =
            FirebaseAuth.getInstance().currentUser?.uid
                ?: return

        if (currentUid == item.userId) {
            showToast(
                "You cannot request contact for your own item"
            )
            return
        }

        btnRequestContact.isEnabled = false

        val contactRequest = ContactRequest(
            reportId = reportId,
            requesterId = currentUid,
            ownerId = item.userId,
            itemName = item.itemName,
            status = "Pending"
        )

        val requestRef = database
            .getReference("contactRequests")
            .child(reportId)
            .child(currentUid)

        requestRef.setValue(contactRequest)
            .addOnSuccessListener {

                showToast(
                    "Contact request sent successfully"
                )

                btnRequestContact.visibility = View.GONE
                tvContactRequestStatus.visibility = View.VISIBLE
                tvContactRequestStatus.text =
                    "Contact Request: Pending"

                tvContactRequestStatus.setTextColor(
                    Color.parseColor("#F57C00")
                )

                tvApprovedContactNumber.visibility = View.GONE
            }
            .addOnFailureListener { error ->

                btnRequestContact.isEnabled = true

                showToast(
                    "Failed to send request: ${error.message}"
                )
            }
    }

    private fun loadItemPhoto(photoPath: String) {

        if (photoPath.isBlank()) {
            ivDetailPhoto.visibility = View.GONE
            tvNoPhoto.visibility = View.VISIBLE
            tvNoPhoto.text = "No photo available"
            return
        }

        ivDetailPhoto.visibility = View.GONE
        tvNoPhoto.visibility = View.VISIBLE
        tvNoPhoto.text = "Loading photo..."

        lifecycleScope.launch {
            try {

                val imageBytes =
                    SupabaseManager.client.storage
                        .from("item-photos")
                        .downloadAuthenticated(photoPath)

                val bitmap = withContext(
                    Dispatchers.Default
                ) {
                    val options =
                        BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }

                    BitmapFactory.decodeByteArray(
                        imageBytes,
                        0,
                        imageBytes.size,
                        options
                    )

                    var sampleSize = 1

                    while (
                        options.outWidth / sampleSize > 1200 ||
                        options.outHeight / sampleSize > 1200
                    ) {
                        sampleSize *= 2
                    }

                    options.inJustDecodeBounds = false
                    options.inSampleSize = sampleSize

                    BitmapFactory.decodeByteArray(
                        imageBytes,
                        0,
                        imageBytes.size,
                        options
                    )
                }

                if (bitmap != null) {
                    ivDetailPhoto.setImageBitmap(bitmap)
                    ivDetailPhoto.visibility = View.VISIBLE
                    tvNoPhoto.visibility = View.GONE
                } else {
                    showPhotoError()
                }

            } catch (error: CancellationException) {
                throw error

            } catch (error: Exception) {
                showPhotoError()
            }
        }
    }

    private fun showPhotoError() {
        ivDetailPhoto.visibility = View.GONE
        tvNoPhoto.visibility = View.VISIBLE
        tvNoPhoto.text = "Unable to load photo"
    }

    override fun onResume() {
        super.onResume()

        // Refresh request approval after returning to screen
        currentItem?.let { item ->
            setupContactRequest(item)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}

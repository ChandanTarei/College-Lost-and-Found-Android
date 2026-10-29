
package com.example.collegelostandfound

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

import io.github.jan.supabase.storage.storage

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Calendar
import java.util.Locale

class ReportItemActivity : AppCompatActivity() {

    private val databaseUrl =
        "https://collegelostandfound-2007-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val maxPhotoSize = 5 * 1024 * 1024

    private lateinit var etItemName: EditText
    private lateinit var spCategory: Spinner
    private lateinit var rgItemType: RadioGroup
    private lateinit var etDescription: EditText
    private lateinit var etLocation: EditText
    private lateinit var etContactNumber: EditText
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectPhoto: Button
    private lateinit var btnSubmitReport: Button
    private lateinit var ivItemPreview: ImageView

    private var selectedDate = ""
    private var selectedImageUri: Uri? = null

    private val photoPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                selectedImageUri = uri

                ivItemPreview.setImageURI(uri)
                ivItemPreview.visibility = View.VISIBLE

                Toast.makeText(
                    this,
                    "Photo selected successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_report_item)

        UiPolish.apply(this)

        // Check Firebase authentication role
        checkFirebaseRole()

        etItemName = findViewById(R.id.etItemName)
        spCategory = findViewById(R.id.spCategory)
        rgItemType = findViewById(R.id.rgItemType)
        etDescription = findViewById(R.id.etDescription)
        etLocation = findViewById(R.id.etLocation)
        etContactNumber = findViewById(R.id.etContactNumber)

        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnSubmitReport = findViewById(R.id.btnSubmitReport)
        ivItemPreview = findViewById(R.id.ivItemPreview)

        styleReportTypeButtons()

        val categories = listOf(
            "Select Category",
            "Electronics",
            "Wallet",
            "ID Card",
            "Books",
            "Keys",
            "Bag",
            "Clothing",
            "Other"
        )

        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )

        categoryAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spCategory.adapter = categoryAdapter

        btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        btnSelectPhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        btnSubmitReport.setOnClickListener {
            submitReport()
        }
    }

    // Firebase authentication diagnostic
    private fun checkFirebaseRole() {

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Log.e(
                "AUTH_CHECK",
                "No Firebase user logged in"
            )
            return
        }

        Log.d(
            "AUTH_CHECK",
            "Checking role for UID: ${user.uid}"
        )

        user.getIdToken(true)
            .addOnSuccessListener { result ->

                val role = result.claims["role"]

                Log.d(
                    "AUTH_CHECK",
                    "Firebase UID: ${user.uid}"
                )

                Log.d(
                    "AUTH_CHECK",
                    "Firebase role: $role"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    "AUTH_CHECK",
                    "Failed to refresh Firebase token",
                    error
                )
            }
    }

    // Dark Lost / Found labels
    private fun styleReportTypeButtons() {

        for (index in 0 until rgItemType.childCount) {

            val view = rgItemType.getChildAt(index)

            if (view is RadioButton) {

                view.setTextColor(Color.BLACK)

                val indicatorColor =
                    when (view.text.toString().trim()) {

                        "Lost" -> Color.RED

                        "Found" -> Color.rgb(0, 137, 123)

                        else -> Color.BLACK
                    }

                view.buttonTintList =
                    ColorStateList.valueOf(indicatorColor)
            }
        }
    }

    // Date picker
    private fun showDatePicker() {

        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->

                selectedDate = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    year,
                    month + 1,
                    day
                )

                btnSelectDate.text = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }

    // Submit item report
    private fun submitReport() {

        val userId =
            FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            showMessage("Please login first")
            return
        }

        val itemName =
            etItemName.text.toString().trim()

        val category =
            spCategory.selectedItem.toString()

        val description =
            etDescription.text.toString().trim()

        val location =
            etLocation.text.toString().trim()

        val contactNumber =
            etContactNumber.text.toString().trim()

        val selectedTypeId =
            rgItemType.checkedRadioButtonId

        if (itemName.isEmpty()) {
            etItemName.error = "Enter item name"
            return
        }

        if (spCategory.selectedItemPosition == 0) {
            showMessage("Please select a category")
            return
        }

        if (selectedTypeId == -1) {
            showMessage("Please select Lost or Found")
            return
        }

        val itemType =
            findViewById<RadioButton>(selectedTypeId)
                .text.toString().trim()

        if (itemType != "Lost" && itemType != "Found") {
            showMessage("Invalid report type")
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "Enter description"
            return
        }

        if (location.isEmpty()) {
            etLocation.error = "Enter location"
            return
        }

        if (
            contactNumber.isNotEmpty() &&
            !contactNumber.matches(
                Regex("[0-9+() -]{7,15}")
            )
        ) {
            etContactNumber.error =
                "Enter a valid contact number"
            return
        }

        if (selectedDate.isEmpty()) {
            showMessage("Please select a date")
            return
        }

        val database =
            FirebaseDatabase.getInstance(databaseUrl)

        val reportRef =
            database.getReference("items").push()

        val reportId = reportRef.key

        if (reportId == null) {
            showMessage("Unable to create report")
            return
        }

        val itemData = hashMapOf<String, Any>(
            "reportId" to reportId,
            "userId" to userId,
            "itemName" to itemName,
            "category" to category,
            "type" to itemType,
            "description" to description,
            "location" to location,
            "date" to selectedDate,
            "status" to "Pending"
        )

        btnSubmitReport.isEnabled = false
        btnSubmitReport.text = "Submitting..."

        reportRef.setValue(itemData)
            .addOnSuccessListener {

                // Save optional private contact
                val contactTask =
                    if (contactNumber.isNotEmpty()) {

                        database
                            .getReference("privateContacts")
                            .child(userId)
                            .child(reportId)
                            .child("contactNumber")
                            .setValue(contactNumber)

                    } else {
                        null
                    }

                if (contactTask == null) {

                    uploadPhotoIfSelected(userId, reportId)

                } else {

                    contactTask
                        .addOnSuccessListener {

                            uploadPhotoIfSelected(
                                userId,
                                reportId
                            )
                        }
                        .addOnFailureListener { error ->

                            Log.e(
                                "REPORT_SAVE",
                                "Private contact save failed",
                                error
                            )

                            showMessage(
                                "Report saved, but contact save failed"
                            )

                            uploadPhotoIfSelected(
                                userId,
                                reportId
                            )
                        }
                }
            }
            .addOnFailureListener { error ->

                btnSubmitReport.isEnabled = true
                btnSubmitReport.text = "Submit Report"

                Log.e(
                    "REPORT_SAVE",
                    "Firebase report save failed",
                    error
                )

                showMessage(
                    "Report failed: ${error.message}"
                )
            }
    }

    // Supabase photo upload
    private fun uploadPhotoIfSelected(
        userId: String,
        reportId: String
    ) {

        val uri = selectedImageUri

        if (uri == null) {

            reportSaved(
                "Report submitted successfully"
            )
            return
        }

        lifecycleScope.launch {

            try {

                Log.d(
                    "PHOTO_UPLOAD",
                    "Starting photo upload"
                )

                val mimeType =
                    contentResolver.getType(uri)

                val extension = when (mimeType) {

                    "image/jpeg" -> "jpg"

                    "image/png" -> "png"

                    "image/webp" -> "webp"

                    else -> throw IllegalArgumentException(
                        "Only JPG, PNG and WebP photos are allowed"
                    )
                }

                val imageBytes =
                    withContext(Dispatchers.IO) {

                        contentResolver.openInputStream(uri)
                            ?.use { stream ->

                                readPhotoWithLimit(
                                    stream,
                                    maxPhotoSize
                                )

                            } ?: throw IllegalStateException(
                            "Cannot read selected photo"
                        )
                    }

                if (imageBytes.size > maxPhotoSize) {

                    throw IllegalArgumentException(
                        "Photo must be smaller than 5 MB"
                    )
                }

                val photoPath =
                    "$userId/$reportId.$extension"

                Log.d(
                    "PHOTO_UPLOAD",
                    "Photo validated; size=${imageBytes.size}, type=$mimeType"
                )

                // Upload photo to Supabase
                SupabaseManager.client.storage
                    .from("item-photos")
                    .upload(photoPath, imageBytes) {
                        upsert = false
                    }

                Log.d(
                    "PHOTO_UPLOAD",
                    "Supabase photo upload successful"
                )

                // Store photo path in Firebase
                FirebaseDatabase
                    .getInstance(databaseUrl)
                    .getReference("items")
                    .child(reportId)
                    .child("photoPath")
                    .setValue(photoPath)
                    .addOnSuccessListener {

                        Log.d(
                            "PHOTO_UPLOAD",
                            "Photo path saved in Firebase"
                        )

                        reportSaved(
                            "Report and photo saved successfully"
                        )
                    }
                    .addOnFailureListener { error ->

                        Log.e(
                            "PHOTO_UPLOAD",
                            "Firebase photo path save failed",
                            error
                        )

                        reportSaved(
                            "Photo uploaded, but path save failed"
                        )
                    }

            } catch (error: CancellationException) {

                throw error

            } catch (error: Exception) {

                Log.e(
                    "PHOTO_UPLOAD",
                    "Supabase photo upload failed",
                    error
                )

                reportSaved(
                    "Report saved, but photo upload failed. Check Logcat."
                )
            }
        }
    }

    // Read photo safely on Android API 26+
    private fun readPhotoWithLimit(
        input: InputStream,
        maxSize: Int
    ): ByteArray {

        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)

        val limit = maxSize + 1

        while (output.size() < limit) {

            val remaining = limit - output.size()

            val count = input.read(
                buffer,
                0,
                minOf(buffer.size, remaining)
            )

            if (count == -1) break

            if (count > 0) {
                output.write(buffer, 0, count)
            }
        }

        return output.toByteArray()
    }

    private fun reportSaved(message: String) {

        btnSubmitReport.isEnabled = true
        btnSubmitReport.text = "Submit Report"

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    private fun showMessage(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}

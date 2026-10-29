
package com.example.collegelostandfound

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EditReportActivity : AppCompatActivity() {

    private val databaseUrl =
        "https://collegelostandfound-2007-default-rtdb.asia-southeast1.firebasedatabase.app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_report)

        val itemName = findViewById<EditText>(R.id.etEditItemName)
        val category = findViewById<EditText>(R.id.etEditCategory)
        val location = findViewById<EditText>(R.id.etEditLocation)
        val date = findViewById<EditText>(R.id.etEditDate)
        val description = findViewById<EditText>(R.id.etEditDescription)
        val saveButton = findViewById<Button>(R.id.btnSaveChanges)

        val reportId = intent.getStringExtra("reportId")
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (reportId.isNullOrBlank() || userId == null) {
            Toast.makeText(this, "Invalid report or user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val reportRef = FirebaseDatabase
            .getInstance(databaseUrl)
            .getReference("items")
            .child(reportId)

        saveButton.isEnabled = false

        // Load existing report details
        reportRef.get().addOnSuccessListener { snapshot ->
            val report = snapshot.getValue(LostItem::class.java)

            if (report == null || report.userId != userId) {
                Toast.makeText(this, "Report not found or access denied", Toast.LENGTH_SHORT).show()
                finish()
                return@addOnSuccessListener
            }

            itemName.setText(report.itemName)
            category.setText(report.category)
            location.setText(report.location)
            date.setText(report.date)
            description.setText(report.description)

            saveButton.isEnabled = true
        }.addOnFailureListener {
            Toast.makeText(this, "Could not load report", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Save edited information
        saveButton.setOnClickListener {
            val name = itemName.text.toString().trim()
            val cat = category.text.toString().trim()
            val loc = location.text.toString().trim()
            val reportDate = date.text.toString().trim()
            val desc = description.text.toString().trim()

            if (name.isEmpty() || cat.isEmpty() ||
                loc.isEmpty() || reportDate.isEmpty() || desc.isEmpty()
            ) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveButton.isEnabled = false

            // Check ownership again before saving
            reportRef.get().addOnSuccessListener { snapshot ->
                val existing = snapshot.getValue(LostItem::class.java)

                if (existing == null || existing.userId != userId) {
                    Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show()
                    saveButton.isEnabled = true
                    return@addOnSuccessListener
                }

                val updates = mapOf<String, Any>(
                    "itemName" to name,
                    "category" to cat,
                    "location" to loc,
                    "date" to reportDate,
                    "description" to desc
                )

                reportRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Report updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    .addOnFailureListener { error ->
                        saveButton.isEnabled = true
                        Toast.makeText(
                            this,
                            "Update failed: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }.addOnFailureListener {
                saveButton.isEnabled = true
                Toast.makeText(this, "Could not verify report", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

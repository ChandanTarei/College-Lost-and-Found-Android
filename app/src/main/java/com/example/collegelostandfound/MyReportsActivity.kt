
package com.example.collegelostandfound

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import io.github.jan.supabase.storage.storage

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MyReportsActivity : AppCompatActivity() {

    private val itemList = mutableListOf<LostItem>()

    private lateinit var adapter: LostItemAdapter
    private lateinit var database: DatabaseReference
    private lateinit var tvNoReports: TextView

    private val databaseUrl =
        "https://collegelostandfound-2007-default-rtdb.asia-southeast1.firebasedatabase.app"

    private var deletionInProgress = false

    private val itemListener = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {

            itemList.clear()

            val currentUserId =
                FirebaseAuth.getInstance().currentUser?.uid

            for (child in snapshot.children) {

                val item =
                    child.getValue(LostItem::class.java)

                if (
                    item != null &&
                    item.userId == currentUserId
                ) {
                    item.reportId = child.key ?: ""
                    itemList.add(item)
                }
            }

            adapter.notifyDataSetChanged()

            tvNoReports.visibility =
                if (itemList.isEmpty()) View.VISIBLE
                else View.GONE

            if (itemList.isEmpty()) {
                tvNoReports.text =
                    "You haven't reported any items yet."
            }

            UiFeedback.hideLoading(this@MyReportsActivity)
        }

        override fun onCancelled(error: DatabaseError) {

            UiFeedback.hideLoading(this@MyReportsActivity)

            showMessage("Error: ${error.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_my_reports)

        UiPolish.apply(this)
        ScreenInsets.apply(this)

        val recyclerView =
            findViewById<RecyclerView>(
                R.id.recyclerMyReports
            )

        tvNoReports = findViewById(R.id.tvNoReports)

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        adapter = LostItemAdapter(
            items = itemList,
            showDeleteButton = true,

            onDeleteClick = { item ->
                confirmDelete(item)
            },

            onUpdateStatusClick = { item ->
                showStatusDialog(item)
            },

            onViewContactClick = { item ->
                showContactNumber(item)
            },

            onEditClick = { item ->

                val intent = Intent(
                    this@MyReportsActivity,
                    EditReportActivity::class.java
                )

                intent.putExtra("reportId", item.reportId)

                startActivity(intent)
            }
        )

        recyclerView.adapter = adapter

        if (FirebaseAuth.getInstance().currentUser == null) {
            showMessage("Please login first")
            finish()
            return
        }

        database = FirebaseDatabase
            .getInstance(databaseUrl)
            .getReference("items")

        UiFeedback.showLoading(
            this,
            "Loading your reports..."
        )

        database.addValueEventListener(itemListener)
    }

    // Show the report owner's saved contact number
    private fun showContactNumber(item: LostItem) {

        val user =
            FirebaseAuth.getInstance().currentUser

        if (
            user == null ||
            item.userId != user.uid ||
            item.reportId.isBlank()
        ) {
            showMessage(
                "You can only view your own contact number"
            )
            return
        }

        FirebaseDatabase.getInstance(databaseUrl)
            .getReference("privateContacts")
            .child(user.uid)
            .child(item.reportId)
            .child("contactNumber")
            .get()
            .addOnSuccessListener { snapshot ->

                val number =
                    snapshot.getValue(String::class.java)

                AlertDialog.Builder(this)
                    .setTitle("My Contact Number")
                    .setMessage(
                        if (number.isNullOrBlank()) {
                            "No contact number saved for this report."
                        } else {
                            number
                        }
                    )
                    .setPositiveButton("OK", null)
                    .show()
            }
            .addOnFailureListener { error ->
                showMessage(
                    "Unable to load contact: ${error.message}"
                )
            }
    }

    // Update report status
    private fun showStatusDialog(item: LostItem) {

        val newStatus =
            if (item.type == "Lost") {
                "Recovered"
            } else {
                "Returned"
            }

        AlertDialog.Builder(this)
            .setTitle("Update Item Status")
            .setMessage(
                "Mark '${item.itemName}' as $newStatus?"
            )
            .setPositiveButton("Confirm") { _, _ ->

                val currentUserId =
                    FirebaseAuth.getInstance()
                        .currentUser?.uid

                if (
                    currentUserId == null ||
                    item.userId != currentUserId ||
                    item.reportId.isBlank()
                ) {
                    showMessage(
                        "You cannot update this report"
                    )
                } else {
                    database.child(item.reportId)
                        .child("status")
                        .setValue(newStatus)
                        .addOnSuccessListener {
                            showMessage(
                                "Status updated to $newStatus"
                            )
                        }
                        .addOnFailureListener { error ->
                            showMessage(
                                "Update failed: ${error.message}"
                            )
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Confirm before deleting
    private fun confirmDelete(item: LostItem) {

        if (deletionInProgress) {
            showMessage(
                "Another report is being deleted"
            )
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage(
                "Delete '${item.itemName}' and its photo, " +
                        "contact number, and contact requests?"
            )
            .setPositiveButton("Delete") { _, _ ->
                deleteReport(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Delete related data and finally the report itself
    private fun deleteReport(item: LostItem) {

        val currentUserId =
            FirebaseAuth.getInstance().currentUser?.uid

        if (
            currentUserId == null ||
            item.userId != currentUserId ||
            item.reportId.isBlank()
        ) {
            showMessage("You cannot delete this report")
            return
        }

        if (deletionInProgress) return

        deletionInProgress = true

        UiFeedback.showLoading(
            this,
            "Deleting report and associated data..."
        )

        lifecycleScope.launch {

            var currentStep = "checking report"

            try {

                val reportRef =
                    database.child(item.reportId)

                // Recheck ownership and get the latest photo path
                val snapshot =
                    awaitSnapshot(reportRef)

                if (!snapshot.exists()) {
                    throw IllegalStateException(
                        "Report no longer exists"
                    )
                }

                val savedOwnerId =
                    snapshot.child("userId")
                        .getValue(String::class.java)

                if (savedOwnerId != currentUserId) {
                    throw SecurityException(
                        "You do not own this report"
                    )
                }

                val photoPath =
                    snapshot.child("photoPath")
                        .getValue(String::class.java)
                        .orEmpty()

                // Step 1: Delete Supabase photo if present
                if (photoPath.isNotBlank()) {

                    currentStep = "deleting Supabase photo"

                    val validPrefix =
                        "$currentUserId/${item.reportId}."

                    if (
                        !photoPath.startsWith(validPrefix) ||
                        !(
                                photoPath.endsWith(".jpg") ||
                                        photoPath.endsWith(".png") ||
                                        photoPath.endsWith(".webp")
                                )
                    ) {
                        throw IllegalStateException(
                            "Unexpected photo path"
                        )
                    }

                    SupabaseManager.client.storage
                        .from("item-photos")
                        .delete(photoPath)
                }

                val firebase =
                    FirebaseDatabase.getInstance(databaseUrl)

                // Step 2: Delete contact requests
                currentStep = "deleting contact requests"

                awaitRemoval(
                    firebase.getReference("contactRequests")
                        .child(item.reportId)
                )

                // Step 3: Delete private contact information
                currentStep = "deleting private contact"

                awaitRemoval(
                    firebase.getReference("privateContacts")
                        .child(currentUserId)
                        .child(item.reportId)
                )

                // Step 4: Delete report last
                currentStep = "deleting Firebase report"

                awaitRemoval(reportRef)

                showMessage(
                    "Report and associated data deleted successfully"
                )

            } catch (error: CancellationException) {

                throw error

            } catch (error: Exception) {

                showMessage(
                    "Cleanup stopped while $currentStep: " +
                            "${error.message}. " +
                            "Check remaining data before retrying."
                )

            } finally {

                deletionInProgress = false

                UiFeedback.hideLoading(
                    this@MyReportsActivity
                )
            }
        }
    }

    // Read a Firebase node using a coroutine
    private suspend fun awaitSnapshot(
        reference: DatabaseReference
    ): DataSnapshot = suspendCoroutine { continuation ->

        reference.get()
            .addOnSuccessListener { snapshot ->
                continuation.resume(snapshot)
            }
            .addOnFailureListener { error ->
                continuation.resumeWithException(error)
            }
    }

    // Remove a Firebase node using a coroutine
    private suspend fun awaitRemoval(
        reference: DatabaseReference
    ): Unit = suspendCoroutine { continuation ->

        reference.removeValue()
            .addOnSuccessListener {
                continuation.resume(Unit)
            }
            .addOnFailureListener { error ->
                continuation.resumeWithException(error)
            }
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {

        if (::database.isInitialized) {
            database.removeEventListener(itemListener)
        }

        super.onDestroy()
    }
}


package com.example.collegelostandfound

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LostItemsActivity : AppCompatActivity() {

    private val allItems = mutableListOf<LostItem>()
    private val displayedItems = mutableListOf<LostItem>()

    private lateinit var adapter: LostItemAdapter
    private lateinit var database: DatabaseReference

    private var searchText = ""
    private var hasLoadedItems = false

    private val databaseUrl =
        "https://collegelostandfound-2007-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val itemListener = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {

            allItems.clear()

            for (child in snapshot.children) {

                val item = child.getValue(
                    LostItem::class.java
                )

                if (item != null && item.type == "Lost") {

                    // Use Firebase key as the report ID.
                    // This also supports older reports
                    // that were created without photos.
                    item.reportId = child.key ?: ""

                    allItems.add(item)
                }
            }

            hasLoadedItems = true

            // Update reports and search results
            updateList()

            // Hide Firebase loading animation
            UiFeedback.hideLoading(this@LostItemsActivity)
        }

        override fun onCancelled(error: DatabaseError) {

            hasLoadedItems = true

            // Hide loading animation on failure
            UiFeedback.hideLoading(this@LostItemsActivity)

            Toast.makeText(
                this@LostItemsActivity,
                "Failed to load items: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep your existing layout
        setContentView(R.layout.activity_lost_items)

        // Step 21: Modern UI styling
        UiPolish.apply(this)
        ScreenInsets.apply(this)

        // Verify Firebase authentication
        if (FirebaseAuth.getInstance().currentUser == null) {

            Toast.makeText(
                this,
                "Please login first",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        // Find existing RecyclerView without
        // changing any XML resource IDs.
        val root = findViewById<View>(
            android.R.id.content
        )

        val recyclerView = findChild<RecyclerView>(root)

        if (recyclerView == null) {

            Toast.makeText(
                this,
                "Lost Items RecyclerView not found",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        // Configure report list
        recyclerView.layoutManager =
            LinearLayoutManager(this)

        adapter = LostItemAdapter(
            items = displayedItems
        )

        recyclerView.adapter = adapter

        // Keep your existing live search
        val searchField = findChild<EditText>(root)

        searchField?.doOnTextChanged { text, _, _, _ ->

            searchText = text?.toString()
                ?.trim()
                .orEmpty()

            updateList()
        }

        // Connect Firebase Realtime Database
        database = FirebaseDatabase
            .getInstance(databaseUrl)
            .getReference("items")

        // Display loading animation
        UiFeedback.showLoading(
            this,
            "Loading lost items..."
        )

        // Start listening for Firebase items
        database.addValueEventListener(itemListener)
    }

    private fun updateList() {

        if (!::adapter.isInitialized) return

        displayedItems.clear()

        val query = searchText.trim()

        // Filter Lost Items using search
        val filtered = allItems.filter { item ->

            query.isEmpty() ||
                    item.itemName.contains(
                        query,
                        ignoreCase = true
                    ) ||
                    item.category.contains(
                        query,
                        ignoreCase = true
                    ) ||
                    item.location.contains(
                        query,
                        ignoreCase = true
                    ) ||
                    item.description.contains(
                        query,
                        ignoreCase = true
                    )
        }

        displayedItems.addAll(filtered)

        // Refresh RecyclerView
        adapter.notifyDataSetChanged()

        // Show empty message only after
        // Firebase has finished loading.
        if (hasLoadedItems) {

            UiFeedback.showEmpty(
                this,
                displayedItems.isEmpty(),
                if (query.isEmpty()) {
                    "No lost items available.\nReport an item to get started."
                } else {
                    "No lost items found.\nTry another search."
                }
            )
        }
    }

    // Find existing UI controls by type
    // without depending on specific XML IDs.
    private inline fun <reified T : View> findChild(
        root: View
    ): T? {

        return findChildView(
            root,
            T::class.java
        )
    }

    private fun <T : View> findChildView(
        root: View,
        type: Class<T>
    ): T? {

        if (type.isInstance(root)) {
            return type.cast(root)
        }

        if (root is ViewGroup) {

            for (index in 0 until root.childCount) {

                val child = root.getChildAt(index)

                val result = findChildView(
                    child,
                    type
                )

                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    override fun onDestroy() {

        // Stop Firebase listener when Activity closes
        if (::database.isInitialized) {

            database.removeEventListener(
                itemListener
            )
        }

        super.onDestroy()
    }
}

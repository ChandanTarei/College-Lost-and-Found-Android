
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

class FoundItemsActivity : AppCompatActivity() {

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

                if (item != null && item.type == "Found") {

                    // Get the actual Firebase database key.
                    // This also supports reports without photos.
                    item.reportId = child.key ?: ""

                    allItems.add(item)
                }
            }

            hasLoadedItems = true

            // Refresh search and RecyclerView
            updateList()

            // Hide loading animation
            UiFeedback.hideLoading(this@FoundItemsActivity)
        }

        override fun onCancelled(error: DatabaseError) {

            hasLoadedItems = true

            // Hide loading animation when Firebase fails
            UiFeedback.hideLoading(this@FoundItemsActivity)

            Toast.makeText(
                this@FoundItemsActivity,
                "Failed to load items: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep your existing Found Items layout
        setContentView(R.layout.activity_found_items)

        // Step 21: Apply modern UI styling
        UiPolish.apply(this)
        ScreenInsets.apply(this)

        // Check Firebase login
        if (FirebaseAuth.getInstance().currentUser == null) {

            Toast.makeText(
                this,
                "Please login first",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        // Locate your existing RecyclerView
        val root = findViewById<View>(
            android.R.id.content
        )

        val recyclerView = findChildView(
            root,
            RecyclerView::class.java
        )

        if (recyclerView == null) {

            Toast.makeText(
                this,
                "Found Items RecyclerView not found",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        // Maintain existing LostItemAdapter functionality
        adapter = LostItemAdapter(
            items = displayedItems
        )

        recyclerView.adapter = adapter

        // Keep live search working
        val searchField = findChildView(
            root,
            EditText::class.java
        )

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

        // Show loading animation
        UiFeedback.showLoading(
            this,
            "Loading found items..."
        )

        // Listen for Firebase report updates
        database.addValueEventListener(itemListener)
    }

    private fun updateList() {

        if (!::adapter.isInitialized) return

        displayedItems.clear()

        val query = searchText.trim()

        // Filter Found Items
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

        // Refresh report list
        adapter.notifyDataSetChanged()

        // Show an empty-state message only after
        // Firebase has finished loading successfully.
        if (hasLoadedItems) {

            UiFeedback.showEmpty(
                this,
                displayedItems.isEmpty(),
                if (query.isEmpty()) {
                    "No found items available.\nCheck back later or report a found item."
                } else {
                    "No found items found.\nTry another search."
                }
            )
        }
    }

    // Find existing controls without changing
    // any XML resource IDs.
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

        // Remove Firebase listener when this screen closes
        if (::database.isInitialized) {

            database.removeEventListener(
                itemListener
            )
        }

        super.onDestroy()
    }
}

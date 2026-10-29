
package com.example.collegelostandfound

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LostItemAdapter(
    private val items: List<LostItem>,
    private val showDeleteButton: Boolean = false,
    private val onDeleteClick: ((LostItem) -> Unit)? = null,
    private val onUpdateStatusClick: ((LostItem) -> Unit)? = null,
    private val onViewContactClick: ((LostItem) -> Unit)? = null,
    private val onEditClick: ((LostItem) -> Unit)? = null
) : RecyclerView.Adapter<LostItemAdapter.ItemViewHolder>() {

    private var photoScope = createPhotoScope()

    private fun createPhotoScope(): CoroutineScope {
        return CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )
    }

    class ItemViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val itemName: TextView =
            view.findViewById(R.id.tvItemName)

        val category: TextView =
            view.findViewById(R.id.tvCategory)

        val location: TextView =
            view.findViewById(R.id.tvLocation)

        val date: TextView =
            view.findViewById(R.id.tvDate)

        val description: TextView =
            view.findViewById(R.id.tvDescription)

        val status: TextView =
            view.findViewById(R.id.tvItemStatus)

        val reportPhoto: ImageView =
            view.findViewById(R.id.ivReportPhoto)

        val editButton: Button =
            view.findViewById(R.id.btnEditReport)

        val deleteButton: Button =
            view.findViewById(R.id.btnDeleteReport)

        val updateStatusButton: Button =
            view.findViewById(R.id.btnUpdateStatus)

        val viewContactButton: Button =
            view.findViewById(R.id.btnViewContact)

        var photoJob: Job? = null
        var currentPhotoPath = ""
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lost, parent, false)

        return ItemViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int
    ) {

        val item = items[position]

        // Step 21: Rounded modern report cards
        UiPolish.styleReportCard(holder.itemView)

        // Item name
        holder.itemName.text = item.itemName

        holder.itemName.setTextColor(
            Color.parseColor("#3048A5")
        )

        holder.itemName.textSize = 19f

        // Type and category
        holder.category.text =
            "Type: ${item.type} | Category: ${item.category}"

        // Location
        holder.location.text =
            "Location: ${item.location}"

        // Date
        holder.date.text =
            "Date: ${item.date}"

        // Description
        holder.description.text =
            "Description: ${item.description}"

        // Status
        holder.status.text =
            "Status: ${item.status}"

        holder.status.setTextColor(
            if (
                item.status == "Recovered" ||
                item.status == "Returned"
            ) {
                Color.parseColor("#2E7D32")
            } else {
                Color.parseColor("#F57C00")
            }
        )

        // Preserve Supabase photo loading
        loadItemPhoto(
            holder,
            item.photoPath
        )

        // Open Item Details for items with or without photos
        holder.itemView.setOnClickListener {

            if (item.reportId.isNotBlank()) {

                val intent = Intent(
                    holder.itemView.context,
                    ItemDetailsActivity::class.java
                )

                intent.putExtra(
                    "reportId",
                    item.reportId
                )

                holder.itemView.context.startActivity(
                    intent
                )
            }
        }

        // Edit Report
        holder.editButton.visibility =
            if (showDeleteButton && onEditClick != null) {
                View.VISIBLE
            } else {
                View.GONE
            }

        holder.editButton.setOnClickListener {
            onEditClick?.invoke(item)
        }

        // Delete Report
        holder.deleteButton.visibility =
            if (showDeleteButton && onDeleteClick != null) {
                View.VISIBLE
            } else {
                View.GONE
            }

        holder.deleteButton.setOnClickListener {
            onDeleteClick?.invoke(item)
        }

        // Update Status
        holder.updateStatusButton.visibility =
            if (
                showDeleteButton &&
                onUpdateStatusClick != null
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        holder.updateStatusButton.setOnClickListener {
            onUpdateStatusClick?.invoke(item)
        }

        // View owner's private contact
        holder.viewContactButton.visibility =
            if (
                showDeleteButton &&
                onViewContactClick != null
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        holder.viewContactButton.setOnClickListener {
            onViewContactClick?.invoke(item)
        }
    }

    private fun loadItemPhoto(
        holder: ItemViewHolder,
        photoPath: String
    ) {

        // Cancel previous photo loading when card is reused
        holder.photoJob?.cancel()
        holder.photoJob = null

        holder.currentPhotoPath = photoPath
        holder.reportPhoto.setImageDrawable(null)

        // Photo is optional
        if (photoPath.isBlank()) {

            holder.reportPhoto.visibility = View.GONE
            return
        }

        holder.reportPhoto.visibility = View.VISIBLE

        // Temporary image while loading
        holder.reportPhoto.setImageResource(
            android.R.drawable.ic_menu_gallery
        )

        holder.photoJob = photoScope.launch {

            try {

                // Download photo from private Supabase Storage
                val imageBytes =
                    SupabaseManager.client.storage
                        .from("item-photos")
                        .downloadAuthenticated(photoPath)

                // Decode on background thread
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
                        options.outWidth / sampleSize > 1024 ||
                        options.outHeight / sampleSize > 1024
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

                // Prevent the wrong photo appearing on recycled cards
                if (holder.currentPhotoPath == photoPath) {

                    if (bitmap != null) {

                        holder.reportPhoto.setImageBitmap(
                            bitmap
                        )

                    } else {

                        holder.reportPhoto.setImageResource(
                            android.R.drawable.ic_menu_gallery
                        )
                    }
                }

            } catch (error: CancellationException) {

                throw error

            } catch (error: Exception) {

                Log.e(
                    "LostItemAdapter",
                    "Photo download failed: $photoPath",
                    error
                )

                if (holder.currentPhotoPath == photoPath) {

                    holder.reportPhoto.setImageResource(
                        android.R.drawable.ic_menu_gallery
                    )
                }
            }
        }
    }

    override fun onViewRecycled(
        holder: ItemViewHolder
    ) {

        holder.photoJob?.cancel()
        holder.photoJob = null

        holder.currentPhotoPath = ""

        holder.reportPhoto.setImageDrawable(null)

        super.onViewRecycled(holder)
    }

    override fun onAttachedToRecyclerView(
        recyclerView: RecyclerView
    ) {
        super.onAttachedToRecyclerView(recyclerView)

        // Create a new scope if this adapter is reused
        if (
            photoScope.coroutineContext[Job]
                ?.isCancelled == true
        ) {
            photoScope = createPhotoScope()
        }
    }

    override fun onDetachedFromRecyclerView(
        recyclerView: RecyclerView
    ) {

        // Stop photo downloads when the list is detached
        photoScope.coroutineContext[Job]?.cancel()

        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun getItemCount(): Int = items.size
}

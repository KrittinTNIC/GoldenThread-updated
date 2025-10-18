package com.example.finalproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.R
import com.example.finalproject.ui.thread.data.LocationDramaItem

class FavoriteThreadAdapter : ListAdapter<LocationDramaItem, FavoriteThreadAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_drama, parent, false) // Reusing the same layout
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLocationName: TextView = itemView.findViewById(R.id.tvLocationName)
        private val tvDramaTitle: TextView = itemView.findViewById(R.id.tvDramaTitle)
        private val tvSceneNotes: TextView = itemView.findViewById(R.id.tvSceneNotes)

        fun bind(item: LocationDramaItem) {
            tvLocationName.text = item.nameEn
            tvDramaTitle.text = item.titleEn
            tvSceneNotes.text = item.sceneNotes

            // Hide views that are not needed
            itemView.findViewById<View>(R.id.btnGoToDrama).visibility = View.GONE
            itemView.findViewById<View>(R.id.btnNextPoint).visibility = View.GONE
            itemView.findViewById<View>(R.id.btnFavoriteDrama).visibility = View.GONE
            itemView.findViewById<View>(R.id.tvTravelTime).visibility = View.GONE
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<LocationDramaItem>() {
        override fun areItemsTheSame(oldItem: LocationDramaItem, newItem: LocationDramaItem): Boolean {
            return oldItem.nameEn == newItem.nameEn && oldItem.titleEn == newItem.titleEn
        }

        override fun areContentsTheSame(oldItem: LocationDramaItem, newItem: LocationDramaItem): Boolean {
            return oldItem == newItem
        }
    }
}

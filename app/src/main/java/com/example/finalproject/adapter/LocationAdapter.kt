package com.example.finalproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.R
import com.example.finalproject.model.FeaturedLocation

class LocationAdapter(
    private val onLocationClick: (FeaturedLocation) -> Unit
) : ListAdapter<FeaturedLocation, LocationAdapter.LocationViewHolder>(LocationDiffCallback) {

    inner class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLocationName: TextView = itemView.findViewById(R.id.tvLocationName)
        private val tvCity: TextView = itemView.findViewById(R.id.tvCity)
        private val tvDramaCount: TextView = itemView.findViewById(R.id.tvDramaCount)

        fun bind(location: FeaturedLocation) {
            tvLocationName.text = location.name
            tvCity.text = location.city
            tvDramaCount.text = "${location.dramaCount} dramas"

            itemView.setOnClickListener {
                onLocationClick(location)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_featured_location, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val LocationDiffCallback = object : DiffUtil.ItemCallback<FeaturedLocation>() {
            override fun areItemsTheSame(oldItem: FeaturedLocation, newItem: FeaturedLocation): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: FeaturedLocation, newItem: FeaturedLocation): Boolean {
                return oldItem == newItem
            }
        }
    }
}
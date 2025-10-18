package com.example.finalproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.R
import com.example.finalproject.model.LocationDetail

class LocationDetailAdapter : ListAdapter<LocationDetail, LocationDetailAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_simple, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLocationName: TextView = itemView.findViewById(R.id.tvLocationName)
        private val tvLocationAddress: TextView = itemView.findViewById(R.id.tvLocationAddress)
        private val tvTravelTime: TextView = itemView.findViewById(R.id.tvTravelTime)
        private val tvSceneNotes: TextView = itemView.findViewById(R.id.tvSceneNotes)

        fun bind(location: LocationDetail) {
            tvLocationName.text = location.name
            tvLocationAddress.text = location.address
            tvTravelTime.text = itemView.context.getString(R.string.travel_time_format, location.travelTimeFromPrevious)
            tvSceneNotes.text = location.description ?: itemView.context.getString(R.string.no_scene_description)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<LocationDetail>() {
            override fun areItemsTheSame(oldItem: LocationDetail, newItem: LocationDetail): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: LocationDetail, newItem: LocationDetail): Boolean {
                return oldItem == newItem
            }
        }
    }
}
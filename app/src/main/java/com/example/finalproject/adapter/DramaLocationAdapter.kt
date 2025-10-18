package com.example.finalproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.R
import com.example.finalproject.ui.thread.data.LocationDramaItem
import com.example.finalproject.util.FavoriteManager

class DramaLocationAdapter(
    private val listener: OnItemButtonClickListener
) : ListAdapter<LocationDramaItem, DramaLocationAdapter.ViewHolder>(DiffCallback) {

    interface OnItemButtonClickListener {
        fun onGoToDrama(item: LocationDramaItem)
        fun onNextPoint(item: LocationDramaItem)
        fun onFavorite(item: LocationDramaItem, isFavorite: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_drama, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLocationName: TextView = itemView.findViewById(R.id.tvLocationName)
        private val tvDramaTitle: TextView = itemView.findViewById(R.id.tvDramaTitle)
        private val tvSceneNotes: TextView = itemView.findViewById(R.id.tvSceneNotes)
        private val tvTravelTime: TextView = itemView.findViewById(R.id.tvTravelTime)
        private val btnGoToDrama: Button = itemView.findViewById(R.id.btnGoToDrama)
        private val btnNextPoint: Button = itemView.findViewById(R.id.btnNextPoint)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavoriteDrama)

        fun bind(
            item: LocationDramaItem,
            listener: OnItemButtonClickListener
        ) {
            tvLocationName.text = item.nameEn
            tvDramaTitle.text = "${item.titleEn} (${item.releaseYear})"
            tvSceneNotes.text = item.sceneNotes
            tvTravelTime.text =
                if (item.carTravelMin > 0) "${item.carTravelMin} min drive" else "Starting point"

            // Check if the item is in favorite threads and set the initial icon state
            val isFavorite = FavoriteManager.isThreadFavorite(item)
            updateFavoriteButton(isFavorite)

            btnGoToDrama.setOnClickListener { listener.onGoToDrama(item) }
            btnNextPoint.setOnClickListener { listener.onNextPoint(item) }

            // Set the click listener for the favorite button
            btnFavorite.setOnClickListener {
                val newFavoriteState = !FavoriteManager.isThreadFavorite(item)
                if (newFavoriteState) {
                    FavoriteManager.addThreadFavorite(item)
                } else {
                    FavoriteManager.removeThreadFavorite(item)
                }
                // Update the icon immediately
                updateFavoriteButton(newFavoriteState)
                // Notify the fragment that a change was made
                listener.onFavorite(item, newFavoriteState)
            }
        }

        private fun updateFavoriteButton(isFavorite: Boolean) {
            if (isFavorite) {
                btnFavorite.setImageResource(R.drawable.ic_favorite_filled)
                btnFavorite.setColorFilter(ContextCompat.getColor(itemView.context, R.color.main))
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                btnFavorite.setColorFilter(ContextCompat.getColor(itemView.context, R.color.main))
            }
        }
    }

    companion object {
        object DiffCallback : DiffUtil.ItemCallback<LocationDramaItem>() {
            override fun areItemsTheSame(
                oldItem: LocationDramaItem,
                newItem: LocationDramaItem
            ): Boolean {
                // Use a unique combination of fields to determine if items are the same.
                return oldItem.nameEn == newItem.nameEn && oldItem.titleEn == newItem.titleEn
            }

            override fun areContentsTheSame(
                oldItem: LocationDramaItem,
                newItem: LocationDramaItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}

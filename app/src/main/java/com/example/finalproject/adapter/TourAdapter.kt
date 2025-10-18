package com.example.finalproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.finalproject.model.Tour
import com.example.finalproject.R

class TourAdapter(
    private val onTourClick: (Tour) -> Unit,
    private val onFavoriteClick: (Tour, Boolean) -> Unit
) : ListAdapter<Tour, TourAdapter.TourViewHolder>(TourDiffCallback) {

    private val favorites = mutableSetOf<String>()

    inner class TourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivTourPoster: ImageView = itemView.findViewById(R.id.ivTourPoster)
        private val tvTourTitle: TextView = itemView.findViewById(R.id.tvTourTitle)
        private val tvTourSummary: TextView = itemView.findViewById(R.id.tvTourSummary)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavoriteDrama)

        fun bind(tour: Tour) {
            tvTourTitle.text = tour.titleEn
            tvTourSummary.text = tour.description ?: "No description available"

            // Load image
            Glide.with(itemView.context)
                .load(tour.posterUrl)
                .placeholder(R.drawable.placeholder_bg)
                .error(R.drawable.placeholder_bg)
                .centerCrop()
                .into(ivTourPoster)

            // Set favorite state
            // updateFavoriteIcon(favorites.contains(tour.dramaId))

            // Set click listeners
            itemView.setOnClickListener {
                onTourClick(tour)
            }

            /**
            btnFavorite.setOnClickListener {
                val isCurrentlyFavorite = favorites.contains(tour.dramaId)
                val newFavoriteState = !isCurrentlyFavorite
                onFavoriteClick(tour, newFavoriteState) // Notify fragment

                // Update internal state and UI
                if (newFavoriteState) {
                    favorites.add(tour.dramaId)
                } else {
                    favorites.remove(tour.dramaId)
                }
                updateFavoriteIcon(newFavoriteState)
            }
            **/
        }

        /**
        private fun updateFavoriteIcon(isFavorite: Boolean) {
            val favoriteIcon = if (isFavorite) {
                R.drawable.ic_favorite_filled
            } else {
                R.drawable.ic_favorite_border
            }
            btnFavorite.setImageResource(favoriteIcon)
        }
        **/
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TourViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tour_card, parent, false)
        return TourViewHolder(view)
    }

    override fun onBindViewHolder(holder: TourViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setFavorites(favoriteIds: Set<String>) {
        favorites.clear()
        favorites.addAll(favoriteIds)
        notifyDataSetChanged()
    }

    companion object {
        private val TourDiffCallback = object : DiffUtil.ItemCallback<Tour>() {
            override fun areItemsTheSame(oldItem: Tour, newItem: Tour): Boolean {
                return oldItem.dramaId == newItem.dramaId
            }

            override fun areContentsTheSame(oldItem: Tour, newItem: Tour): Boolean {
                return oldItem == newItem
            }
        }
    }
}

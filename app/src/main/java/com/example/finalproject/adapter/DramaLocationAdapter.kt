package com.example.finalproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.R
import com.example.finalproject.ui.thread.ThreadFragment


class DramaLocationAdapter(
    private var items: List<ThreadFragment.LocationDramaItem>,
    private val listener: OnItemButtonClickListener
) : RecyclerView.Adapter<DramaLocationAdapter.ViewHolder>() {

    interface OnItemButtonClickListener {
        fun onGoToDrama(item: ThreadFragment.LocationDramaItem)
        fun onNextPoint(item: ThreadFragment.LocationDramaItem)
        fun onFavorite(item: ThreadFragment.LocationDramaItem)
    }

    fun updateData(newItems: List<ThreadFragment.LocationDramaItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_drama, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, listener)
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLocationName: TextView = itemView.findViewById(R.id.tvLocationName)
        private val tvDramaTitle: TextView = itemView.findViewById(R.id.tvDramaTitle)
        private val tvSceneNotes: TextView = itemView.findViewById(R.id.tvSceneNotes)
        private val tvTravelTime: TextView = itemView.findViewById(R.id.tvTravelTime)
        private val btnGoToDrama: Button = itemView.findViewById(R.id.btnGoToDrama)
        private val btnNextPoint: Button = itemView.findViewById(R.id.btnNextPoint)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)

        fun bind(item: ThreadFragment.LocationDramaItem, listener: OnItemButtonClickListener) {
            tvLocationName.text = item.nameEn
            tvDramaTitle.text = "${item.titleEn} (${item.releaseYear})"
            tvSceneNotes.text = item.sceneNotes
            tvTravelTime.text = if (item.carTravelMin > 0) "${item.carTravelMin} min drive" else "Starting point"

            // Button click listeners
            btnGoToDrama.setOnClickListener { listener.onGoToDrama(item) }
            btnNextPoint.setOnClickListener { listener.onNextPoint(item) }
            btnFavorite.setOnClickListener { listener.onFavorite(item) }
        }
    }
}
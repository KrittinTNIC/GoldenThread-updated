package com.example.finalproject.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.finalproject.R
import com.example.finalproject.databinding.ItemPosterBinding
import com.example.finalproject.model.Drama

class PosterAdapter(
    private val context: Context,
    private val dramaList: List<Drama>,
    private val onDramaClick: ((Drama) -> Unit)? = null
) : RecyclerView.Adapter<PosterAdapter.PosterViewHolder>() {

    inner class PosterViewHolder(val binding: ItemPosterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        val binding = ItemPosterBinding.inflate(LayoutInflater.from(context), parent, false)
        return PosterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
        val drama = dramaList[position]

        // Load image
        val glideUrl = GlideUrl(
            drama.posterUrl,
            LazyHeaders.Builder()
                .addHeader("Referer", "https://mydramalist.com")
                .build()
        )

        Glide.with(context)
            .load(glideUrl)
            .placeholder(R.drawable.placeholder_poster)
            .error(R.drawable.placeholder_poster)
            .into(holder.binding.posterImage)

        holder.itemView.setOnClickListener {
            onDramaClick?.invoke(drama)
        }
    }

    override fun getItemCount(): Int = dramaList.size
}
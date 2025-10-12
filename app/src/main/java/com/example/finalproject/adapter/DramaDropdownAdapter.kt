package com.example.finalproject.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.finalproject.R
import com.example.finalproject.model.Drama

class DramaDropdownAdapter(context: Context, private val dramas: List<Drama>) :
    ArrayAdapter<Drama>(context, R.layout.item_dropdown_drama, dramas) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown_drama, parent, false)

        val drama = getItem(position)
        val tvTitle = view.findViewById<TextView>(R.id.tvDramaTitle)
        val tvYear = view.findViewById<TextView>(R.id.tvDramaYear)

        drama?.let {
            tvTitle.text = it.titleEn
            tvYear.text = it.releaseYear.toString()
        }

        return view
    }
}
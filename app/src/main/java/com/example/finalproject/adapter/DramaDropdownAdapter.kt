package com.example.finalproject.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.finalproject.R
import com.example.finalproject.model.Drama

class DramaDropdownAdapter(context: Context, private val originalList: List<Drama>) :
    ArrayAdapter<Drama>(context, R.layout.item_dropdown_drama, ArrayList(originalList)) {

    private var filteredList: List<Drama> = originalList

    override fun getCount(): Int = filteredList.size

    override fun getItem(position: Int): Drama? = filteredList[position]

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

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                if (constraint.isNullOrBlank()) {
                    results.values = originalList
                    results.count = originalList.size
                } else {
                    val query = constraint.toString().trim().lowercase()

                    val filtered = originalList.filter {
                        it.titleEn.lowercase().contains(query) ||
                                it.titleTh.lowercase().contains(query)
                    }.sortedBy { drama ->
                        val enIndex = drama.titleEn.lowercase().indexOf(query)
                        val thIndex = drama.titleTh.lowercase().indexOf(query)
                        minOf(
                            if (enIndex == -1) Int.MAX_VALUE else enIndex,
                            if (thIndex == -1) Int.MAX_VALUE else thIndex
                        )
                    }

                    results.values = filtered
                    results.count = filtered.size
                }
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as? List<Drama> ?: emptyList()
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return (resultValue as? Drama)?.titleEn ?: ""
            }
        }
    }
}

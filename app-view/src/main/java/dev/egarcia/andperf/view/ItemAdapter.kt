package dev.egarcia.andperf.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.egarcia.andperf.shared.Item


class ItemAdapter(private val data: List<Item>) : RecyclerView.Adapter<ItemVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return ItemVH(v)
    }
    override fun onBindViewHolder(holder: ItemVH, position: Int) = holder.bind(data[position])
    override fun getItemCount(): Int = data.size
}


class ItemVH(v: View) : RecyclerView.ViewHolder(v) {
    private val title: TextView = v.findViewById(R.id.title)
    private val subtitle: TextView = v.findViewById(R.id.subtitle)
    fun bind(item: Item) {
        title.text = item.title
        subtitle.text = item.subtitle
    }
}
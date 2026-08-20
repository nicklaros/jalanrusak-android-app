package com.jalanrusak.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jalanrusak.R
import com.jalanrusak.data.api.dto.TopAreaResponse
import com.jalanrusak.databinding.ItemTopAreaBinding

class TopAreasAdapter(
    private val onItemClick: ((TopAreaResponse) -> Unit)? = null
) : RecyclerView.Adapter<TopAreasAdapter.TopAreaViewHolder>() {

    private var items = listOf<TopAreaResponse>()

    fun submitList(newItems: List<TopAreaResponse>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopAreaViewHolder {
        val binding = ItemTopAreaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopAreaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopAreaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TopAreaViewHolder(
        private val binding: ItemTopAreaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TopAreaResponse) {
            binding.rankText.text = getRankDisplay(item.rank)
            binding.areaNameText.text = item.name ?: item.code
            binding.areaCodeText.text = item.code
            binding.reportCountText.text = item.reportCount.toString()

            // Set rank badge color
            val rankColor = when (item.rank) {
                1 -> R.color.accent // Gold - first place
                2 -> R.color.gray_500 // Silver - second place
                3 -> R.color.warning // Bronze - third place
                else -> R.color.primary
            }
            binding.rankText.setTextColor(
                ContextCompat.getColor(binding.root.context, rankColor)
            )

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }

        private fun getRankDisplay(rank: Int): String {
            return when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "#$rank"
            }
        }
    }
}

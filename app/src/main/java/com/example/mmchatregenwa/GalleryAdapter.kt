package com.example.mmchatregenwa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mmchatregenwa.databinding.ItemGalleryGridBinding

class GalleryAdapter(private val onMediaClick: (ChatMessage.Media) -> Unit) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    private var items: List<ChatMessage.Media> = emptyList()

    fun submitList(newItems: List<ChatMessage.Media>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemGalleryGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GalleryViewHolder(binding, onMediaClick)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class GalleryViewHolder(
        private val binding: ItemGalleryGridBinding,
        private val onMediaClick: (ChatMessage.Media) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ChatMessage.Media) {
            binding.ivVideoIcon.visibility = if (item.fileName.endsWith(".mp4", true)) View.VISIBLE else View.GONE
            
            Glide.with(binding.root.context)
                .load(item.mediaUri)
                .centerCrop()
                .into(binding.ivGalleryThumbnail)

            binding.root.setOnClickListener {
                onMediaClick(item)
            }
        }
    }
}

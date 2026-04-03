package com.example.mmchatregenwa

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mmchatregenwa.databinding.ItemMediaPagerBinding

class MediaPagerAdapter : RecyclerView.Adapter<MediaPagerAdapter.MediaViewHolder>() {

    private var mediaItems: List<ChatMessage.Media> = emptyList()

    fun submitList(items: List<ChatMessage.Media>) {
        mediaItems = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaPagerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(mediaItems[position])
    }

    override fun onViewRecycled(holder: MediaViewHolder) {
        holder.stopVideo()
    }

    override fun getItemCount(): Int = mediaItems.size

    class MediaViewHolder(private val binding: ItemMediaPagerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage.Media) {
            val uri = item.mediaUri?.let { Uri.parse(it) } ?: return
            val isVideo = item.fileName.endsWith(".mp4", true)

            if (isVideo) {
                binding.ivPagerMedia.visibility = View.GONE
                binding.vvPagerMedia.visibility = View.VISIBLE
                binding.vvPagerMedia.setVideoURI(uri)
                binding.vvPagerMedia.setOnPreparedListener { it.start() }
            } else {
                binding.vvPagerMedia.visibility = View.GONE
                binding.ivPagerMedia.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(uri)
                    .into(binding.ivPagerMedia)
            }
        }

        fun stopVideo() {
            binding.vvPagerMedia.stopPlayback()
        }
    }
}

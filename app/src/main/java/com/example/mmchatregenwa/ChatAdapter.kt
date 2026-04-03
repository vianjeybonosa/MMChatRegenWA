package com.example.mmchatregenwa

import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mmchatregenwa.databinding.ItemChatAudioBinding
import com.example.mmchatregenwa.databinding.ItemChatMediaBinding
import com.example.mmchatregenwa.databinding.ItemChatSystemBinding
import com.example.mmchatregenwa.databinding.ItemChatTextBinding

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingUri: String? = null

    companion object {
        private const val TYPE_TEXT = 0
        private const val TYPE_MEDIA = 1
        private const val TYPE_SYSTEM = 2
        private const val TYPE_AUDIO = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatMessage.Text -> TYPE_TEXT
            is ChatMessage.Media -> TYPE_MEDIA
            is ChatMessage.System -> TYPE_SYSTEM
            is ChatMessage.Audio -> TYPE_AUDIO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TEXT -> TextViewHolder(ItemChatTextBinding.inflate(inflater, parent, false))
            TYPE_MEDIA -> MediaViewHolder(ItemChatMediaBinding.inflate(inflater, parent, false))
            TYPE_SYSTEM -> SystemViewHolder(ItemChatSystemBinding.inflate(inflater, parent, false))
            TYPE_AUDIO -> AudioViewHolder(ItemChatAudioBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is TextViewHolder -> holder.bind(item as ChatMessage.Text)
            is MediaViewHolder -> holder.bind(item as ChatMessage.Media)
            is SystemViewHolder -> holder.bind(item as ChatMessage.System)
            is AudioViewHolder -> holder.bind(item as ChatMessage.Audio)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is MediaViewHolder) {
            Glide.with(holder.binding.root.context).clear(holder.binding.ivMedia)
        }
    }

    class TextViewHolder(private val binding: ItemChatTextBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage.Text) {
            val params = binding.cardView.layoutParams as ConstraintLayout.LayoutParams
            if (item.isMe) {
                params.startToStart = ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                params.horizontalBias = 1f
                binding.cardView.setCardBackgroundColor(Color.parseColor("#DCF8C6"))
                binding.tvSender.visibility = View.GONE
            } else {
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                params.horizontalBias = 0f
                binding.cardView.setCardBackgroundColor(Color.WHITE)
                binding.tvSender.visibility = View.VISIBLE
                binding.tvSender.text = item.sender
            }
            binding.cardView.layoutParams = params
            binding.tvMessage.text = item.message
            binding.tvTimestamp.text = item.timestamp
        }
    }

    class MediaViewHolder(val binding: ItemChatMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage.Media) {
            val params = binding.cardViewMedia.layoutParams as ConstraintLayout.LayoutParams
            if (item.isSticker) {
                binding.cardViewMedia.cardElevation = 0f
                binding.cardViewMedia.setCardBackgroundColor(Color.TRANSPARENT)
                binding.ivMedia.layoutParams.width = 160.dpToPx()
                binding.ivMedia.layoutParams.height = 160.dpToPx()
            } else {
                binding.cardViewMedia.cardElevation = 1f
                binding.ivMedia.layoutParams.width = 240.dpToPx()
                binding.ivMedia.layoutParams.height = 240.dpToPx()
                binding.cardViewMedia.setCardBackgroundColor(if (item.isMe) Color.parseColor("#DCF8C6") else Color.WHITE)
            }
            if (item.isMe) {
                params.startToStart = ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                params.horizontalBias = 1f
                binding.tvSenderMedia.visibility = View.GONE
            } else {
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                params.horizontalBias = 0f
                binding.tvSenderMedia.visibility = if (item.isSticker) View.GONE else View.VISIBLE
                binding.tvSenderMedia.text = item.sender
            }
            binding.cardViewMedia.layoutParams = params
            binding.tvTimestampMedia.text = item.timestamp
            
            val mediaUri = item.mediaUri
            if (mediaUri != null) {
                binding.ivMedia.visibility = View.VISIBLE
                binding.tvMediaStatus.visibility = View.GONE
                Glide.with(binding.root.context)
                    .load(Uri.parse(mediaUri))
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .dontTransform() // Prevent downsampling that breaks animations
                    .error(android.R.drawable.stat_notify_error)
                    .into(binding.ivMedia)
            } else {
                binding.ivMedia.visibility = View.GONE
                binding.tvMediaStatus.visibility = View.VISIBLE
                binding.tvMediaStatus.text = "File Missing: ${item.fileName}"
            }
        }

        private fun Int.dpToPx(): Int = (this * binding.root.context.resources.displayMetrics.density).toInt()
    }

    inner class AudioViewHolder(private val binding: ItemChatAudioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage.Audio) {
            val params = binding.cardViewAudio.layoutParams as ConstraintLayout.LayoutParams
            if (item.isMe) {
                params.startToStart = ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                params.horizontalBias = 1f
                binding.cardViewAudio.setCardBackgroundColor(Color.parseColor("#DCF8C6"))
            } else {
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                params.horizontalBias = 0f
                binding.cardViewAudio.setCardBackgroundColor(Color.WHITE)
            }
            binding.cardViewAudio.layoutParams = params
            binding.tvAudioTimestamp.text = item.timestamp
            
            val isPlaying = currentlyPlayingUri == item.audioUri
            binding.btnPlayPause.setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)

            binding.btnPlayPause.setOnClickListener {
                if (isPlaying) {
                    stopAudio()
                } else {
                    item.audioUri?.let { uri -> playAudio(uri) }
                }
            }
        }

        private fun playAudio(uriString: String) {
            stopAudio()
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(binding.root.context, Uri.parse(uriString))
                    prepare()
                    start()
                    setOnCompletionListener { stopAudio() }
                }
                currentlyPlayingUri = uriString
                this@ChatAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopAudio() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlayingUri = null
        notifyDataSetChanged()
    }

    class SystemViewHolder(private val binding: ItemChatSystemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage.System) {
            binding.tvSystemMessage.text = item.message
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem == newItem
    }
}

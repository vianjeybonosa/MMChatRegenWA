package com.example.mmchatregenwa

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.mmchatregenwa.databinding.ItemChatAudioBinding
import com.example.mmchatregenwa.databinding.ItemChatMediaBinding
import com.example.mmchatregenwa.databinding.ItemChatSystemBinding
import com.example.mmchatregenwa.databinding.ItemChatTextBinding

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingUri: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var updateSeekBarRunnable: Runnable? = null

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
            is AudioViewHolder -> holder.bind(item as ChatMessage.Audio)
            is SystemViewHolder -> holder.bind(item as ChatMessage.System)
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
            val context = binding.root.context
            
            if (item.isSticker) {
                binding.cardViewMedia.cardElevation = 0f
                binding.cardViewMedia.setCardBackgroundColor(Color.TRANSPARENT)
                binding.ivMedia.layoutParams.width = 160.dpToPx()
                binding.ivMedia.layoutParams.height = 160.dpToPx()
                binding.ivMedia.adjustViewBounds = false
                binding.ivMedia.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            } else {
                binding.cardViewMedia.cardElevation = 1f
                // Dynamically size based on aspect ratio
                binding.ivMedia.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.ivMedia.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.ivMedia.adjustViewBounds = true
                binding.ivMedia.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
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
            
            val mediaUriStr = item.mediaUri
            if (mediaUriStr != null) {
                binding.ivMedia.visibility = View.VISIBLE
                binding.tvMediaStatus.visibility = View.GONE
                val uri = Uri.parse(mediaUriStr)

                if (item.isSticker) {
                    Glide.with(context)
                        .asBitmap()
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(300, 300)
                        .error(android.R.drawable.stat_notify_error)
                        .into(binding.ivMedia)
                } else {
                    Glide.with(context)
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(android.R.drawable.stat_notify_error)
                        .into(binding.ivMedia)
                }

                binding.ivMedia.setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            val mimeType = when {
                                item.fileName.endsWith(".mp4", true) -> "video/*"
                                item.fileName.endsWith(".webp", true) || item.fileName.endsWith(".was", true) -> "image/webp"
                                else -> "image/*"
                            }
                            setDataAndType(uri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(fallbackIntent)
                        } catch (ex: Exception) {}
                    }
                }
            } else {
                binding.ivMedia.visibility = View.GONE
                binding.tvMediaStatus.visibility = View.VISIBLE
                binding.tvMediaStatus.text = "File Missing: ${item.fileName}"
                binding.ivMedia.setOnClickListener(null)
            }
        }

        private fun Int.dpToPx(): Int = (this * binding.root.context.resources.displayMetrics.density).toInt()
    }

    inner class AudioViewHolder(val binding: ItemChatAudioBinding) : RecyclerView.ViewHolder(binding.root) {
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

            if (isPlaying && mediaPlayer != null) {
                binding.seekBarAudio.max = mediaPlayer!!.duration
                binding.seekBarAudio.progress = mediaPlayer!!.currentPosition
                startSeekBarUpdate(binding.seekBarAudio)
            } else {
                binding.seekBarAudio.progress = 0
            }

            binding.btnPlayPause.setOnClickListener {
                if (isPlaying) {
                    stopAudio()
                } else {
                    item.audioUri?.let { uri -> playAudio(uri, this) }
                }
            }

            binding.seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && currentlyPlayingUri == item.audioUri) {
                        mediaPlayer?.seekTo(progress)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun playAudio(uriString: String, holder: AudioViewHolder) {
        stopAudio()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(holder.itemView.context, Uri.parse(uriString))
                prepare()
                start()
                setOnCompletionListener { stopAudio() }
            }
            currentlyPlayingUri = uriString
            holder.binding.seekBarAudio.max = mediaPlayer!!.duration
            startSeekBarUpdate(holder.binding.seekBarAudio)
            this@ChatAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startSeekBarUpdate(seekBar: SeekBar) {
        updateSeekBarRunnable?.let { handler.removeCallbacks(it) }
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        seekBar.progress = it.currentPosition
                        handler.postDelayed(this, 100)
                    }
                }
            }
        }
        handler.post(updateSeekBarRunnable!!)
    }

    private fun stopAudio() {
        updateSeekBarRunnable?.let { handler.removeCallbacks(it) }
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlayingUri = null
        this@ChatAdapter.notifyDataSetChanged()
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

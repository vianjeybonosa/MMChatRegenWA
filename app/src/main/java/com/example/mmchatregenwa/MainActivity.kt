package com.example.mmchatregenwa

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.mmchatregenwa.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ChatViewModel by viewModels()
    
    private val chatAdapter by lazy {
        ChatAdapter { mediaItem ->
            showMediaViewer(mediaItem)
        }
    }
    
    private val galleryAdapter by lazy {
        GalleryAdapter { mediaItem ->
            showMediaViewer(mediaItem)
        }
    }
    
    private val mediaPagerAdapter = MediaPagerAdapter()
    private var allNonStickerMedia: List<ChatMessage.Media> = emptyList()
    private var fullMessageList: List<ChatMessage> = emptyList()
    
    private var chatBubbleJob: Job? = null
    private var galleryBubbleJob: Job? = null

    private val openFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.loadChatFromFolder(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupSearch()
        setupScrollButtons()
        setupMediaViewer()
        setupGallery()
        setupCustomScrollers()
        observeViewModel()

        binding.btnLoad.setOnClickListener {
            openFolderLauncher.launch(null)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.layoutMediaViewer.mediaViewerRoot.isVisible -> {
                        hideMediaViewer()
                    }
                    binding.layoutGallery.galleryRoot.isVisible -> {
                        hideGallery()
                    }
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun setupRecyclerView() {
        binding.rvChat.apply {
            adapter = chatAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val layoutManager = layoutManager as? LinearLayoutManager
                    val firstVisiblePos = layoutManager?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION
                    if (firstVisiblePos != RecyclerView.NO_POSITION) {
                        val item = fullMessageList.getOrNull(firstVisiblePos)
                        if (item != null) {
                            showDateBubble(binding.cvChatDateBubble, binding.tvChatDateBubble, item.timestamp, true)
                            updateCustomScrollerThumb(binding.chatScrollThumb, binding.chatScrollMap, firstVisiblePos, fullMessageList.size)
                        }
                    }
                }
            })
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupScrollButtons() {
        binding.fabTop.setOnClickListener {
            binding.rvChat.scrollToPosition(0)
        }
        binding.fabBottom.setOnClickListener {
            val count = chatAdapter.itemCount
            if (count > 0) {
                binding.rvChat.scrollToPosition(count - 1)
            }
        }
    }

    private fun setupMediaViewer() {
        binding.layoutMediaViewer.vpMedia.adapter = mediaPagerAdapter
        binding.layoutMediaViewer.btnCloseViewer.setOnClickListener {
            hideMediaViewer()
        }
        binding.layoutMediaViewer.vpMedia.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateMediaCounter(position)
            }
        })

        binding.layoutMediaViewer.btnShowInChat.setOnClickListener {
            val currentMedia = allNonStickerMedia.getOrNull(binding.layoutMediaViewer.vpMedia.currentItem)
            currentMedia?.let { media ->
                val chatIndex = fullMessageList.indexOfFirst { it.id == media.id }
                if (chatIndex != -1) {
                    hideMediaViewer()
                    hideGallery()
                    binding.rvChat.scrollToPosition(chatIndex)
                }
            }
        }
    }

    private fun setupGallery() {
        binding.layoutGallery.rvGallery.apply {
            adapter = galleryAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val layoutManager = layoutManager as? androidx.recyclerview.widget.GridLayoutManager
                    val firstVisiblePos = layoutManager?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION
                    if (firstVisiblePos != RecyclerView.NO_POSITION) {
                        val item = allNonStickerMedia.getOrNull(firstVisiblePos)
                        if (item != null) {
                            showDateBubble(binding.layoutGallery.cvGalleryDateBubble, binding.layoutGallery.tvGalleryDateBubble, item.timestamp, false)
                            updateCustomScrollerThumb(binding.layoutGallery.galleryScrollThumb, binding.layoutGallery.galleryScrollMap, firstVisiblePos, allNonStickerMedia.size)
                            updateCustomScrollerThumb(binding.layoutGallery.galleryScrollThumbLeft, binding.layoutGallery.galleryScrollMapLeft, firstVisiblePos, allNonStickerMedia.size)
                        }
                    }
                }
            })
        }
        binding.btnGallery.setOnClickListener {
            showGallery()
        }
        binding.layoutGallery.btnCloseGallery.setOnClickListener {
            hideGallery()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCustomScrollers() {
        // Chat Scroller
        binding.chatScrollMap.setOnTouchListener { _, event ->
            if (fullMessageList.isEmpty()) return@setOnTouchListener false
            handleScrollerTouch(event, binding.chatScrollMap, binding.rvChat, fullMessageList.size)
            true
        }
        
        // Gallery Scrollers (Right and Left)
        binding.layoutGallery.galleryScrollMap.setOnTouchListener { _, event ->
            if (allNonStickerMedia.isEmpty()) return@setOnTouchListener false
            handleScrollerTouch(event, binding.layoutGallery.galleryScrollMap, binding.layoutGallery.rvGallery, allNonStickerMedia.size)
            true
        }
        binding.layoutGallery.galleryScrollMapLeft.setOnTouchListener { _, event ->
            if (allNonStickerMedia.isEmpty()) return@setOnTouchListener false
            handleScrollerTouch(event, binding.layoutGallery.galleryScrollMapLeft, binding.layoutGallery.rvGallery, allNonStickerMedia.size)
            true
        }
    }

    private fun handleScrollerTouch(event: MotionEvent, map: View, recyclerView: RecyclerView, totalItems: Int) {
        if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
            val y = event.y.coerceIn(0f, map.height.toFloat())
            val percent = y / map.height.toFloat()
            val position = (percent * (totalItems - 1)).toInt()
            recyclerView.scrollToPosition(position)
        }
    }

    private fun updateCustomScrollerThumb(thumb: View, map: View, currentPos: Int, totalItems: Int) {
        if (totalItems <= 0 || map.height <= 0) return
        val percent = currentPos.toFloat() / totalItems.toFloat()
        val topMargin = (percent * (map.height - thumb.height)).toInt()
        val params = thumb.layoutParams as ConstraintLayout.LayoutParams
        params.topMargin = topMargin
        thumb.layoutParams = params
    }

    private fun showDateBubble(cardView: View, textView: TextView, timestamp: String, isChat: Boolean) {
        val datePart = timestamp.substringBefore(",")
        val parts = datePart.split("/", ".", "-")
        val displayDate = if (parts.size >= 3) {
            val month = when(parts[1].trim().toIntOrNull() ?: 0) {
                1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
                7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                else -> ""
            }
            "$month ${parts[2].trim()}"
        } else timestamp

        textView.text = displayDate
        cardView.isVisible = true

        if (isChat) {
            chatBubbleJob?.cancel()
            chatBubbleJob = lifecycleScope.launch {
                delay(1500)
                cardView.isVisible = false
            }
        } else {
            galleryBubbleJob?.cancel()
            galleryBubbleJob = lifecycleScope.launch {
                delay(1500)
                cardView.isVisible = false
            }
        }
    }

    private fun showGallery() {
        galleryAdapter.submitList(allNonStickerMedia)
        binding.layoutGallery.galleryRoot.isVisible = true
    }

    private fun hideGallery() {
        binding.layoutGallery.galleryRoot.isVisible = false
    }

    private fun updateMediaCounter(position: Int) {
        if (allNonStickerMedia.isNotEmpty()) {
            binding.layoutMediaViewer.tvMediaCounter.text = getString(R.string.media_counter, position + 1, allNonStickerMedia.size)
        }
    }

    private fun showMediaViewer(clickedItem: ChatMessage.Media) {
        if (clickedItem.isSticker) return

        val position = allNonStickerMedia.indexOfFirst { it.id == clickedItem.id }
        if (position != -1) {
            mediaPagerAdapter.submitList(allNonStickerMedia)
            binding.layoutMediaViewer.vpMedia.setCurrentItem(position, false)
            binding.layoutMediaViewer.mediaViewerRoot.isVisible = true
            updateMediaCounter(position)
        }
    }

    private fun hideMediaViewer() {
        stopCurrentVideo()
        binding.layoutMediaViewer.mediaViewerRoot.isVisible = false
    }

    private fun stopCurrentVideo() {
        val viewPager = binding.layoutMediaViewer.vpMedia
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView
        val holder = recyclerView?.findViewHolderForAdapterPosition(viewPager.currentItem) as? MediaPagerAdapter.MediaViewHolder
        holder?.stopVideo()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.chatMessages.collect { messages ->
                        fullMessageList = messages
                        chatAdapter.submitList(messages)
                        allNonStickerMedia = messages.filter { it is ChatMessage.Media && !it.isSticker }.map { it as ChatMessage.Media }
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading
                    }
                }
            }
        }
    }
}

package com.example.mmchatregenwa

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val parser = ChatParser(application)
    
    private val _allMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @OptIn(FlowPreview::class)
    val chatMessages: StateFlow<List<ChatMessage>> = combine(_allMessages, _searchQuery) { messages, query ->
        if (query.isBlank()) {
            messages
        } else {
            messages.filter { message ->
                when (message) {
                    is ChatMessage.Text -> message.message.contains(query, ignoreCase = true)
                    is ChatMessage.Media -> message.fileName.contains(query, ignoreCase = true)
                    is ChatMessage.Audio -> message.fileName.contains(query, ignoreCase = true)
                    is ChatMessage.System -> message.message.contains(query, ignoreCase = true)
                }
            }
        }
    }.debounce(300)
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadChatFromFolder(folderUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val parsedMessages = parser.parseChatFolder(folderUri)
            _allMessages.value = parsedMessages
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

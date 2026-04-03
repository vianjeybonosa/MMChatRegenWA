package com.example.mmchatregenwa

sealed class ChatMessage {
    abstract val id: String
    abstract val sender: String
    abstract val timestamp: String
    abstract var isMe: Boolean

    data class Text(
        override val id: String,
        override val sender: String,
        override val timestamp: String,
        override var isMe: Boolean = false,
        val message: String
    ) : ChatMessage()

    data class Media(
        override val id: String,
        override val sender: String,
        override val timestamp: String,
        override var isMe: Boolean = false,
        val fileName: String,
        val mediaUri: String?,
        val isSticker: Boolean = false
    ) : ChatMessage()

    data class Audio(
        override val id: String,
        override val sender: String,
        override val timestamp: String,
        override var isMe: Boolean = false,
        val fileName: String,
        val audioUri: String?
    ) : ChatMessage()

    data class System(
        override val id: String,
        override val sender: String = "System",
        override val timestamp: String,
        override var isMe: Boolean = false,
        val message: String
    ) : ChatMessage()
}

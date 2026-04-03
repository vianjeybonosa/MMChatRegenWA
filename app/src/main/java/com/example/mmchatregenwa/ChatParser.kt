package com.example.mmchatregenwa

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class ChatParser(private val context: Context) {

    private val mediaMap = HashMap<String, Uri>()
    private val baseRegex = """^(\d{1,2}/\d{1,2}/\d{2,4},\s\d{1,2}:\d{2}\s?[apAP][mM])\s-\s(.*)$""".toRegex()

    suspend fun parseChatFolder(folderUri: Uri): List<ChatMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<ChatMessage>()
        val rootDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        
        var chatFileDoc: DocumentFile? = null
        val senderCounts = mutableMapOf<String, Int>()

        rootDoc.listFiles().forEach { fileDoc ->
            val name = fileDoc.name ?: return@forEach
            if (fileDoc.isFile) {
                if (name.endsWith(".txt")) {
                    if (chatFileDoc == null || name.contains("_chat") || fileDoc.length() > (chatFileDoc?.length() ?: 0)) {
                        chatFileDoc = fileDoc
                    }
                } else {
                    mediaMap[name] = fileDoc.uri
                }
            }
        }

        val chatUri = chatFileDoc?.uri ?: return@withContext emptyList()

        context.contentResolver.openInputStream(chatUri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                var currentMessage: ChatMessage? = null

                while (line != null) {
                    val baseMatch = baseRegex.find(line)
                    if (baseMatch != null) {
                        currentMessage?.let { messages.add(it) }
                        val timestamp = baseMatch.groupValues[1]
                        val remaining = baseMatch.groupValues[2]

                        if (remaining.contains(": ")) {
                            val splitIndex = remaining.indexOf(": ")
                            val sender = remaining.substring(0, splitIndex)
                            val content = remaining.substring(splitIndex + 2)
                            senderCounts[sender] = (senderCounts[sender] ?: 0) + 1
                            currentMessage = createMessage(timestamp, sender, content)
                        } else {
                            currentMessage = ChatMessage.System(UUID.randomUUID().toString(), "System", timestamp, false, remaining)
                        }
                    } else if (currentMessage is ChatMessage.Text) {
                        currentMessage = currentMessage.copy(message = currentMessage.message + "\n" + line)
                    }
                    line = reader.readLine()
                }
                currentMessage?.let { messages.add(it) }
            }
        }
        
        val myName = senderCounts.maxByOrNull { it.value }?.key
        messages.forEach { it.isMe = it.sender == myName }
        messages
    }

    private fun createMessage(timestamp: String, sender: String, content: String): ChatMessage {
        val id = UUID.randomUUID().toString()
        val trimmedContent = content.trim()
        
        return when {
            trimmedContent.endsWith("(file attached)") -> {
                val fileName = trimmedContent.removeSuffix("(file attached)").trim()
                val uri = mediaMap[fileName]?.toString()
                if (fileName.endsWith(".opus", true)) {
                    ChatMessage.Audio(id, sender, timestamp, false, fileName, uri)
                } else {
                    ChatMessage.Media(id, sender, timestamp, false, fileName, uri, fileName.endsWith(".webp", true) || fileName.endsWith(".was", true))
                }
            }
            trimmedContent.isEmpty() -> ChatMessage.Text(id, sender, timestamp, false, "📞 WhatsApp Call / Event")
            else -> ChatMessage.Text(id, sender, timestamp, false, trimmedContent)
        }
    }
}

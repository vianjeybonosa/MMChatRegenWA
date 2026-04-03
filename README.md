MMChatRegenWA - High-Performance WhatsApp Chat Explorer
MMChatRegenWA is a lightweight, high-performance WhatsApp chat viewer for Android. Built with Kotlin and MVVM, it is designed to handle massive chat logs (years of history) and heavy media without memory crashes or UI lag.
🚀 Key Features
🛠 Core Performance
•
Text-First Lazy Parsing: Uses BufferedReader to read logs line-by-line, populating the UI instantly while media paths are resolved in the background.
•
RAM Optimization: Media is only loaded when it scrolls into view using Glide. High-res images are automatically downsampled to save memory.
•
Async Transcoding: Automatically generates static PNG previews for .webp and .was stickers to ensure smooth scrolling and prevent decoding crashes.
📱 WhatsApp-Style UI
•
Contextual Bubbles: WhatsApp-accurate colors and alignment (Me vs. Receiver).
•
System Message Support: End-to-end encryption notices and date changes are rendered as centered labels.
•
Interactive Media: Tap any image, video, or sticker to open it in a built-in full-screen viewer.
•
Voice Note Player: Fully integrated support for .opus files with real-time SeekBars (sliders) and playback controls.
🔍 Advanced Navigation
•
Custom Scroll Map: A slim, translucent side-bar that acts like a "minimap." Tap or drag anywhere on it to jump through months of history instantly.
•
Floating Date Bubbles: See exactly which month and year you are viewing while fast-scrolling.
•
Media Gallery Grid: A dedicated "Media" section showing all chat images and videos in a 3-column grid.
•
"Show in Chat": Found an old photo in the gallery? Jump directly to its exact location in the conversation with one tap.
•
Debounced Search: Search through thousands of messages with real-time results that don't block the main thread.
🛠 Tech Stack
•
Language: Kotlin
•
Architecture: MVVM (Model-View-ViewModel)
•
Reactive UI: StateFlow & SharedFlow
•
Concurrency: Kotlin Coroutines (Dispatchers.IO for file parsing)
•
Image Loading: Glide (with Disk Caching & Custom Transcoders)
•
Storage: Android Storage Access Framework (SAF) for Scoped Storage compliance.
📦 How to Use
1.
Export Chat: In WhatsApp, open a chat -> Tap Three Dots -> More -> Export Chat -> Include Media.
2.
Prepare Folder: Unzip the exported .zip file into a folder on your phone's internal storage (e.g., Downloads/MyChat).
3.
Launch App: Open MMChatRegenWA and tap Load Chat.
4.
Select Folder: Use the system folder picker to select the folder containing your _chat.txt and media files.
5.
Enjoy: Navigate your history with the fast-scroll handle or browse the media gallery.
⚠️ Media Support Note
•
Images/Videos: Full native support (.jpg, .png, .mp4).
•
Stickers: .webp and .was files show a static preview in chat and animate when opened in the standalone viewer.
•
Voice Notes: .opus and .m4a formats are supported natively via the integrated player.
Developed with a focus on efficiency and stability for power users with 100k+ message histories.

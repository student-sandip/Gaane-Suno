# 🎵 GaaneSuno – Smart Android Music Player (Offline + Online)

**GaaneSuno** is a modern, feature-rich Android music player that seamlessly blends **offline playback** with **online streaming** — all inside one elegant, responsive, and minimal design.  
It offers a **buttery-smooth UI**, **powerful controls**, and **real-time synchronization** between different activities, ensuring an effortless and enjoyable listening experience.  

---

## ✨ Key Features

### 🎶 **Offline Music Player**
- 🎧 Smooth **Play / Pause / Next / Previous** controls  
- ⏩ Stylish **SeekBar** with animated progress  
- 🔁 **Repeat & Shuffle** modes  
- 🕓 Built-in **Sleep Timer**  
- 📱 Fully **responsive UI** with adaptive layouts  
- 🧑‍🎤 Dynamic **Artist & Song Info** with smooth marquee animation  
- 🔊 Integrated **Volume Control** (with mute/max toggle)  
- ⏱ Real-time **progress & total duration** display  
- 🧭 **Inter-activity Sync** between `MainActivity` and `PlayerActivity`  
- 📤 Uses **BroadcastReceiver** for live updates between service and UI  

---

### 🌐 **Online Music Streaming**
- 🌎 Fetches trending tracks from **iTunes API** (Bollywood, Hindi, Bengali, Pop, English, etc.)  
- 🎨 Beautiful **album artwork previews** with Glide  
- 💡 **Highlight system** for currently playing song (even after refresh!)  
- ⏳ **Smooth delayed refresh** to prevent UI flickering  
- 🔄 Auto-refresh online song list with **5-sec delay** for better user experience  
- 🎵 Fully integrated **OnlinePlayerActivity** with streaming controls  
- 🧭 Syncs with **CURRENT_SONG SharedPreferences** for seamless resume  
- 🧠 **Smart caching** logic to keep your last-played song highlighted  
- ❤️ Add or Remove from **Favorites** with confirmation alerts  

---

### 💖 **Favorites Section**
- ❤️ Dedicated **FavoritesActivity** showing user-saved songs  
- 🗑 Option to **remove songs** with confirmation dialog  
- 🧩 Favorite icons update dynamically across activities  

---

### ⚙️ **Smart Settings**
- Fully responsive **SettingsActivity** to manage app preferences  
- Custom **themes, sound, and UI** options coming soon  

---

### 🔍 **Instant Search**
- Lightning-fast search powered by clean filtering logic  
- Supports both offline and online tracks  
- Beautiful search transition animations  

---

### 🧭 **Navigation & UX**
- Bottom Navigation for easy switching between:
  - 🎵 **Offline Music**
  - 🌐 **Online Music**
  - ❤️ **Favorites**
- 🎬 **Custom Activity Transitions** with fade and slide animations  
- ✨ **TypeWriter Splash Screen** for a dynamic startup experience  

---

<p align="center"> <!-- 1st Row --> <img src="https://github.com/user-attachments/assets/fab55a57-49dc-4c2b-9dff-f814a6e4eccc" width="22%" /> <img src="https://github.com/user-attachments/assets/61e62f91-d5af-46a7-a0a4-4ae804020b92" width="22%" /> <img src="https://github.com/user-attachments/assets/0431bc0d-fe80-4ff7-8a92-8a5c211aa4c0" width="22%" /> <img src="https://github.com/user-attachments/assets/68b26539-f23b-4267-ae44-f7ce28f5f0ed" width="22%" /> </p> <p align="center"> <!-- 2nd Row --> <img src="https://github.com/user-attachments/assets/c2156470-932a-4420-be59-5b32cc928f4d" width="22%" /> <img src="https://github.com/user-attachments/assets/3cab9da7-d853-4a13-8843-8b843d7595b5" width="22%" /> <img src="https://github.com/user-attachments/assets/4ba5db45-4f63-41e0-b171-ac4665330dd0" width="22%" /> <img src="https://github.com/user-attachments/assets/5a5c255a-64a8-44f0-bef3-2893e905fff2" width="22%" /> </p>
**Theres also many more functionalities in the app,,, once you visit you will see those all features.**

---

## 🛠️ Built With

| Technology | Purpose |
|-------------|----------|
| **Java** | Core logic and app architecture |
| **Android SDK** | UI and system integration |
| **MediaPlayer + Service** | Music playback handling |
| **BroadcastReceiver** | Live playback communication |
| **ConstraintLayout + LinearLayout** | Responsive design |
| **Glide** | Image loading & caching |
| **Material Design Components** | UI consistency |
| **sdp / ssp library** | Adaptive sizing across devices |

---

## 🧠 Architecture Highlights

- 🎶 **MusicService** handles all background playback  
- 🎧 **MediaSessionCompat** for notification and Bluetooth media control  
- 🌐 **OnlineActivity** fetches and manages iTunes streaming data  
- 📀 **PlayerActivity** syncs seamlessly with both local & remote sources  
- 💾 **SharedPreferences** store the current song state for quick resume  
- 🧩 **RecyclerView + Custom Adapter** manage both offline and online song lists  

---

## 🚀 Future Enhancements

- 🌙 Dark / Light Theme Toggle  
- 📶 Offline Caching for Online Songs  
- 🧠 AI-based “Smart Recommendations”  
- 💬 Lyrics Integration  
- 📱 Play Store Release (coming soon!)  

---

## 🙏 Thanks for Visiting!

Thank you for checking out **GaaneSuno**!  
It’s more than just a music player — it’s a blend of art, technology, and user love ❤️  

💻 **Developed with passion by [Sandip Saha](https://student-sandip.github.io/MyFolio/)**
If you liked this project, don’t forget to ⭐ **star the repo** and share your feedback!  

> “Music is not just heard, it’s *felt* — and GaaneSuno brings that feeling alive.” 🎶  

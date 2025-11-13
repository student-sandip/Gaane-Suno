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

## 🖼️ Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/167765f1-833c-47f0-9832-a9f124e7f28a" width="22%" />
  <img src="https://github.com/user-attachments/assets/9124286e-3777-46f3-8d95-e3be08767281" width="22%" />
  <img src="https://github.com/user-attachments/assets/613cf586-d894-4d65-9921-d55c9dde11d1" width="22%" />
  <img src="https://github.com/user-attachments/assets/954eaa24-2973-4ed8-bac0-b997515535a4" width="22%" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/5e28c7db-4a23-4350-9d5d-d1de6b069dca" width="22%" />
  <img src="https://github.com/user-attachments/assets/61fdd1e6-b15b-4f5a-a472-e864bcdc80aa" width="22%" />
  <img src="https://github.com/user-attachments/assets/096c0d83-304a-4051-bdf4-3954ae3ec27b" width="22%" />
  <img src="https://github.com/user-attachments/assets/96aab8aa-8d74-4bb3-97e3-319f7df4218f" width="22%" />
</p>


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

🎧 **Developed with passion by [Sandip Saha](#)**  
If you liked this project, don’t forget to ⭐ **star the repo** and share your feedback!  

> “Music is not just heard, it’s *felt* — and GaaneSuno brings that feeling alive.” 🎶  

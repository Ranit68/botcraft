# BotCraft 🤖

BotCraft is a sophisticated Android application designed to provide users with an immersive AI chat experience. Discover, interact, and build connections with a diverse range of AI personalities tailored to different moods and styles.

## 🚀 Features

- **Discover AI Bots**: Explore a vast collection of bots categorized by personality (Popular, Bold, Professional, Friendly, Romantic, and more).
- **Advanced Filtering**: Filter bots by gender, category, or use the real-time search to find the perfect companion.
- **Personalized Experience**: The app adapts to your preferences, allowing you to block bots and follow your favorites.
- **Dynamic Trending Section**: Stay updated with the most popular bots in the community.
- **Rich Media Integration**: High-quality image rendering with Glide, PhotoView for zooming, and smooth Lottie animations.
- **Seamless Chat**: Engaging chat interface with real-time updates powered by Firebase.
- **Monetization & Ads**: Integrated Google Play Billing and AdMob for a sustainable ecosystem.

## 🛠 Tech Stack

- **Language**: Java / Android SDK
- **Backend**: 
    - Firebase Firestore (Database)
    - Firebase Auth (Authentication)
    - Firebase Storage (Media Hosting)
    - Firebase Realtime Database
- **Networking**: Retrofit 2 & OkHttp 5
- **UI/UX**: 
    - Material Design Components (MDC)
    - Lottie Animations
    - Glide (Image Loading)
    - CircleImageView & PhotoView
- **Architecture**: Modular and responsive design with Skeleton loading screens.
- **Other Tools**: Google Play Billing API, Google AdMob SDK, GSON.

## 📸 Screenshots

| Home Screen | Chat Interface | Bot Discovery |
| :---: | :---: | :---: |
| ![Home](https://via.placeholder.com/200x400?text=Home+Screen) | ![Chat](https://via.placeholder.com/200x400?text=Chat+Interface) | ![Discover](https://via.placeholder.com/200x400?text=Bot+Discovery) |

## ⚙️ Installation & Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Ranit68/botcraft.git
   ```
2. **Firebase Setup**:
   - Create a new project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with the package name `com.ranit.botscraft`.
   - Download the `google-services.json` and place it in the `app/` directory.
   - Enable Email/Google Auth, Firestore, and Realtime Database.
3. **Open in Android Studio**:
   - Sync Project with Gradle Files.
   - Run the app on an emulator or physical device.

## 📂 Project Structure

- `ui/`: Contains Fragments and Activities (Home, Chat, Profile).
- `model/`: Data models for Bots, Users, and Conversations.
- `adapter/`: RecyclerView adapters for featured and discovery lists.
- `manager/`: Firebase and singleton utility classes.

## 🤝 Contributing

Contributions are welcome! If you'd like to improve BotCraft, please follow these steps:
1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---
*Developed with ❤️ by [Ranit](https://github.com/Ranit68)*

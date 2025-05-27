# Emotion Music Recommender App

An Android application that provides personalized music recommendations based on real-time facial emotion detection. The app uses machine learning to analyze facial expressions and suggests music that matches the user's emotional state.

![App Screenshot](App_Screenshot(1).png)

## Features

- Real-time facial emotion detection
- Personalized music recommendations
- Firebase Cloud Storage integration for music streaming
- History tracking of detected emotions
- Music player with playback controls
- Offline support for saved music

## Tech Stack

- **Language**: Kotlin
- **Platform**: Android (minimum SDK 23)
- **Architecture**: MVVM
- **Dependencies**:
  - TensorFlow Lite for emotion detection
  - Firebase Firestore & Cloud Storage
  - ML Kit Face Detection
  - AndroidX Navigation Components
  - Glide for image loading
  - Coroutines for async operations

## Installation

1. Clone the repository:
```sh
git clone https://github.com/yourusername/Emotion-Music-Recommender-App.git
```

2. Open the project in Android Studio

3. Add your `google-services.json` file to the `app` directory

4. Build and run the application:
```sh
./gradlew assembleDebug
```

## Configuration

1. Set up Firebase project:
   - Create a new Firebase project
   - Enable Firestore Database
   - Enable Cloud Storage
   - Download and add `google-services.json`

2. Configure the app in `build.gradle.kts`:
```kotlin
defaultConfig {
    applicationId = "com.ebmr.myapplication1"
    minSdk = 23
    targetSdk = 34
    versionCode = 5
    versionName = "1.5"
}
```

## Usage

1. Launch the app
2. Grant camera permissions when prompted
3. Take or select a photo
4. The app will detect your emotion and recommend appropriate music
5. Play, pause, or skip songs from the music player interface
6. View your emotion detection history in the gallery

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/YourFeature`
3. Commit changes: `git commit -am 'Add YourFeature'`
4. Push to branch: `git push origin feature/YourFeature`
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Firebase team for cloud infrastructure
- TensorFlow team for ML tools
- Android development community for support and resources

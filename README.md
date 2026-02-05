# Android Gallery App

This project is a Kotlin + Jetpack Compose gallery app that displays all photos on the device in a fast,
scrollable grid. Images are loaded from MediaStore using coroutines and shown as cached, down-
sampled thumbnails for performance. Tapping a photo opens a full-screen viewer with higher resolution
and pinch-to-zoom. The app supports rotation, updates when media changes, and handles large images
efficiently without third-party image libraries. Built for Android SDK 26–36.

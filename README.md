<div align="center">
  <img src="app/src/main/assets/logo-rounded.png" width="128" alt="UseMemos Logo" />
  <h1>UseMemos Android Client</h1>
</div>

A high-performance, native Android application designed to mirror the [Memos](https://usememos.com/) web experience. This client prioritizes an **Offline-First Architecture**, leveraging native Android capabilities to ensure you can capture your thoughts anytime, anywhere.

## Key Features

- **Jetpack Compose UI**: Built entirely with modern declarative UI and Material 3 components.
- **Offline-First "Quick Capture"**: Writes memos directly to a local SQLite database (via Room) instantly. 
- **Resilient Background Sync**: Uses Android `WorkManager` alongside Retrofit/Ktor to sync local changes to your backend automatically.

## Architecture Overview

The app follows strict **Clean Architecture** principles:
- **Data Layer**: Room Database (Local) + Retrofit (Remote API).
- **Domain Layer**: Repository pattern mapping local-first logic to API responses.
- **UI Layer**: Unidirectional Data Flow utilizing StateHoisting in Composable screens.

---

## How to Build It Yourself

For complete, step-by-step instructions, including setting up your local environment, deploying the app directly to a connected phone via ADB, and configuring cryptographic keys for secure production **Release Builds**—please follow the dedicated guide:

**[Read the Full `BUILD.md` Guide Here](BUILD.md)**

---

# Offline Notes App 📝

A simple, private, offline-first notes application for Android, built as a lightweight alternative to cloud-based services like Google Keep.

> **Core Philosophy:** Your notes are yours. This app is 100% offline. It has **no internet permission**, no cloud syncing, and no servers. All your data lives and dies on your device.

---

## ✨ Features

* **Create, Read, Update, Delete (CRUD):** A full-featured notes editor to create new notes, read existing ones, update them, or delete them.
* **100% Offline Storage:** All notes are saved locally using a robust Room database.
* **Persistent Data:** Your notes are saved instantly and persist even after you close the app or restart your device.
* **Clean, Responsive UI:** A minimalistic Material Design layout using a staggered grid (like Google Keep) for easy viewing.
* **Instant Search:** Quickly filter and find your notes by title or content.
* **Automatic Timestamps:** Each note automatically stores its creation and last-modified times.

---

## 🛠️ Tech Stack & Architecture

This project follows modern Android development best practices, emphasizing a clean and scalable architecture.

* **Language:** **Kotlin**
* **Architecture:** **MVVM (Model-View-ViewModel)**
    * **Model:** The `Note` entity and the `NoteRepository` abstracting the data source.
    * **View:** `MainActivity` hosting `NoteListFragment` and `NoteEditorFragment`.
    * **ViewModel:** `NoteViewModel` acts as the bridge, holding and processing UI-related data, surviving configuration changes.
* **Database:** **Room Database** (part of Jetpack) for efficient and reliable local SQLite storage.
* **Asynchronous:** **Kotlin Coroutines** and **LiveData** for handling background database operations and reactively updating the UI.
* **Navigation:** **Jetpack Navigation Component** to manage fragment transactions and argument passing.
* **UI:** **Android XML Layouts** with `RecyclerView` (using `StaggeredGridLayoutManager`) and `MaterialCardView`.

---

## 🚀 How to Build

You can build this project using Android Studio:

1.  **Clone** the repository.
```bash
git clone https://github.com/wakodepranav2005-git/offline_notes.git
```
2.  **Open** the `offline_notes` directory in Android Studio.
3.  Let Gradle sync all the dependencies.
4.  **Run** the app on an emulator or a physical Android device (Android 8.0+).

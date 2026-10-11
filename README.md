# YaadRakh ⏰ — Your Offline Personal Reminder & Routine Assistant

**YaadRakh** is a modern, offline-first Android application built using Kotlin, XML, MVVM Architecture, and Room Database. It helps users manage personal reminders, daily routines, spoken voice announcements, and family profiles without needing an internet connection.

---

## 🌟 Key Features

### 1. ⚡ 100% Offline-First
- Works completely without Wi-Fi or Mobile Data.
- All reminder data, routines, history, and settings are stored locally on the device using **Room Database (SQLite)** and `SharedPreferences`.
- Zero cloud backend, zero internet permissions required.

### 2. ⏰ Exact Alarm & Smart Notifications
- Uses system **AlarmManager** for pinpoint reminder scheduling.
- **BroadcastReceiver** handles alarms even when the app is closed.
- **BootReceiver** automatically restores all scheduled alarms when the phone restarts.
- High-priority interactive notifications with **✓ DONE** and **Snooze (10M / 30M)** quick actions.

### 3. 🔊 Spoken Text-to-Speech (TTS) Voice Reminders
- **Human-like Polite Speech**: Constructing polite announcements in **Hindi** and **English**.
- **Custom Repeat Count**: Choose how many times the voice announcement repeats (1x, 2x, 3x, or 5x) with natural pauses.
- **Character Tones & Speed**: Customize pitch and speed (Natural Warm, Clear High, Deep).
- **Elderly-Friendly Voice**: Respectful tone tailored for older adults.

### 4. 🌅 Daily Routines & Spoken Morning Briefing
- Manage recurring habits (e.g., Morning Walk, Exercise, Reading, Medicine).
- Optional **Spoken Morning Briefing** that speaks an offline TTS summary of your day's schedule every morning.

### 5. 📊 History & Daily Progress Tracking
- **Progress Card**: Real-time daily progress bar calculated from completed vs. missed reminders.
- **Activity History**: Complete searchable history categorized by date (*Today*, *Yesterday*, *Older*).
- **Filter & Search**: Filter history by status (*Completed*, *Missed*, *Snoozed*) or search by title and category.

### 6. 👨‍👩‍👧 Local Family Profiles
- Associate reminders with specific family members (e.g., *Self*, *Father*, *Mother*, *Children*).
- Filter reminders by person on the **My Reminders** screen.

### 7. 👴 Elderly-Friendly UI
- Toggle **Elderly Mode** in Settings for enlarged fonts, bigger touch targets, and simplified navigation.

---

## 🏗️ Architecture & Technology Stack

- **Language**: Kotlin
- **UI Framework**: XML Views & Material Design 3 (ViewBinding)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Database**: Room Database (with KSP annotation processing & database migrations)
- **Concurrency**: Kotlin Coroutines & Flow / LiveData
- **Scheduling**: `AlarmManager` with `setExactAndAllowWhileIdle`
- **System Events**: `BroadcastReceiver` & `BOOT_COMPLETED` handling
- **Text-to-Speech**: Android `TextToSpeech` API with custom language Fallbacks

---

## 📂 Project Structure

```text
com.example.yaadrakh
│
├── alarm/
│   └── AlarmScheduler.kt             # Centralized AlarmManager scheduling
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt            # Room Database Singleton & Migrations
│   │   ├── FamilyProfileEntity.kt    # Local family profile schema
│   │   ├── ReminderDao.kt            # Room Queries & DAOs
│   │   ├── ReminderEntity.kt         # Main reminder schema
│   │   ├── ReminderHistoryEntity.kt  # Occurrence history schema
│   │   ├── RoutineEntity.kt          # Daily routines schema
│   │   └── RoutineOccurrenceEntity.kt# Routine occurrence status schema
│   └── repository/
│       └── ReminderRepository.kt     # Single Source of Truth
│
├── receiver/
│   ├── BootReceiver.kt               # Restores alarms on device reboot
│   └── ReminderReceiver.kt           # Handles alarm broadcasts & notifications
│
├── ui/
│   ├── auth/
│   │   └── NameSetupDialogFragment.kt # First-time profile name setup
│   ├── family/                       # Family profile dialogs & adapters
│   ├── history/                      # History list, filters & search
│   ├── home/                         # Dashboard, progress card & today list
│   ├── reminder/                     # Add/Edit form & details view
│   ├── reminders/                    # Search, category & person filters
│   ├── routine/                      # Routines management
│   └── settings/                     # Voice, language, elderly mode settings
│
├── utils/
│   ├── DateTimeUtils.kt              # Date/time & category emoji helpers
│   ├── NotificationHelper.kt         # System Notification Channels & Builders
│   ├── VoiceReminderHelper.kt        # Spoken TTS synthesis & repeat engine
│   └── VoiceSettings.kt              # Local SharedPreferences manager
│
└── viewmodel/
    └── ReminderViewModel.kt          # ViewModel providing LiveData to UI
```

---

## 🚀 Setup & Installation

1. **Prerequisites**:
   - Android Studio Ladybug (or newer)
   - JDK 11 or higher
   - Android Device / Emulator running Android 8.0 (API Level 26) or higher

2. **Clone / Open Project**:
   ```bash
   git clone https://github.com/your-username/YaadRakh.git
   ```
   Open the `YaadRakh` directory in **Android Studio**.

3. **Build & Run**:
   - Allow Gradle to sync dependencies.
   - Select your connected Android device or emulator.
   - Click **Run** (`Shift + F10` / `Cmd + R`).

---

## 🔒 Permissions Used

- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: Required for precise alarm delivery at scheduled times.
- `POST_NOTIFICATIONS`: Android 13+ permission for showing reminder alert banners.
- `RECEIVE_BOOT_COMPLETED`: Restores active alarms automatically after device restarts.
- `VIBRATE`: Provides haptic feedback on reminder alerts.

---

## 📄 License

This project is licensed under the **MIT License**. Feel free to use and modify it.

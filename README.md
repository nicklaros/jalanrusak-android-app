# Jalan Rusak Android App

Phase 1 Quick Report MVP implementation for the Jalan Rusak (Road Damage Report) app.

## Project Overview

This is a native Android application built with Kotlin that allows users to submit road damage reports with a single tap from their home screen widget.

## Phase 1 Features (Current Implementation)

### ✅ Implemented
- **Home Screen Widget** - "Lapor Cepat" widget for one-tap report submission
- **User Authentication** - Email/password login with secure token storage
- **GPS Location Capture** - Automatic location capture using Play Services
- **Quick Report Submission** - Submit reports with GPS coordinates and auto-generated title
- **Overlay Popup** - Visual feedback during report submission (locating → submitting → success)
- **Success Notifications** - Confirmation after successful report submission
- **Error Handling** - Graceful handling of no GPS, no internet, auth errors

### Architecture

```
┌─────────────────────────────────────────────┐
│              Home Screen Widget              │
│  (QuickReportWidget - One-tap submission)   │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│              Overlay Activity                │
│     (QuickReportOverlay - Visual feedback)  │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│          Background Service                  │
│   (QuickReportService - GPS + API call)     │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│               API Layer                      │
│    (ApiClient - Retrofit to backend API)     │
└─────────────────────────────────────────────┘
```

## Tech Stack

- **Kotlin** - Primary language
- **Retrofit** - HTTP client for API calls
- **Coroutines** - Asynchronous programming
- **DataStore** - Secure token storage
- **Play Services** - Location services
- **Material Design** - UI components
- **ViewModel** - MVVM architecture pattern

## API Integration

**Base URL:** `https://api.jalanrusak.com/api/v1`

**Endpoints Used:**
- `POST /auth/login` - User authentication
- `POST /damaged-roads` - Create road damage report

## Project Structure

```
app/src/main/
├── java/com/jalanrusak/
│   ├── data/                  # Data layer
│   │   ├── api/              # API client and DTOs
│   │   ├── repository/       # Data repositories
│   │   └── local/           # Local storage (DataStore)
│   ├── domain/               # Business logic
│   │   └── usecase/         # Use cases (Login, QuickReport)
│   ├── ui/                   # UI layer
│   │   ├── login/           # Login screen
│   │   ├── overlay/         # Quick report overlay
│   │   └── widget/          # Home screen widget
│   ├── service/             # Background services
│   └── util/                # Utilities (Result wrapper)
└── res/                      # Resources
    ├── drawable/            # Icons and backgrounds
    ├── layout/              # UI layouts
    ├── values/              # Strings, colors, themes
    └── xml/                 # Widget and backup configs
```

## Building the Project

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Build Steps
1. Clone this repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build APK: `./gradlew assembleDebug`
5. Install on device: `adb install app/build/outputs/apk/debug/app-debug.apk`

## Usage

### First Time Setup
1. Install the app
2. Open the app to see the login screen
3. Login with your Jalan Rusak credentials

### Adding Widget
1. Long press on home screen
2. Select "Widgets"
3. Find "Jalan Rusak"
4. Drag "Lapor Cepat" to home screen

### Submitting Quick Report
1. Tap the widget
2. App captures GPS location automatically
3. Overlay shows "Mendapatkan lokasi..." → "Mengirim laporan..."
4. Success notification appears
5. Report is created on the backend

## Permissions Required

- `INTERNET` - API calls
- `ACCESS_FINE_LOCATION` - GPS capture
- `ACCESS_COARSE_LOCATION` - Approximate location
- `POST_NOTIFICATIONS` - Success notifications
- `SYSTEM_ALERT_WINDOW` - Overlay popup

## Error Handling

The app handles these scenarios:

| Scenario | Handling |
|----------|----------|
| No GPS permission | Prompts user to enable |
| No internet connection | Shows error notification |
| Not logged in | Prompts user to login |
| GPS timeout | Shows error with retry option |
| API error | Shows error message |
| Auth token expired | Shows login prompt |

## Dependencies

See [app/build.gradle.kts](app/build.gradle.kts) for full list. Key dependencies:

```kotlin
// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Location
implementation("com.google.android.gms:play-services-location:21.0.1")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## Future Phases

### Phase 2: Authentication Expansion
- User registration
- Forgot password
- Profile management

### Phase 3: Dashboard
- Report list view
- Report details
- Top damaged areas

### Phase 4: Full Report Creation
- Photo upload
- Map picker for location
- Full form with all fields

### Phase 5: Polish
- Offline mode
- Push notifications
- Settings screen

## Development Notes

### Quick Report Flow
1. Widget tap → `QuickReportWidget.onReceive()`
2. Starts `QuickReportOverlay` activity
3. Starts `QuickReportService` in background
4. Service captures GPS via `LocationManager`
5. Service submits report via `ReportRepository`
6. State updates flow to overlay via StateFlow
7. Overlay shows progress and closes on completion

### Authentication Flow
1. User enters credentials in `LoginActivity`
2. `LoginViewModel` calls `LoginUseCase`
3. `AuthRepository` calls login API
4. Tokens stored in `TokenManager` (DataStore)
5. Subsequent API calls include Bearer token

## License

MIT License - See LICENSE file for details

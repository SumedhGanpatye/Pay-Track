# Pay&Track

Private, offline personal expense tracker for Android — Kotlin, Jetpack Compose, Material 3, Room.

## Features

- **Pay with UPI** — enter amount / category / note, copy amount, open GPay / PhonePe / Paytm / BHIM, confirm after paying
- **Quick Add** — log expenses manually from Home
- **Analytics** — category spend charts and period averages
- **Settings** — profile, UPI defaults, export CSV, clear data, logout

## Open in Android Studio

1. **File → Open** → select this folder  
2. Let Gradle sync  
3. Run on a device or emulator (API 26+)

## Architecture

| Layer | Responsibility |
|-------|----------------|
| `data/` | Room entity, DAO, database, repository |
| `domain/` | UPI app launch + clipboard, preferences, profile, categories |
| `ui/screens/` | Home, Analysis, Settings, account signup |
| `ui/screens/scanpay/` | Pay with UPI (payment details) |
| `service/` | Expense-recorded system notifications |

## Privacy

- User-initiated actions only — no Accessibility Service / Notification Listener  
- No overlay / third-party app monitoring  
- Expense data stays in on-device Room SQLite  

## Build

```bash
.\gradlew.bat :app:assembleRelease
```

Release APK is size-optimized (R8, resource shrink, arm ABIs only).

## Notes

- Dark charcoal + emerald theme  
- App display name: **Pay&Track**

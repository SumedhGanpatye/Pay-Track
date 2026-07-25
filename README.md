# Money Tracker

Private, 100% offline personal expense tracker built with Kotlin, Jetpack Compose, Material Design 3, and Room SQLite.

## Open in Android Studio

1. **File → Open** → select this folder
2. Let Gradle sync
3. Run on an emulator or device (API 26+)

## Architecture

| Layer | Responsibility |
|-------|----------------|
| `data/` | Room entity, DAO, database, repository |
| `ui/screens/` | Home, Analysis, Settings (MVVM) |
| `ui/screens/scanpay/` | Scan & Pay navigation placeholders (not implemented yet) |
| `ui/navigation/` | Compose NavHost + nested Scan & Pay graph |
| `ui/components/` | Reusable Material 3 widgets |
| `service/` | Local expense-added notifications only |

## Privacy

- User-initiated actions only — no Accessibility Service
- No overlay / `SYSTEM_ALERT_WINDOW`
- No monitoring of third-party apps
- No notification parsing or background tracking
- All expense data stays in on-device Room SQLite

## Upcoming: Scan & Pay

Planned in-app flow (placeholders already wired in navigation):

Home → Scan & Pay → Camera → Merchant → Amount → Category → Note → Choose UPI → Store expense

## Notes

- Dark theme is the default (deep charcoal + neon teal accents)
- No cloud SDKs or network permissions

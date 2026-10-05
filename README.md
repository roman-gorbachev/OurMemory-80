# OurMemory for Android

An app about a historical military cemetery in Minsk: who is buried there, where each grave is and the stories behind the names. There is also a SwiftUI version for iOS: [OurMemory-ios](https://github.com/roman-gor/OurMemory-ios). Both apps share one Firebase database, so the content, memorial candles and visitor submissions are the same in both. Editors can also manage the content in the browser with the [web admin panel](https://ourmemory-admin.web.app).

The project started as a college assignment in mobile development and the digitization of historical data.

## Features

- **Veterans.** A list with search by name and a category filter. Each veteran's page has a biography, awards, photos, an audio biography, dates of birth and death, and the burial place.
- **Map.** Burials on Yandex MapKit with clustering, your location, and audio tours by stop.
- **Remembrance.** Light a candle (one counter shared by all visitors), add veterans to favorites (synced when signed in with Google), and get reminders for May 9 and anniversaries of favorite veterans.
- **QR codes.** The QR code on a grave plaque opens the veteran's page; app links `https://chatroom-85fb8.web.app/veteran/{id}` work too.
- **Visitors.** Relatives can send memories and photos, and anyone can leave feedback or report a mistake. Admin replies show up under "My requests". Before sending, text is checked for profanity and photos for explicit content (LiteRT).
- **Administration.** Editors for veterans, burials and tours with media uploads, moderation of submissions, and feedback review. A super admin adds administrators by e-mail. The same tools are available on the web in the admin panel.
- **Languages and appearance.** Russian, Belarusian, English and Chinese. Light and dark themes and adjustable text size.

## Tech stack

- Kotlin, Coroutines and Flow
- Jetpack Compose, Material 3, Navigation Compose
- MVVM with unidirectional state (`UiState` + `UiIntent`)
- Hilt (KSP) for dependency injection
- Firebase Auth, Realtime Database, Storage; Google sign-in through Credential Manager
- Yandex MapKit
- Media3 ExoPlayer for audio
- Coil for images, Retrofit for direct Yandex Disk links
- DataStore for local settings, WorkManager for reminders
- Google Code Scanner for QR codes, LiteRT for photo checks
- Detekt for static analysis

## Architecture

A single `:app` module, package `com.gorman.ourmemoryapp`:

- `data/` holds data sources and repositories per area (veterans, burials, tours, candles, submissions and so on).
- `domain/` holds models and repository interfaces.
- `di/` holds the Hilt modules.
- `ui/` holds screens per feature, each with `models/`, `ui/` and `viewmodels/`.
- `reminders/` holds the WorkManager jobs for reminders.

All data lives in Firebase Realtime Database under the `OurMemory` node. The node layout and access rules are described in [CLAUDE.md](CLAUDE.md).

Storage shares one bucket and one rules file with Green Passport. [`firebase/storage.rules`](firebase/storage.rules) is a copy of the full file; it is deployed from the Green Passport Android repository (`storage.rules`), and the two must stay identical. New admin uids go into `isOurMemoryAdmin()` in both.

## Building

You need Android Studio (JDK 17) and the Android SDK. `minSdk` is 28.

1. Put your Yandex MapKit key in `local.properties`:
   ```properties
   MAPKIT_API_KEY=your-key
   ```
2. Firebase is configured by `app/google-services.json`. For Google sign-in, add the SHA-1 of your signing key to the Firebase project settings.
3. Build and install:
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

The release build (`./gradlew assembleRelease`) is signed when the repo root has a `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`).

Checks and tests:

```bash
./gradlew detektAll
./gradlew testDebugUnitTest
```

## Tools

- `tools/qr/generate_qr_sheet.py` generates a PDF of QR cards from the database.
- `tools/nsfw/convert_model.py` rebuilds the TFLite model used for photo checks.
- `firebase/` holds the hosting for app links (`assetlinks.json`) and the veteran web page.

## License

All rights reserved. The code is published for viewing only; see [LICENSE](LICENSE).

## Author

Roman Gorbachev

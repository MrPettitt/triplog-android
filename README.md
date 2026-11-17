# TripLog

**An offline-first Android app for logging work trips and fuel expenses, with automatic background sync.**

Built with Kotlin, Jetpack Compose, Room, Hilt and WorkManager. Log a trip or a fuel fill-up with no signal, and TripLog uploads it the moment the device is back online.

<p align="center">
  <img src="docs/screenshots/home.png" width="23%" alt="Home dashboard" />
  <img src="docs/screenshots/log-trip.png" width="23%" alt="Log trip form" />
  <img src="docs/screenshots/fuel-expense.png" width="23%" alt="Fuel expense form" />
  <img src="docs/screenshots/all-trips.png" width="23%" alt="All trips list" />
</p>

## Features

- **Trip logging**: start and end location, distance, purpose, date, time and notes. Tap a trip to edit or delete it.
- **Fuel expenses**: date, litres, amount (ZAR), odometer, station and a receipt photo, with a list of past expenses. Tap one to edit or delete it.
- **Offline first**: everything is stored in Room (SQLite) first, so nothing is lost without a connection.
- **Automatic sync**: a sync is requested when you save a record and whenever the network returns, plus a 15-minute periodic sync. Requests are de-duplicated into a single worker. Records show a clear Pending or Synced status, and the home banner says so when no backend is configured.
- **Receipt uploads**: photos are taken with the device camera app (no camera permission needed), shrunk to 1600 px on the long side, and uploaded as multipart. An expense only turns Synced once its receipt is uploaded; a failed upload is retried without creating the expense again.
- **Readable dark UI**: Material 3 with 16sp primary body text. Text and status colours meet WCAG AA contrast (4.5:1) on the app surfaces, including the white label on the primary button (about 5:1).

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM + Repository |
| Local DB | Room |
| DI | Hilt |
| Background work | WorkManager |
| Networking | Retrofit, OkHttp, Moshi |
| Images | Camera capture via `TakePicture`, Coil 3 |
| Testing | JUnit 4, kotlinx-coroutines-test, hand-written fakes |
| CI | GitHub Actions: lint, unit tests, debug build |

## Project structure

```
app/src/main/java/com/triplog/
├── data/
│   ├── local/        # Room: entities, DAOs, database, converters
│   ├── remote/       # Retrofit ApiService and DTOs
│   └── repository/   # TripRepository, FuelExpenseRepository
├── di/               # Hilt modules
├── domain/model/     # Trip, FuelExpense, TripPurpose
├── ui/
│   ├── home/  logtrip/  expenses/  trips/   # Screens + ViewModels
│   ├── navigation/   # NavGraph
│   ├── theme/        # Colours, typography, theme
│   └── Components.kt # Shared composables
├── util/             # ConnectivityObserver, formatters
└── worker/           # SyncWorker
```

## Getting started

**Requirements:** Android Studio Ladybug or newer, JDK 17 or 21 (Gradle 8.10 does not run on newer JDKs), Android device or emulator on API 26+.

```bash
git clone https://github.com/MrPettitt/triplog-android.git
cd triplog-android
./gradlew assembleDebug
```

Or open the folder in Android Studio and press **Run**. Android Studio creates `local.properties` with your SDK path automatically.

### Run the checks

```bash
./gradlew lintDebug testDebugUnitTest
```

The same commands run on every push and pull request through GitHub Actions.

### Configure the backend (optional)

The app works fully offline. To enable sync, point it at your API. Any one of these works, checked in this order:

1. A Gradle property: `./gradlew assembleDebug -PapiBaseUrl=https://api.example.org/api/v1/`
2. A line in `local.properties` (git-ignored): `apiBaseUrl=https://api.example.org/api/v1/`
3. The `API_BASE_URL` environment variable (used by the release workflow, see [docs/RELEASING.md](docs/RELEASING.md)).

Debug builds can use a different server with `apiBaseUrlDebug` (or `API_BASE_URL_DEBUG`), for example `apiBaseUrlDebug=http://10.0.2.2:8080/api/v1/` for a server running on your computer while using the emulator. Debug builds allow plain `http://`; release builds do not. A trailing slash is added if missing, and the build fails early if the value is not an http(s) URL.

To add authentication, attach your token in the OkHttp interceptor in `di/AppModule.kt`.

## Backend API contract

```
POST    /api/v1/trips
PUT     /api/v1/trips/{id}
DELETE  /api/v1/trips/{id}
GET     /api/v1/trips
POST    /api/v1/fuel-expenses
PUT     /api/v1/fuel-expenses/{id}
DELETE  /api/v1/fuel-expenses/{id}
POST    /api/v1/fuel-expenses/{id}/receipt   (multipart)
GET     /api/v1/fuel-expenses
```

Request and response shapes are in `data/remote/dto/Dtos.kt`.

### Retries and duplicates

Every trip and expense gets a random `client_id` when it is created on the device. It is sent in the request body and as an `Idempotency-Key` header on every attempt, so the server can ignore a repeat of a request it already handled (for example when the response was lost). Servers should return the original record for a repeated key.

Responses in the 2xx range count as success. `408`, `429` and `5xx` responses and network errors are retried with backoff. Other `4xx` responses are treated as rejected: the record stays Pending and is tried again on the next sync.

Until `API_BASE_URL` is changed from the placeholder, sync is skipped and the app says so.

### Edits and deletes

Editing a record that is already on the server sends a `PUT` with the same body as the create request. Deleting it sends a `DELETE`. A `404` or `410` on delete counts as success. On the device a deleted record disappears immediately and is removed for good once the server confirms. A record that never reached the server is simply discarded.

A record is only marked Synced if it was not edited again while the upload was running, so a late edit is never lost.

### Backup

The trip database and receipt photos are included in Android cloud backup and device-to-device transfer (see `res/xml/data_extraction_rules.xml`). Nothing else is. Until the app can download records from the server, this backup is the only way to get your data back on a new phone.

## Releasing

See [docs/RELEASING.md](docs/RELEASING.md) for signing setup and how tagged releases publish a signed APK and AAB.

## Roadmap

- [ ] Employee authentication (JWT / OAuth)
- [ ] Employer web dashboard
- [ ] Automatic distance via Google Maps
- [ ] PDF / CSV expense report export
- [ ] Sync status notifications

## License

[MIT](LICENSE)

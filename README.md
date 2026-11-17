<div align="center">

# 🚗 TripLog

**Log work trips and fuel expenses. Everything stays on your phone.**

[![CI](https://github.com/MrPettitt/triplog-android/actions/workflows/ci.yml/badge.svg)](https://github.com/MrPettitt/triplog-android/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/MrPettitt/triplog-android?color=2ea44f&label=release)](https://github.com/MrPettitt/triplog-android/releases)
![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white)
![Tests](https://img.shields.io/badge/unit%20tests-104-brightgreen)
![Offline](https://img.shields.io/badge/network-none-lightgrey)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

No account. No server. No internet permission.

</div>

<p align="center">
  <img src="docs/screenshots/home.png" width="23%" alt="Home dashboard" />
  <img src="docs/screenshots/log-trip.png" width="23%" alt="Log trip form" />
  <img src="docs/screenshots/fuel-expense.png" width="23%" alt="Fuel expense form" />
  <img src="docs/screenshots/all-trips.png" width="23%" alt="All trips list" />
</p>

<p align="center">
  <img src="docs/screenshots/home-light.png" width="23%" alt="Home dashboard, light theme" />
  <img src="docs/screenshots/all-trips-light.png" width="23%" alt="All trips list, light theme" />
</p>

## At a glance

| | |
|---|---|
| 🧭 **Trips** | Log, edit, delete, search and filter |
| ⛽ **Fuel** | Amount, litres, odometer, station and a receipt photo |
| 📊 **Summary** | One card per month, with a monthly fuel budget and progress bar |
| 📤 **Export** | CSV through the Android share sheet |
| 🌗 **Themes** | Light, dark or follow the system |
| 🔒 **Privacy** | Local Room database, no network access |

## Project status

Current version: **v1.8.0**. This is a demo project: there is no signing key, no published APK and nothing external to manage.

| Check | Status | Notes |
|---|---|---|
| Lint | ✅ Passing | Runs in CI on every push and PR |
| Unit tests | ✅ 104 passing | ViewModels, repositories, migrations, formatters, filters, summary, budget, CSV |
| Debug build | ✅ Passing | Built in CI |
| Emulator run | ✅ Checked | Pixel-style AVD, API 35: trips, fuel, summary, budget warning, themes |
| Data migrations | ✅ Tested | Schema v1 to v4 covered by migration tests |
| Accessibility | ✅ WCAG AA text contrast | Both themes, 16sp body text |
| Budget: change, remove, over-budget red state | 🟡 Unit tested, not yet checked on screen | |
| Upgrade from a real v1.2.0 install | 🟡 Not checked on a device | Migration tests pass |
| CSV opened in Excel or Sheets | 🟡 Not checked | Format is tested, UTF-8 with ISO dates |

## Release history

| Version | What it added |
|---|---|
| v1.8.0 | Monthly fuel budget |
| v1.7.0 | Light theme and theme picker |
| v1.6.0 | Monthly summary |
| v1.5.0 | Search and filters |
| v1.4.0 | CSV export |
| v1.3.0 | Fully local: sync code, signing and external services removed |
| v1.2.0 | Edit and delete, data safety |
| v1.0.0 | First release |

## Features

- **Trip logging**: start and end location, distance, purpose, date, time and notes. Tap a trip to edit or delete it.
- **Fuel expenses**: date, litres, amount (ZAR), odometer, station and a receipt photo, with a list of past expenses. Tap one to edit or delete it.
- **Monthly summary**: a Summary tab with one card per month that has activity: number of trips, distance, fuel spend, litres and fill-ups. It can be exported as CSV too.
- **Monthly fuel budget**: set a budget on the Summary tab and a progress bar shows this month's fuel spend against it (green, amber from 80%, red when over). Months that went over are flagged. The budget is stored on the device and can be changed or removed at any time.
- **Search and filters**: the Trips and Fuel screens have a search box and date range chips (all time, this month, last 30 days, this year); trips can also be filtered by purpose. The list, the totals and the CSV export all follow the filter.
- **CSV export**: the share button on the Trips and Fuel screens exports the list as a CSV file (ISO dates, dot decimals, UTF-8, safe against spreadsheet formulas) and opens the Android share sheet. The app never sends the file anywhere itself.
- **Local storage**: everything is stored in a Room (SQLite) database on the device. The app has no internet permission.
- **Receipt photos**: taken with the device camera app (no camera permission needed) and shrunk to 1600 px on the long side. Deleting or replacing a receipt removes the old photo from storage.
- **Input checks**: comma decimals are accepted; zero, negative and non-numeric values and future dates are rejected with a clear message.
- **Light and dark themes**: follows the phone setting by default, or pick Light or Dark with the button on the Home screen (the choice is remembered). Material 3 with 16sp primary body text. In both themes text and status colours meet WCAG AA contrast (4.5:1) on the app surfaces, and the white label on the primary button is about 5:1.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM + Repository |
| Local DB | Room |
| DI | Hilt |
| Images | Camera capture via `TakePicture`, Coil 3 |
| Testing | JUnit 4, kotlinx-coroutines-test, hand-written fakes |
| CI | GitHub Actions: lint, unit tests, debug build |

## Project structure

```
app/src/main/java/com/triplog/
├── data/
│   ├── local/        # Room: entities, DAOs, database, converters, migrations
│   ├── settings/     # Remembered theme choice
│   └── repository/   # TripRepository, FuelExpenseRepository
├── di/               # Hilt module
├── domain/model/     # Trip, FuelExpense, TripPurpose
├── ui/
│   ├── home/  logtrip/  expenses/  trips/  summary/   # Screens + ViewModels
│   ├── navigation/   # NavGraph
│   ├── theme/        # Light and dark colours, typography, theme mode
│   └── Components.kt, Pickers.kt, Dialogs.kt, ExportShare.kt, FilterBar.kt   # Shared composables
└── util/             # Formatters, number parsing, list filters, monthly summary, CSV export, receipt image shrinking
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

## Data and backup

The trip database and receipt photos are included in Android cloud backup and device-to-device transfer (see `res/xml/data_extraction_rules.xml`). Nothing else is. The database schema is exported to `app/schemas/` and upgrades from every earlier version are covered by migrations in `data/local/Migrations.kt`, so existing trips and expenses are kept.

## License

[MIT](LICENSE)

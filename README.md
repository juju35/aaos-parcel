# Parcel — Android Automotive

[Parcel](https://parcelapp.net) deliveries for **Android Automotive OS 12+** (API 31), built with the
[Car App Library](https://developer.android.com/training/cars/apps) templates, with a notification whenever a
delivery has news. The car counterpart of [ha-parcel](https://github.com/juju35/ha-parcel).

- Deliveries under way first, by expected date: status in colour, expected date, last event and where it happened
- Optionally the recently delivered ones too (Settings › Deliveries listed)
- Detail: expected date or window, carrier and tracking number, extra information, every event, newest first;
  **Go** hands the pickup point to the car's navigation app when the parcel awaits pickup
- **Hourly check** in the background: one notification per delivery with a new status or event; tapping it opens
  the delivery
- Settings: Parcel API key, deliveries listed, notifications on or off

## Parcel API key

Generate a key at [web.parcelapp.net](https://web.parcelapp.net) (Parcel Premium), then in the app: Settings ›
Parcel API key, parked (the car's keyboard is locked while driving). The key is checked with one call before it is
kept, stays on the car (left out of Android backups) and is only sent to `api.parcel.app`.

## How it checks

Same API as ha-parcel: `GET https://api.parcel.app/external/deliveries/?filter_mode=recent` with the `api-key`
header, limited by Parcel to **20 requests an hour**.

| When | What |
| --- | --- |
| Every hour (WorkManager, with a network, while the car is on) | Fetch, compare each delivery's status and latest event with the previous check, notify the ones that changed |
| Opening the app | Last check shown at once, fetched again if older than 5 minutes |
| Refresh button | Fetch now |
| New key | The key's check is the first one: deliveries already there are not notified |

The "recent" list is always fetched, so a delivery that turns delivered still gets its notification; "under way
only" just filters the screen. The carriers' names come from Parcel's public `supported_carriers.json`, once a day.

## Layout

```
app/src/main/java/me/jstephan/parcel/
├── ParcelApp.kt              # notification channel, hourly check schedule
├── ParcelService.kt          # CarAppService + Session (opens a delivery from its notification)
├── Prefs.kt                  # API key (not backed up), filter, notifications
├── data/                     # HTTP, Parcel API client, models, repository (cache + change detection)
├── work/                     # CheckWorker (hourly), Notifier
└── ui/                       # list, detail (ListTemplate), settings, API key (SignInTemplate)
```

## Build and run

```sh
./gradlew assembleDebug
```

Test on an emulator from Android Studio's Device Manager with an **Automotive** system image
(Android 12L / API 32 or later, "with Google APIs/Play"). Debug builds accept any templates host;
release builds only the hosts in the library's allowlist. To run the hourly check without waiting, force its job
(WorkManager runs on JobScheduler; the id is in the dump):

```sh
adb shell dumpsys jobscheduler | grep -A2 me.jstephan.parcel   # JOB #u0a123/<id>
adb shell cmd jobscheduler run -f me.jstephan.parcel <id>
```

CI: build, Detekt + ktlint, Android lint and jscpd on every pull request; pushing a new `appVersion` with its
CHANGELOG.md section to `main` publishes the release (APK + bundle, Google Play when its secrets are set).
See [play/README.md](play/README.md) for the store listing.

## License

MIT

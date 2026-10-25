# Changelog

## 0.1.1

### Fixes
- A delivery update found by the app itself (on opening, or with the refresh button) is now notified too; before, only the hourly check notified, and an update the app had already seen never was.
- Deleting the API key while a check was running could notify "Clé d'API Parcel refusée".
- Expected dates Parcel sends as text ("2026-10-26 09:00:00") are shown as a day ("lun. 26 oct."), as the timestamps.
- Parcel trimming the oldest events of a delivery no longer counts as an update.

### Changes
- A delivery Parcel adds by itself (from your e-mails) is notified when it shows up; still nothing on the first check after a new key.
- Build: Android Gradle plugin 9.4 with its built-in Kotlin (no more Kotlin Android plugin), Gradle 9.8; unit tests for the Parcel answers, the update detection and the dates, run by the Build workflow.

## 0.1.0

First version.

### Features
- Parked use only: the system blocks the app while the car moves; notifications still arrive.
- Deliveries of the Parcel account, under way first by expected date: status in colour, expected date, last event and place.
- Delivery detail: expected date or window, carrier and tracking number, extra information, every event; "Go" to the pickup point when the parcel awaits pickup.
- Hourly check in the background (WorkManager, with a network): a notification for each delivery with a new status or event, opening its detail.
- API key `demo`: made-up deliveries with no Parcel account (try-out, emulator, Play review).
- Settings: Parcel API key (checked before it is kept, left out of backups), deliveries listed (under way and recently delivered, or under way only), notifications on or off.

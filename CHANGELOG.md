# Changelog

## Unreleased

### Changes
- Build: Android Gradle plugin 9.4 with its built-in Kotlin (no more Kotlin Android plugin), Gradle 9.8.

## 0.1.0

First version.

### Features
- Parked use only: the system blocks the app while the car moves; notifications still arrive.
- Deliveries of the Parcel account, under way first by expected date: status in colour, expected date, last event and place.
- Delivery detail: expected date or window, carrier and tracking number, extra information, every event; "Go" to the pickup point when the parcel awaits pickup.
- Hourly check in the background (WorkManager, with a network): a notification for each delivery with a new status or event, opening its detail.
- API key `demo`: made-up deliveries with no Parcel account (try-out, emulator, Play review).
- Settings: Parcel API key (checked before it is kept, left out of backups), deliveries listed (under way and recently delivered, or under way only), notifications on or off.

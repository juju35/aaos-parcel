# Google Play

Everything the Play Console asks for, ready to paste or upload. Templated Car App Library apps are distributed on
**Android Automotive OS only** tracks (`automotive:qa` for internal testing, `automotive:production`).

> Google Play only accepts templated Android Automotive apps of a supported category. The app declares the POI
> category (the pickup point of a parcel is handed to the navigation app), but package tracking is not one of the
> listed categories: the review may refuse it for production. Internal testing and `adb install` work either way.

| File | Console field |
| --- | --- |
| `listing/fr-FR/title.txt` | Store listing › App name (30 max) |
| `listing/fr-FR/short_description.txt` | Short description (80 max) |
| `listing/fr-FR/full_description.txt` | Full description (4,000 max) — says it is not made by Parcel |
| `whatsnew/fr-FR.txt` | Release notes of the next release (500 max), sent by the Release workflow |
| `graphics/icon-512.png` | App icon (512 × 512), ha-parcel's icon (`python scripts/play_graphics.py`) |
| `graphics/feature-graphic.png` | Feature graphic (1024 × 500), same script |
| `graphics/automotive/*.png` | Advanced settings › Form factors › Android Automotive OS screenshots (landscape 1024 × 768, portrait 800 × 1280): real screenshots from the generic Automotive emulator with the demo key, as Google asks, refreshed by running the Emulator workflow by hand with *screenshots* |
| `../PRIVACY.md` | App content › Privacy policy: `https://github.com/juju35/aaos-parcel/blob/main/PRIVACY.md` |

## App content answers

| Section | Answer |
| --- | --- |
| Privacy policy | URL above |
| Ads | No ads |
| App access | Some functionality is restricted. Note for the reviewer: *"The app shows the deliveries of a Parcel account (Parcel Premium API key). To try it without one, parked (the app is blocked while driving): Configurer on the first screen (or Réglages › Clé d'API Parcel), type demo and confirm: made-up deliveries are shown, and the hourly check posts a notification for one of them."* |
| Content rating | Category "All other app types"; no to every question → PEGI 3 / everyone |
| Target audience | 18 and over (drivers); not designed for children |
| Financial features, health | None |

### Data safety

- Collects or shares user data: **Yes**
- **App activity › Other user-generated content**: the deliveries of the Parcel account, received from Parcel to be
  shown; not shared with anyone else
- **Personal info › Other info**: the Parcel API key, sent to Parcel only
- Encrypted in transit: **Yes**; deletion: deleting the key in the app or clearing its data removes everything
- Every other data type: not collected

These are the developer's declarations: check them against `PRIVACY.md` before submitting.

Automotive screenshots can't go through the Publishing API (no automotive `AppImageType`): upload them in the Console.

## Release flow

1. Signing: create an upload key once and add it to the repository secrets (see the main README).
2. First upload by hand (the API can't create the first release): Console › Test and release › Advanced settings ›
   Form factors › **Add Android Automotive OS**, then upload `parcel-x.y.z.aab` from the GitHub release to the
   Android Automotive OS internal testing track.
3. Then, with the `PLAY_SERVICE_ACCOUNT_JSON` secret, each release is uploaded by the Release workflow to the
   track in the `PLAY_TRACK` variable (default `automotive:qa`), as a draft while the app has never been published
   (`PLAY_RELEASE_STATUS` variable, then `completed`).
4. Promote in the Console. Internal testing has no form-factor review; closed testing a non-blocking one;
   open testing and production a blocking one against the car app quality guidelines.

A personal developer account created after 13 November 2023 also needs a closed test with at least 12 testers opted
in for 14 days in a row before it can apply for production access.

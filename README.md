# Anicca

Android home-screen widgets about time passing. It started with your age as a number with eight decimals, moving in real time.
(Anicca is the Pali word for impermanence. The package id is still `com.lifeclock.widget` so placed widgets survive the rename.)
Kotlin, RemoteViews for the widgets, Compose for the app. Colors follow Material You.

Widgets:

| Widget | Size | Shows |
|---|---|---|
| Age | 2×2 | the number only |
| Life in years | 2×2 | a circle per year of life, the current one filling up |
| Year ruler | 4×2 | the number and a tape with a tick per day |
| Year in days | 4×2 | the number and a dot per day |
| Path around the Sun | 2×2 | whole kilometres travelled with Earth since birth, grouped by thousands, running |
| Birth light | 2×2 | the light of your birth day as a ring among nearby stars, and which star it reaches next |
| Mala | 2×2 | 108 beads, one moves a day, finished laps in the middle |
| Heartbeats | 2×2 | beats since birth at a fixed 70 a minute and what is left of the average 2.5 billion |
| Stone garden | 4×2 | raked sand with stones and ripples that meet and cancel, a new arrangement every day from the date |
| Planets | 2×2 → 4×4 | the eight planets from above on a compressed scale; names, last month's track and Earth's speed from about 4×4 |

Digits are vector drawables, so there is no font dependency. The fast decimals are a ViewFlipper
that Android runs by itself; an inexact alarm builds the next set of frames. No background service.

The app and widgets are in English and Russian and follow the system language.

Adding a widget: write its renderer, add a provider class and a `<receiver>` in the manifest, then add one
entry to `WidgetRegistry`. The Widgets tab, refresh and the picker previews pick it up from there.

Planet positions come from the JPL Keplerian elements (valid 1800-2050), computed on the phone with no network;
see `Planets.kt` and `PlanetsTest.kt`. Planet widgets redraw every few hours.

## Build

Android Studio's JDK (17 or newer) and SDK platform 37.0. Put the SDK path in `local.properties` as `sdk.dir=...`.
Build from a path that has only ASCII characters.

```
./gradlew assembleDebug testDebugUnitTest
```

On Windows use `gradlew.bat`. More notes for contributors are in `CLAUDE.md`.
## Use

Open the app, enter your birth date, then add a widget from the launcher's widget list or from the
Widgets tab. Long-press a widget to change its font, color and background.

## RemoteViews gotcha

Widget layouts can only use classes that RemoteViews allows. A bare `<View>` in the layout makes the
launcher drop the preview and the widget show "Can't load widget". Use `FrameLayout` for spacers and
dividers.

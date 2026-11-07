# Anicca: developer notes

Android home-screen widgets about time passing. Kotlin, RemoteViews for the widgets, Jetpack Compose for the app.
The application id stays `com.lifeclock.widget`: changing it would drop widgets already placed on a home screen.

## Widgets
`WidgetRegistry` is the single list. Kinds: NUMBER (age), GRID (life in years), RULER, DOTS, MALA, PULSE
(heartbeats), PLANETS (2x2 up to 4x4), PATH (kilometres around the Sun), LIGHT (birth light), GARDEN (4x2 stone
garden). To add a widget: a renderer in `WidgetRenderer`, a provider class, a `<receiver>` in the manifest and
an xml in `res/xml`, a preview layout, one entry in `WidgetRegistry`, and strings in both languages.

Design notes:
- UI text is Russian and English. Every string is in `values/strings.xml` and `values-ru/strings.xml`; keep both
  in sync, with plural forms for Russian.
- The red accent (`#E5242B`) marks "now": the ruler needle, today's dot, Earth, the current mala bead.
- Heartbeats use a fixed 70 bpm and an average lifetime of 2.5 billion beats. They are shown as an average, never
  as a forecast. The ECG is a 24-frame ViewFlipper at 71 ms per frame: one sweep is two beats, 1.71 s, which is 70 bpm.
- Path: whole kilometres with thin gaps between thousands, no decimal point.
- Planets: compressed, roughly logarithmic orbit scale; positions come from the JPL Keplerian elements
  (valid 1800-2050), computed on the device; refreshed every 3 hours.
- Stone garden: ripples from several stones add up as interference and are lit from the top left. The layout is
  seeded by the date. Low contrast and anti-aliased on purpose.
- Colours follow the system (Material You) unless the widget settings say otherwise.

## Pitfalls already hit
- RemoteViews cannot inflate a bare `<View>`; use `FrameLayout`. This once broke both 4x2 widgets.
- When a widget is redrawn the launcher re-applies actions to the existing view. Anything that was shown must be
  explicitly hidden again, otherwise it stays (the readability plate did). See `applyPlate`.
- Widget size: portrait is `OPTION_APPWIDGET_MIN_WIDTH` by `MAX_HEIGHT`, landscape is `MAX_WIDTH` by `MIN_HEIGHT`.
  Using the minimum for both draws wide widgets at half height. See `BaseLifeProvider.sizeOf`.
- `ImageView.setScaleType` is not remotable; one missing method fails the whole widget update.

## Build and test
Requirements: JDK 17 or newer (the one bundled with Android Studio works), Android SDK platform 37.
Point `sdk.dir` in `local.properties` at the SDK. Build from a path with only ASCII characters; the Android
build tools can fail otherwise.

    ./gradlew assembleDebug testDebugUnitTest      # Windows: gradlew.bat

There are 20 unit tests for the planet maths, the trip and star calculations, the pulse and the garden generator.

Emulator tips: reinstalling the app drops placed widgets, so pin them again from the Widgets tab. A time zone
change is a quick way to force every widget to redraw.

## Known gaps and ideas
- Only tried on an emulator, not on a physical device. The battery cost of the ECG animation is unmeasured.
- Switching the colour from "custom" back to "system" keeps the old tint until a full redraw.
- Day length (sunrise, sunset) as a very small widget is an open idea.
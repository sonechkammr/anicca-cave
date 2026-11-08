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
  in sync, with plural forms for Russian. Lint fails the build on `MissingTranslation` and `HardcodedText`.
- The red accent (`#E5242B`, `NowRed`) marks "now": the ruler needle, today's dot, Earth, the current mala bead,
  the current year in the life grid.
- Heartbeats use a fixed 70 bpm and an average lifetime of 2.85 billion beats (about 77 years). They are shown as an average, never
  as a forecast. The widget shows how many are left on average, to the last digit and running down like the age
  decimals (do not round it: the moving digits are the point of the app), and only one word, "left" / "ещё"; no other
  text. Past the average the count stays at 0. The ECG is
  a 24-frame ViewFlipper at 71 ms per frame: one sweep is two beats, 1.71 s, which is 70 bpm.
- Path: whole kilometres with thin gaps between thousands, no decimal point.
- Planets: compressed, roughly logarithmic orbit scale; positions come from the JPL Keplerian elements
  (valid 1800-2050), computed on the device; refreshed every 3 hours. At 4x4 with a birth date saved, each planet leaves a translucent trail in its own colour (Earth red, with
  the planet palette every planet in its colour, otherwise the widget tint) for everything it has travelled since the
  last birthday (`Planets.travelledSince`, not capped at one lap). Each lap is its own translucent pass, so places a
  fast planet crossed again and again get darker. No numbers on it, on purpose. Without a birth date it falls back to the
  last month's track.
- Stone garden: ripples from several stones add up as interference and are lit from the top left. The layout is
  seeded by the date. Low contrast and anti-aliased on purpose.
- Colours follow the system (Material You) unless the widget settings say otherwise.
- Widget motion is content, not decoration: the moving decimals are the age and the ECG sweep is what the pulse
  widget shows, so "remove animations" does not stop them. Only decorative app motion (the confetti) obeys it.

## The app (Compose)
Packages under `com.lifeclock.widget`:
- `ui/AniccaApp.kt`: Material 3 `NavigationBar` + `NavHost`. Routes: `setup` (first launch, no bar), `today`,
  `widgets`, `settings`. Tabs hang off `today`; saving the first birth pops `setup`.
- `ui/AppViewModel.kt`: state over the existing `Prefs` (SharedPreferences, keys unchanged), milestone confetti, and
  the one 10 Hz clock (`nowMs`, runs only while the UI is active). Only the age header and the milliseconds row follow
  every tick; everything else follows a once-a-second value. `Trip.kmAt` runs once per second.
- `ui/theme`: `AniccaTheme` (Material You), `Anicca.spacing/type/shapes` tokens, `raised()` and `inset()` depth
  modifiers, `Gallery.kt` (a preview page of all of it: light, dark, Russian, 200% font).
- `ui/components`: `Screen` (centred, 640 dp max), `GroupCard`, `ValueRow`, `Panel`, `BirthForm` (still used by
  `WidgetConfigActivity`), `Confetti`, `rememberReducedMotion`.
- `ui/today`: collapsing age header (measured at both extremes, so large fonts fit), grouped lists, moon phase,
  life grid, share as image, `DailyLine`.
- `ui/settings`: one-sentence `SetupScreen`, grouped `SettingsScreen`, the date/time/city bottom sheets, `BirthModel`
  (pure rules: cities, search, date limits), per-app language through `LocaleManager` (Android 13+ only).
- `ui/widgets`: the showcase (chips, pager, wallpaper-like stage, Add, Customize, background slider).

Rules the redesign settled on:
- Medium depth: raised cards and tiles, inset wells for tracks and fields, flat text rows.
- Text is `sp`, spacing is `Anicca.spacing`; touch targets are at least 48 dp for anything that acts.
- Settings and Setup share the same sheets. A birth moment in the future is refused.
- The share image carries the age and the life grid, never the birth date.
- The daily impermanence line: the string array `impermanence_lines` ships empty in both languages. Texts are added
  only after the project owner approves each one; the card stays hidden until the array has lines.
- Moon phase uses the mean synodic month (about 2 % of a cycle, up to a day off). The screen says so.

## Pitfalls already hit
- RemoteViews cannot inflate a bare `<View>`; use `FrameLayout`. This once broke both 4x2 widgets.
- When a widget is redrawn the launcher re-applies actions to the existing view. Anything that was shown must be
  explicitly hidden again, otherwise it stays (the readability plate did). See `applyPlate`.
- Widget size: portrait is `OPTION_APPWIDGET_MIN_WIDTH` by `MAX_HEIGHT`, landscape is `MAX_WIDTH` by `MIN_HEIGHT`.
  Using the minimum for both draws wide widgets at half height. See `BaseLifeProvider.sizeOf`.
- `ImageView.setScaleType` is not remotable; one missing method fails the whole widget update.
- Android's regex engine rejects an unescaped `}` that the desktop JVM accepts (`Regex("\\{x}")` crashed the first
  run of Setup). Unit tests run on the desktop JVM and cannot catch it; prefer plain string handling, and always run
  the app after touching text parsing.
- `Material` `surfaceContainerLowest` is white in light themes and near black in dark ones; do not use it for a
  recessed well. `inset()` derives its fill from the page colour.
- `ModalBottomSheet` content must scroll, or the buttons are cut off in landscape and at large font sizes.
- Nested scrolling: a `LazyColumn` inside a `verticalScroll` needs a finite height (`heightIn(max = ...)`).
- Back from Today must exit the app: pop `setup` with `inclusive = true` when the first birth is saved.

## Build and test
Requirements: JDK 17 or newer (the one bundled with Android Studio works), Android SDK platform 37.
Point `sdk.dir` in `local.properties` at the SDK (backslashes and the drive colon must be escaped, or lint fails
with `PropertyEscape`). Build from a path with only ASCII characters; the Android build tools can fail otherwise.

    ./gradlew assembleDebug testDebugUnitTest lintDebug      # Windows: gradlew.bat

On Windows with a non-ASCII user name, point Gradle at an ASCII home, or the test workers die with "Could not find or
load main class worker.org.gradle.process.internal.worker.GradleWorkerMain":

    $env:GRADLE_USER_HOME = 'C:\gh'

CI (`.github/workflows/ci.yml`) runs the same three tasks on every push. `setup-android` is told to install no extra
packages (its default `tools` package no longer exists).

There are 59 unit tests: planet maths, trip and star calculations, the pulse and the garden generator, `LifeMath` edge
cases (29 February, DST gaps, before 1970, future dates, zone change), the birth rules and sentence template, the moon
phase against real new, quarter and full moons, and the daily-line picker.

Emulator tips: `adb install -r` keeps the app's data and placed widgets; an uninstall or `pm clear` resets the app to
the Setup screen, so pin widgets again from the Widgets tab. A time zone change is a quick way to force every widget to
redraw. For a tablet-sized display try `wm size 1600x2560` and `wm density 280` (then `wm size reset`).

## Known gaps and ideas
- Tried on an emulator only, never on a physical device. Not measured: the battery cost of the 10 Hz clock and the ECG
  animation, and scrolling smoothness (the emulator renders in software, so its frame stats mean little).
- Screen-reader descriptions for the age, the life grid and headings are in the code but were not checked with TalkBack.
- The Russian setup sentence "Я родился(-ась) ..." avoids declining the city, which cannot be done automatically.
- Switching the colour from "custom" back to "system" keeps the old tint until a full redraw.
- The widgets' background opacity is one app-wide default plus an optional per-widget value, so the slider and a
  placed widget can disagree.
- Day length (sunrise, sunset) as a very small widget is an open idea.

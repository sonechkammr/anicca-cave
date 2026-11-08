# Changelog

## 2.0 (November 2026)

A redesign of the app around the existing widgets. The widget providers, the application id and the saved settings
are unchanged, so widgets already on a home screen keep working.

### App
- **Setup:** one sentence, "I was born on [date] at [time] in [city]". Each part opens a bottom sheet with the Material
  calendar (1900 to today), a clock dial with an "I don't know the time" switch, or a searchable city list with
  localized names. A birth date can be set without the keyboard.
- **Today:** the age is a large header that folds into a compact bar while scrolling. Grouped lists below: breakdown,
  totals (including the path around the Sun), moon phase now and on the day of birth, and the life grid.
- **Share as image:** the age and the life grid as a picture (never the birth date), through the system share sheet.
- **Widgets tab:** a showcase with one big live preview at a time on a wallpaper-like stage, category chips, Add and
  Customize, and a live background slider.
- **Settings:** grouped list; the same sheets as Setup; theme; per-app language (English, Русский) on Android 13+.
- Bottom navigation, a ViewModel over the existing preferences, and one lifecycle-aware 10 Hz clock for the digits.
- Design tokens (spacing, type, shapes) and raised/inset depth, with a gallery preview in light, dark, Russian and 200 % font.
- Accessibility: reduced motion, screen-reader text for the age and the life grid, headings, 48 dp targets, a bottom bar
  that never clips, layouts for landscape and tablets.

### Widgets
- **Planets 4×4:** a translucent trail in each planet's own colour for everything it has travelled since your last
  birthday, one pass per lap, darker where a fast planet has gone round more than once.
- **Heartbeats:** one word only ("left" / "ещё") and a count of the beats left on average, running down to the last digit.
  The average is now 2.85 billion beats (about 77 years at 70 a minute).

### Project
- GitHub Actions: build, unit tests and lint on every push; the debug APK is kept as a downloadable artifact.
- Lint fails the build on missing translations and hard-coded text.
- 59 unit tests: date edge cases (29 February, DST gaps, before 1970), planet maths, birth rules, moon phase, pulse.
- Notes for contributors in `CLAUDE.md`.

## 1.0

Ten time-themed home-screen widgets: age, life in years, year ruler, year in days, path around the Sun, birth light,
mala, heartbeats, stone garden and planets.

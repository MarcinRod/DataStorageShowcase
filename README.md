DataStorageShowcase
===================

Small Android Compose sample that demonstrates Room (Room KTX) + DataStore (Preferences) integration.

What this project shows
- A simple color catalog stored in Room (`ColorEntity`, `ColorDao`, `ColorRepository`).
- Two UI variants in `MainActivity`:
  - `ColorsScreenSimple` — in-memory filters only, no DataStore persistence.
  - `ColorsScreenWithDataStore` — persists filter values and UI theme via Preferences DataStore.
- Filtering UI (bottom FilterBar):
  - Text field to search by name.
  - Hue range selector implemented with `RangeSlider` (hue gradient background, colored thumbs).
  - Favorite toggle to show only favorites.
- Add Color dialog: name + hex input with live color preview.
- Deleted colors persistence: deleted items are saved in DataStore so they can be restored later.
- Theme selection persisted in DataStore ("system", "light", "dark").

Important files
- `app/src/main/java/pl/marrod/datastorageshowcase/MainActivity.kt` — contains the UI and two composables (`ColorsScreenSimple`, `ColorsScreenWithDataStore`).
- `app/src/main/java/pl/marrod/datastorageshowcase/data/ColorEntity.kt` — Room entity for colors (now `@Serializable`).
- `app/src/main/java/pl/marrod/datastorageshowcase/data/ColorRepository.kt` — repository wrapping DAO and exposing a Flow of colors.
- `app/src/main/java/pl/marrod/datastorageshowcase/data/SettingsRepository.kt` — Preferences DataStore wrapper. Exposes flows and suspend setters:
  - `nameQueryFlow: Flow<String>`
  - `minHueFlow: Flow<Float>`
  - `maxHueFlow: Flow<Float>`
  - `showFavFlow: Flow<Boolean>`
  - `themeFlow: Flow<String>`
  - `deletedColorsFlow: Flow<List<ColorEntity>>` — JSON array stored under key `deleted_colors_json`.
  - helpers: `setNameQuery(...)`, `setHueRange(...)`, `setShowFavorites(...)`, `setTheme(...)`, `addDeletedColor(...)`, `removeDeletedColor(...)`, `getSettingsOnce()`, `getThemeOnce()`, `migrateLegacyDeletedSetIfNeeded()`.

Testing tips
- Try deleting a color (long-press). Open the Restore dialog via the top-bar Restore icon and verify it appears. Restore should re-insert it.
- Toggle theme in the top-bar menu and relaunch the app to verify persistence.
- Use the FilterBar to search and adjust hue range — values persist when using the DataStore-backed screen.

Next improvements (suggestions)
- Use a small ViewModel to store UI state and keep composables thinner.
- Add an Undo snackbar for deletes (delay persisting to DataStore until undo times out).


License
- This sample is provided as-is for demonstration and learning purposes.


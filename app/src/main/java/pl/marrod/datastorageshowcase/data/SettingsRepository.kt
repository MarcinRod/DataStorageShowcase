package pl.marrod.datastorageshowcase.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey as stringPrefKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.net.URLDecoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


/**
 * Preferences DataStore name: "settings".
 *
 * This file exposes a Context extension property `dataStore` which returns a
 * [DataStore]<[Preferences]> instance named "settings" and a small
 * [SettingsRepository] wrapper that provides typed Flows and suspend helpers
 * for reading/writing app preferences used by the color UI.
 */

/**
 * Context extension that provides the Preferences DataStore instance used by
 * the app to persist small key/value preferences.
 *
 * Usage example:
 * val ds = context.dataStore
 */
val Context.dataStore by preferencesDataStore(name = "settings")


/**
 * Repository that wraps a Preferences [DataStore] and exposes typed Flows and
 * suspend setters for the application's persisted UI settings.
 *
 * Persisted keys (accessible via the companion object):
 * - [NAME_QUERY]    : String  — last name filter typed by the user.
 * - [MIN_HUE]       : Float   — minimum hue bound (degrees 0..360).
 * - [MAX_HUE]       : Float   — maximum hue bound (degrees 0..360).
 * - [SHOW_FAV]      : Boolean — whether the "only favorites" filter is enabled.
 * - [THEME]         : String  — theme preference: "system", "light" or "dark".
 *
 * This wrapper keeps all DataStore access in one place and returns Flows so
 * callers (ViewModels, Composables) can collect settings reactively.
 *
 * @param dataStore the Preferences DataStore instance to read/write settings.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        /** String key for the persisted name query. */
        val NAME_QUERY = stringPrefKey("name_query")
        /** Float key for the persisted minimum hue. */
        val MIN_HUE = floatPreferencesKey("min_hue")
        /** Float key for the persisted maximum hue. */
        val MAX_HUE = floatPreferencesKey("max_hue")
        /** Boolean key for the persisted favorites-only flag. */
        val SHOW_FAV = booleanPreferencesKey("show_favorites")
        /** String key for the persisted theme selection. */
        val THEME = stringPrefKey("theme")

        // JSON key that stores the entire deleted-colors list as a JSON array string.
        private val DELETED_COLORS_JSON = stringPrefKey("deleted_colors_json")

        private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        private val deletedListSerializer = ListSerializer(ColorEntity.serializer())
    }

    /**
     * Flow that emits the persisted name query (or empty string when unset).
     * Collect this in the UI to restore the last text filter.
     */
    val nameQueryFlow: Flow<String> = dataStore.data.map { it[NAME_QUERY] ?: "" }

    /**
     * Flow that emits the persisted minimum hue (or 0f when unset).
     */
    val minHueFlow: Flow<Float> = dataStore.data.map { it[MIN_HUE] ?: 0f }

    /**
     * Flow that emits the persisted maximum hue (or 360f when unset).
     */
    val maxHueFlow: Flow<Float> = dataStore.data.map { it[MAX_HUE] ?: 360f }

    /**
     * Flow that emits whether the favorites-only filter is enabled (defaults to false).
     */
    val showFavFlow: Flow<Boolean> = dataStore.data.map { it[SHOW_FAV] ?: false }

    /**
     * Flow that emits the stored theme preference; default is "system".
     * Values: "system", "light", "dark".
     */
    val themeFlow: Flow<String> = dataStore.data.map { it[THEME] ?: "system" }



    /**
     * Flow that emits the current list of deleted colors stored in the DataStore.
     * The list preserves insertion order because we store the list as a JSON
     * array under a single string key. Parsing failures yield an empty list.
     */
    val deletedColorsFlow: Flow<List<ColorEntity>> = dataStore.data.map { prefs ->
        val raw = prefs[DELETED_COLORS_JSON] ?: "[]"
        try {
            json.decodeFromString(deletedListSerializer, raw)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    /**
     * One-shot read of the deleted colors list.
     */
    suspend fun getDeletedOnce(): List<ColorEntity> {
        val prefs = dataStore.data.first()
        val raw = prefs[DELETED_COLORS_JSON] ?: "[]"
        return try {
            json.decodeFromString(deletedListSerializer, raw)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    /**
     * Persist the provided name query string.
     *
     * @param value the name query to persist (may be empty to clear the filter).
     */
    suspend fun setNameQuery(value: String) {
        dataStore.edit { prefs -> prefs[NAME_QUERY] = value }
    }

    /**
     * Persist the hue range bounds.
     *
     * @param min inclusive minimum hue in degrees (0..360).
     * @param max inclusive maximum hue in degrees (0..360).
     */
    suspend fun setHueRange(min: Float, max: Float) {
        dataStore.edit { prefs ->
            prefs[MIN_HUE] = min
            prefs[MAX_HUE] = max
        }
    }

    /**
     * Persist the favorites-only flag.
     *
     * @param value true to show only favorites, false otherwise.
     */
    suspend fun setShowFavorites(value: Boolean) {
        dataStore.edit { prefs -> prefs[SHOW_FAV] = value }
    }

    /**
     * Persist the UI theme preference.
     *
     * @param value one of: "system", "light", "dark".
     */
    suspend fun setTheme(value: String) {
        dataStore.edit { prefs -> prefs[THEME] = value }
    }

    /**
     * One-shot read: return current persisted settings as a [SettingsSnapshot].
     *
     * This is a suspend function that uses `dataStore.data.first()` to retrieve
     * a single Preferences snapshot. Use this when you need a single synchronous
     * read (for seeding, migrations, or one-time checks) instead of collecting
     * the reactive Flows exposed by this repository.
     *
     * Example:
     * ```kotlin
     * val snapshot = settingsRepository.getSettingsOnce()
     * println(snapshot.nameQuery)
     * ```
     */
    suspend fun getSettingsOnce(): SettingsSnapshot {
        val prefs = dataStore.data.first()
        return SettingsSnapshot(
            nameQuery = prefs[NAME_QUERY] ?: "",
            minHue = prefs[MIN_HUE] ?: 0f,
            maxHue = prefs[MAX_HUE] ?: 360f,
            showOnlyFavorites = prefs[SHOW_FAV] ?: false,
            theme = prefs[THEME] ?: "system"
        )
    }

    /**
     * One-shot read: return the stored theme preference as a String.
     *
     * This suspending helper uses `dataStore.data.first()` to read a single
     * Preferences snapshot and returns the theme value (defaults to
     * "system" when unset).
     *
     * Example:
     * ```kotlin
     * val theme = settingsRepository.getThemeOnce()
     * ```
     *
     * @return the persisted theme string: "system", "light", or "dark".
     */
    suspend fun getThemeOnce(): String {
        val prefs = dataStore.data.first()
        return prefs[THEME] ?: "system"
    }

    /**
     * Add a deleted color to the persisted JSON list. This writes the full
     * list atomically under a single key.
     */
    suspend fun addDeletedColor(color: ColorEntity) {
        dataStore.edit { prefs ->
            val raw = prefs[DELETED_COLORS_JSON] ?: "[]"
            val list: MutableList<ColorEntity> = try {
                json.decodeFromString(deletedListSerializer, raw).toMutableList()
            } catch (_: SerializationException) {
                mutableListOf()
            }
            list.add(color)
            prefs[DELETED_COLORS_JSON] = json.encodeToString(deletedListSerializer, list)
        }
    }
    suspend fun removeDeletedColors(){
        dataStore.edit { prefs ->
            prefs[DELETED_COLORS_JSON] = "[]"
        }
    }
    suspend fun restoreDeletedColors(): List<ColorEntity> {
        val list = getDeletedOnce()
        restoreDeletedColors()
        return list
    }


}

/**
 * One-shot snapshot of persisted settings.
 *
 * Use [SettingsRepository.getSettingsOnce] to obtain this data in a suspend
 * function when you need a synchronous snapshot instead of observing the
 * Flows.
 */
data class SettingsSnapshot(
    val nameQuery: String,
    val minHue: Float,
    val maxHue: Float,
    val showOnlyFavorites: Boolean,
    val theme: String
)

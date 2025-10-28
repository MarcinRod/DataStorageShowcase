package pl.marrod.datastorageshowcase

import android.app.Application
import pl.marrod.datastorageshowcase.data.AppDatabase
import pl.marrod.datastorageshowcase.data.ColorRepository
import pl.marrod.datastorageshowcase.data.SettingsRepository
import pl.marrod.datastorageshowcase.data.dataStore

/**
 * Application subclass that wires up application-wide singletons.
 *
 * This class creates and holds a single instance of [ColorRepository] which is
 * backed by a Room [AppDatabase]. The repository is exposed as a property so
 * other components (Activities, Fragments, ViewModels) can obtain it via the
 * application context and share the same data source instance.
 */
class App : Application() {
    /**
     * The app-wide [ColorRepository] instance. It is initialized in
     * [onCreate] after the Room database has been created.
     *
     * Use `val app = application as App` and then `app.colorRepository` to obtain
     * the repository from UI components. The property is `lateinit` because it
     * is assigned during application startup.
     */
    lateinit var colorRepository: ColorRepository
        private set

    /**
     * App-wide SettingsRepository backed by Preferences DataStore.
     */
    lateinit var settingsRepository: SettingsRepository
        private set

    /**
     * Application lifecycle entry point.
     *
     * We obtain the Room database singleton via [AppDatabase.getDatabase] and
     * create the [ColorRepository] using the DAO provided by the database.
     * This ensures the repository (and its underlying database) live for the
     * entire process lifetime and are available to all consumers.
     */
    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext)
        colorRepository = ColorRepository(db.colorDao())
        // Initialize settings repository from Context.dataStore
        settingsRepository = SettingsRepository(applicationContext.dataStore)
    }
}

package pl.marrod.datastorageshowcase.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// AppDatabase is the Room database holder for the app. It declares the
// list of entities and the database version used by Room to manage schema.
// Room generates an implementation of this abstract class at compile time.
@Database(entities = [ColorEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // Expose DAOs as abstract functions. Room will generate the concrete
    // implementations and provide them when the AppDatabase instance is created.
    abstract fun colorDao(): ColorDao

    companion object {
        // Volatile singleton instance ensures visibility of changes across threads.
        // We keep a single database instance for the whole process.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // getDatabase returns a singleton AppDatabase. It uses double-checked
        // locking (via synchronized) to lazily initialize the Room database the
        // first time it's requested and to avoid creating multiple instances.
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Build the Room database. The databaseBuilder requires the
                // application context, the concrete database class and a name
                // for the underlying SQLite file.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    // .build() creates the database instance. Additional
                    // configuration (migrations, callbacks, in-memory) can be
                    // chained here if needed in the future.
                    .build()

                // Save the created instance to the singleton variable so
                // subsequent callers receive the same instance.
                INSTANCE = instance
                instance
            }
        }
    }
}

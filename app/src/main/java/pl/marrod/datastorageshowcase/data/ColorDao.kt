package pl.marrod.datastorageshowcase.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `colors` table.
 *
 * This interface declares SQL queries and data-access operations for
 * [ColorEntity]. Room generates the concrete implementation at compile-time.
 * Use this DAO to read/write color rows from the database.
 */
@Dao
interface ColorDao {
    /**
     * Returns a cold [Flow] that emits the full list of colors ordered by id
     * whenever the table content changes.
     *
     * Collect this Flow from a ViewModel or Composable to reactively observe
     * database updates (inserts/updates/deletes).
     *
     * @return a Flow that emits the current list of [ColorEntity] ordered by id.
     */
    @Query("SELECT * FROM colors ORDER BY id ASC")
    fun getAll(): Flow<List<ColorEntity>>

    /**
     * One-shot read of all colors ordered by id.
     *
     * This is a suspend function (single read) and does not provide live
     * updates. Useful for initial seeding or operations that require a
     * synchronous snapshot of the table.
     *
     * @return the current list of [ColorEntity] ordered by id.
     */
    @Query("SELECT * FROM colors ORDER BY id ASC")
    suspend fun getAllOnce(): List<ColorEntity>

    /**
     * Return a list of colors marked as favorites.
     *
     * This is a one-off suspend query. If you need to observe changes to
     * favorites over time, expose a Flow-based variant here instead.
     *
     * @return list of favorite [ColorEntity] ordered by id.
     */
    @Query("SELECT * FROM colors WHERE isFavorite = 1 ORDER BY id ASC")
    suspend fun getFavorites(): List<ColorEntity>

    /**
     * Fetch a single color by its primary key id.
     *
     * @param id the primary key of the color to retrieve.
     * @return the [ColorEntity] with the given id or null if not found.
     */
    @Query("SELECT * FROM colors WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ColorEntity?

    /**
     * Update the favorite flag for a specific row.
     *
     * @param id the id of the color row to update.
     * @param isFav the new isFavorite value to set (true = favorite).
     */
    @Query("UPDATE colors SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFav: Boolean)

    /**
     * Query colors with hue in the half-open interval [minHue, maxHue).
     * Results are ordered by hue which is convenient for range-based UI lists.
     *
     * @param minHue inclusive lower bound of hue (degrees 0..360).
     * @param maxHue exclusive upper bound of hue (degrees 0..360).
     * @return list of [ColorEntity] whose hue falls within the provided range.
     */
    @Query("SELECT * FROM colors WHERE hue >= :minHue AND hue < :maxHue ORDER BY hue ASC")
    suspend fun getByHueRange(minHue: Float, maxHue: Float): List<ColorEntity>

    /**
     * Return rows that have an exact hue value.
     *
     * This is provided for completeness but is less commonly used than
     * range-based queries.
     *
     * @param hue the hue value to match.
     * @return list of [ColorEntity] with the exact hue.
     */
    @Query("SELECT * FROM colors WHERE hue = :hue ORDER BY id ASC")
    suspend fun getByHue(hue: Float): List<ColorEntity>

    /**
     * Update the stored hue for a given color row.
     *
     * Useful when computing hue after inserting a color or in a migration
     * where hue values need to be populated.
     *
     * @param id the id of the color row to update.
     * @param hue the hue value (degrees 0..360) to set.
     */
    @Query("UPDATE colors SET hue = :hue WHERE id = :id")
    suspend fun updateHue(id: Int, hue: Float)

    /**
     * Insert a single [ColorEntity]. Conflicts are resolved using
     * [OnConflictStrategy.REPLACE] so rows with the same primary key are
     * overwritten (useful for upsert-like behavior).
     *
     * @param color the [ColorEntity] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(color: ColorEntity)

    /**
     * Bulk insert a list of colors. For atomic multi-step changes wrap calls
     * in a transaction if required.
     *
     * @param colors the list of [ColorEntity] to insert.
     */
    @Insert
    suspend fun insertAll(colors: List<ColorEntity>)

    /**
     * Delete all rows from the `colors` table. This is a destructive operation
     * and should be used carefully.
     */
    @Query("DELETE FROM colors")
    suspend fun clearAll()

    /**
     * Delete a row by its primary key id.
     *
     * This is a convenience operation when you only have the id and don't want
     * to construct a full [ColorEntity] instance to remove it.
     *
     * @param id the id of the color row to delete.
     */
    @Query("DELETE FROM colors WHERE id = :id")
    suspend fun deleteById(id: Int)

    /**
     * Remove a specific [ColorEntity] row from the table. Room will match the
     * entity to the row by primary key.
     *
     * @param color the entity instance to delete.
     */
    @Delete
    suspend fun delete(color: ColorEntity)


}

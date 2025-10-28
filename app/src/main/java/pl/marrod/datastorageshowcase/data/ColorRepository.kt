package pl.marrod.datastorageshowcase.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository that wraps [ColorDao] and exposes higher-level data operations
 * for the rest of the app.
 *
 * The repository is a thin abstraction over the DAO that provides suspend
 * wrappers for common operations and exposes the DAO's Flow as a property
 * for components that want to observe changes.
 *
 * Prefer collecting [allColors] from a ViewModel or composable to reactively
 * observe database updates. Use the suspend helpers for one-off, blocking
 * operations such as seeding or immediate writes.
 */
class ColorRepository(private val dao: ColorDao) {
    /**
     * Cold [Flow] that emits the current list of all colors and re-emits when
     * the underlying table changes. Collect this Flow in the UI or a
     * ViewModel to keep the UI synchronized with database updates.
     */
    val allColors: Flow<List<ColorEntity>> = dao.getAll()

    /**
     * One-shot read of all colors. Useful for seeding or synchronous checks.
     *
     * @return a snapshot list of all [ColorEntity] currently stored.
     */
    suspend fun getAllOnce(): List<ColorEntity> = dao.getAllOnce()

    /**
     * Return a list of favorite colors. This is a suspend one-off query.
     *
     * @return list of favorite [ColorEntity].
     */
    suspend fun getFavorites(): List<ColorEntity> = dao.getFavorites()

    /**
     * Fetch a single color by id.
     *
     * @param id primary key of the color to retrieve.
     * @return the [ColorEntity] or null if not found.
     */
    suspend fun getById(id: Int): ColorEntity? = dao.getById(id)

    /**
     * One-off query for colors within a hue range.
     *
     * @param minHue inclusive lower bound (degrees).
     * @param maxHue exclusive upper bound (degrees).
     * @return list of [ColorEntity] whose hue lies in the provided range.
     */
    suspend fun getByHueRange(minHue: Float, maxHue: Float): List<ColorEntity> =
        dao.getByHueRange(minHue, maxHue)

    /**
     * Insert a single color into the database.
     *
     * @param color the [ColorEntity] to insert.
     */
    suspend fun insert(color: ColorEntity) = dao.insert(color)

    /**
     * Bulk insert multiple colors.
     *
     * @param colors list of [ColorEntity] to insert.
     */
    suspend fun insertAll(colors: List<ColorEntity>) = dao.insertAll(colors)

    /**
     * Delete a full entity instance from the database.
     *
     * @param color the entity to delete.
     */
    suspend fun delete(color: ColorEntity) = dao.delete(color)

    /**
     * Delete a color row by its primary key id. Convenience wrapper around
     * [ColorDao.deleteById].
     *
     * @param id primary key of the row to delete.
     */
    suspend fun deleteById(id: Int) = dao.deleteById(id)

    /**
     * Delete all rows from the table. Destructive operation.
     */
    suspend fun clearAll() = dao.clearAll()

    /**
     * Update stored hue for a color row.
     *
     * @param id the id of the color to update.
     * @param hue the hue value (degrees) to set.
     */
    suspend fun updateHue(id: Int, hue: Float) = dao.updateHue(id, hue)

    /**
     * Toggle the 'favorite' flag for a color by id.
     *
     * This reads the current entity and updates the isFavorite flag to the
     * inverse value.
     *
     * @param id the id of the color whose favorite flag should be toggled.
     */
    suspend fun toggleFavorite(id: Int) {
        val current = dao.getById(id) ?: return
        dao.updateFavorite(id, !current.isFavorite)
    }


}

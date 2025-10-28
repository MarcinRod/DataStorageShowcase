package pl.marrod.datastorageshowcase.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a color stored in the database.
 *
 * This Room entity holds a display name, the Android color packed as an ARGB
 * Int, a favorite flag, and a precomputed hue value (in degrees) to allow
 * efficient range queries at the SQL level.
 *
 * @property id Auto-generated primary key for the color row.
 * @property name Human-readable name for the color (e.g. "Red").
 * @property color Android color stored as an ARGB Int (0xAARRGGBB).
 * @property isFavorite Whether the user marked this color as a favorite.
 * @property hue Hue component of the color in degrees [0,360). The default
 *   value is computed from the provided `color` using
 *   [android.graphics.Color.colorToHSV], so new entities created with a
 *   `color` value will have their hue pre-populated for efficient queries.
 */
@Serializable /* for DataStore serialization if needed */
@Entity(tableName = "colors")
data class ColorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    // store Android color as Int (ARGB)
    val color: Int,
    // mark if user favorited this color
    val isFavorite: Boolean = false,
    // store hue as Float so it can be queried directly from SQL
    // default computes hue from the provided `color` value
    val hue: Float = FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) }[0]
)

package pl.marrod.datastorageshowcase


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.launch
import pl.marrod.datastorageshowcase.data.ColorEntity
import pl.marrod.datastorageshowcase.data.SettingsRepository
import pl.marrod.datastorageshowcase.ui.theme.DataStorageShowcaseTheme
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Main activity that hosts the Compose UI for the app.
 *
 * Responsibilities:
 * - Sets up the app theme and edge-to-edge UI.
 * - Obtains the app's [pl.marrod.datastorageshowcase.data.ColorRepository] from the [App] instance.
 * - Holds ephemeral UI state for filtering (name, hue range, favorites)
 *   and the dialog for adding colors.
 * - Collects the repository Flow of colors and derives a filtered list
 *   that is displayed in the grid.
 */
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Default to the DataStore-backed screen. The simple variant is
            // available as `ColorsScreenSimple` if you want to show the UI
            // without DataStore integration.
            val app = application as App
            val useDataStore = true
            if (useDataStore)
                ColorsScreenWithDataStore(
                    repository = app.colorRepository,
                    settings = app.settingsRepository
                )
            else
                ColorsScreenSimple(repository = app.colorRepository)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
/**
 * Display a grid of color items.
 *
 * This composable renders a lazy vertical grid where each item shows the
 * color swatch, name, and a favorite toggle. The grid supports a long-press
 * callback for deletion, and a toggle callback to mark/unmark favorites.
 *
 * @param colors list of [ColorEntity] to display.
 * @param onToggleFavorite callback invoked with the color id when the favorite
 *   icon is tapped.
 * @param onLongClick callback invoked with the [ColorEntity] when the item is
 *   long-pressed (used for deletion in the host).
 */
@Composable
fun ColorGrid(
    colors: List<ColorEntity>,
    onToggleFavorite: (Int) -> Unit,
    onLongClick: (ColorEntity) -> Unit
) {
    LazyVerticalGrid(columns = GridCells.Fixed(2), content = {
        items(colors) { colorEntity ->
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color(colorEntity.color))
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = { /* no-op for now */ },
                        onLongClick = { onLongClick(colorEntity) }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = colorEntity.name,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                )

                IconButton(
                    onClick = { onToggleFavorite(colorEntity.id) },
                ) {
                    Icon(
                        imageVector = if (colorEntity.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (colorEntity.isFavorite) "Unfavorite" else "Favorite",
                        tint = if (colorEntity.isFavorite) Color.Red else Color.White
                    )
                }
            }
        }
    })
}

private val sampleColors = listOf(
    "Red" to "#F44336".toColorInt(),
    "Pink" to "#E91E63".toColorInt(),
    "Purple" to "#9C27B0".toColorInt(),
    "Deep Purple" to "#673AB7".toColorInt(),
    "Indigo" to "#3F51B5".toColorInt(),
    "Blue" to "#2196F3".toColorInt(),
    "Light Blue" to "#03A9F4".toColorInt(),
    "Cyan" to "#00BCD4".toColorInt(),
    "Teal" to "#009688".toColorInt()
)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
/**
 * Compose preview showing the `FilterBar` and a sample grid of colors.
 *
 * This preview builds a small UI state and renders the main filtering UI for
 * visual validation in Android Studio's preview pane.
 */
@Composable
fun DefaultPreview() {
    DataStorageShowcaseTheme {
        // preview state
        var name by remember { mutableStateOf("") }
        var minHue by remember { mutableFloatStateOf(0f) }
        var maxHue by remember { mutableFloatStateOf(360f) }
        var fav by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Show the FilterBar preview at top
            FilterBar(
                name = name,
                onNameChange = { name = it },
                minHue = minHue,
                maxHue = maxHue,
                onHueChange = { a, b -> minHue = a; maxHue = b },
                showOnlyFavorites = fav,
                onToggleFavorites = { fav = !fav }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sample grid showing filtered result
            val previewColors = sampleColors.mapIndexed { idx, pair ->
                val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(pair.second, it) }
                ColorEntity(
                    id = idx + 1,
                    name = pair.first,
                    color = pair.second,
                    isFavorite = idx % 2 == 0,
                    hue = hsv[0]
                )
            }
            ColorGrid(colors = previewColors, onToggleFavorite = {}, onLongClick = {})
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * FilterBar contains input controls for filtering the color grid.
 *
 * It includes:
 * - A [TextField] to search by name.
 * - A hue range picker (RangeSlider over a hue gradient)
 * - A favorites toggle button.
 *
 * Params:
 * @param name current text query for filtering by name.
 * @param onNameChange callback to update the name query.
 * @param minHue current minimum hue value (degrees).
 * @param maxHue current maximum hue value (degrees).
 * @param onHueChange callback when the hue range changes (start, end).
 * @param showOnlyFavorites whether only favorite colors should be shown.
 * @param onToggleFavorites toggles the favorites-only mode.
 */
@Composable
fun FilterBar(
    name: String,
    onNameChange: (String) -> Unit,
    minHue: Float,
    maxHue: Float,
    onHueChange: (Float, Float) -> Unit,
    showOnlyFavorites: Boolean,
    onToggleFavorites: () -> Unit
) {
    var range by remember(minHue, maxHue) {
        mutableStateOf(minHue..maxHue)
    }
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            // Name filter
            TextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.filter_by_name)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (name.isNotEmpty()) {
                        IconButton(onClick = { onNameChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear filter"
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hue range sliders with hue-gradient background
            Text(text = "Hue range: ${minHue.toInt()}° - ${maxHue.toInt()}°", fontSize = 14.sp)

            // Build a hue gradient with stops every 10 degrees for smoothness
            val hueStops = remember {
                (0..360 step 10).map { h ->
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(h.toFloat(), 1f, 1f)))
                }
            }

            val gradientHeight: Dp = 36.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gradientHeight)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush = Brush.horizontalGradient(hueStops))
            ) {



                // RangeSlider: hue interval selector
                // - `value` is a ClosedFloatingPointRange<Float> (start = lower hue, endInclusive = upper hue).
                // - `valueRange = 0f..360f` maps the slider to hue degrees.
                // - `onValueChange` updates `range` while dragging; here it also calls
                //   `onHueChange(...)` so the parent receives live updates.
                // - Tracks are transparent so the hue gradient background shows through.
                // - `startThumb`/`endThumb` draw colored thumbs (via `SliderThumb`) using
                //   HSV->Color computed from the thumb positions for immediate visual feedback.
                RangeSlider(
                    value = range,
                    onValueChange = {
                        range = it
                        onHueChange(
                            range.start.coerceIn(0f, 360f),
                            range.endInclusive.coerceIn(0f, 360f)
                        )
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    startThumb = {
                        SliderThumb(
                            size = 10.dp,
                            color = Color(
                                android.graphics.Color.HSVToColor(
                                    floatArrayOf(
                                        range.start,
                                        1f,
                                        1f
                                    )
                                )
                            )
                        )
                    },
                    endThumb = {
                        SliderThumb(
                            size = 10.dp,
                            color = Color(
                                android.graphics.Color.HSVToColor(
                                    floatArrayOf(
                                        range.endInclusive,
                                        1f,
                                        1f
                                    )
                                )
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Favorites toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorites) {
                    Icon(
                        imageVector = if (showOnlyFavorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (showOnlyFavorites) stringResource(R.string.showing_only_favorites) else stringResource(
                            R.string.show_only_favorites
                        ),
                        tint = if (showOnlyFavorites) Color.Red else Color.Gray
                    )
                }
                Text(
                    text = if (showOnlyFavorites) stringResource(R.string.only_favorites) else stringResource(
                        R.string.all_colors
                    )
                )
            }
        }
    }
}

/**
 * Small circular thumb that can be used as a custom start/end thumb for
 * slider controls.
 *
 * @param size diameter of the thumb.
 * @param color fill color used for the thumb border.
 * @param modifier additional modifier to apply.
 */
@Composable
fun SliderThumb(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(size)
            .fillMaxHeight()
            .border(
                width = (size - 3.dp) / 2,
                color = color,
            )
            .background(
                color = Color.Black,
            ),
        contentAlignment = Alignment.Center
    ) {}

}

/**
 * Dialog that lets the user enter a color name and a hex value.
 *
 * The dialog validates the hex input and shows a live preview of the color
 * as the user types. On successful validation the provided [onAdd] callback
 * is invoked with the entered name and hex string.
 *
 * @param onDismiss called when the user cancels the dialog.
 * @param onAdd called with (name, hex) when the user confirms and the hex is valid.
 */
@Composable
fun AddColorDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, hex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hex by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // live preview color computed from current hex input (null when invalid/empty)
    var previewColor by remember { mutableStateOf<Color?>(null) }

    // helper to update preview when hex changes
    fun updatePreview(input: String) {
        val candidate = input.trim().let { if (!it.startsWith("#")) "#$it" else it }
        previewColor = try {
            Color(candidate.toColorInt())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                // basic validation for hex
                val candidate = hex.trim().let { if (!it.startsWith("#")) "#$it" else it }
                try {
                    // validate using KTX
                    candidate.toColorInt()
                    error = null
                    onAdd(name.trim(), hex.trim())
                } catch (_: IllegalArgumentException) {
                    error = "Invalid hex color"
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(text = stringResource(R.string.add_color)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Hex input with live preview to the right
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hex,
                        onValueChange = {
                            hex = it
                            updatePreview(it)
                        },
                        label = { Text(stringResource(R.string.color_format_text)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // preview box
                    val boxSize = 48.dp
                    val borderColor =
                        previewColor?.let { if (it.luminance() > 0.6f) Color.Black else Color.White }
                            ?: Color.Gray
                    Box(
                        modifier = Modifier
                            .size(boxSize)
                            .clip(RoundedCornerShape(6.dp))
                            .background(previewColor ?: Color.LightGray)
                            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
                    )
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = error ?: "", color = Color.Red)
                }
            }
        }
    )
}

/**
 * Simple UI variant that does not use DataStore.
 * Shows the same controls but keeps filters in-memory only.
 */
@Composable
fun ColorsScreenSimple(repository: pl.marrod.datastorageshowcase.data.ColorRepository) {
    DataStorageShowcaseTheme {
        val uiScope = rememberCoroutineScope()
        // Filter state
        var showOnlyFavorites by remember { mutableStateOf(false) }
        var nameQuery by remember { mutableStateOf("") }
        var minHue by remember { mutableFloatStateOf(0f) }
        var maxHue by remember { mutableFloatStateOf(360f) }

        // Add color dialog visibility
        var showAddDialog by remember { mutableStateOf(false) }

        Scaffold { innerPadding ->
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(innerPadding)
                ) {
                    // Collect all colors as a Flow and derive filtered colors
                    val allColors by repository.allColors.collectAsState(initial = emptyList())

                    // Seed DB once if empty (one-shot read)
                    LaunchedEffect(Unit) {
                        val once = repository.getAllOnce()
                        if (once.isEmpty()) {
                            repository.insertAll(sampleColors.map {
                                ColorEntity(
                                    name = it.first,
                                    color = it.second
                                )
                            })
                        }
                    }

                    // Derive filtered list from collected flow and UI filter state
                    /* Filtering: apply three checks to `allColors` —
                       name (case-insensitive contains), hue (min ≤ hue ≤ max),
                       and favorites (when enabled). Wrapped in `remember(...)`
                       so it recomputes only when inputs change. For large
                       datasets prefer DAO-side (SQL) filtering. */

                    val filteredColors =
                        remember(allColors, nameQuery, minHue, maxHue, showOnlyFavorites) {
                            allColors.filter { color ->
                                val matchesName = nameQuery.isBlank() || color.name.contains(
                                    nameQuery,
                                    ignoreCase = true
                                )
                                val matchesHue = color.hue >= minHue && color.hue <= maxHue
                                val matchesFav = !showOnlyFavorites || color.isFavorite
                                matchesName && matchesHue && matchesFav
                            }
                        }

                    if (filteredColors.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_colors),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        ColorGrid(
                            colors = filteredColors,
                            onToggleFavorite = { id -> uiScope.launch { repository.toggleFavorite(id) } },
                            onLongClick = { color -> uiScope.launch { repository.delete(color) } })
                    }
                }

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = stringResource(R.string.add_color))
                }

                if (showAddDialog) {
                    AddColorDialog(onDismiss = { showAddDialog = false }, onAdd = { name, hex ->
                        uiScope.launch {
                            try {
                                var h = hex.trim(); if (!h.startsWith("#")) h = "#$h"
                                val colorInt = h.toColorInt()
                                repository.insert(
                                    ColorEntity(
                                        name = name.ifBlank { "Unnamed" },
                                        color = colorInt
                                    )
                                )
                            } catch (_: Exception) {
                            }
                            showAddDialog = false
                        }
                    })
                }

                FilterBar(
                    name = nameQuery,
                    onNameChange = { nameQuery = it },
                    minHue = minHue,
                    maxHue = maxHue,
                    onHueChange = { start, end ->
                        minHue = start
                        maxHue = end
                    },
                    showOnlyFavorites = showOnlyFavorites,
                    onToggleFavorites = { showOnlyFavorites = !showOnlyFavorites })
            }
        }
    }
}

/**
 * DataStore-backed UI variant. Restores/persists filters and theme via [SettingsRepository].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorsScreenWithDataStore(
    repository: pl.marrod.datastorageshowcase.data.ColorRepository,
    settings: SettingsRepository
) {
    // Collect theme before applying
    val themeFromStore by settings.themeFlow.collectAsState(initial = "system")
    val darkTheme = when (themeFromStore) {
        "light" -> false; "dark" -> true; else -> isSystemInDarkTheme()
    }

    DataStorageShowcaseTheme(darkTheme = darkTheme) {
        val uiScope = rememberCoroutineScope()

        // persisted filters
        val nameFromStore by settings.nameQueryFlow.collectAsState(initial = "")
        val minHueFromStore by settings.minHueFlow.collectAsState(initial = 0f)
        val maxHueFromStore by settings.maxHueFlow.collectAsState(initial = 360f)
        val showFavFromStore by settings.showFavFlow.collectAsState(initial = false)

        var showOnlyFavorites by remember { mutableStateOf(showFavFromStore) }
        var nameQuery by remember { mutableStateOf(nameFromStore) }
        var minHue by remember { mutableFloatStateOf(minHueFromStore) }
        var maxHue by remember { mutableFloatStateOf(maxHueFromStore) }
        var showAddDialog by remember { mutableStateOf(false) }

        // keep in sync - when DataStore values change, update local state
        LaunchedEffect(nameFromStore) { nameQuery = nameFromStore }
        LaunchedEffect(minHueFromStore) { minHue = minHueFromStore }
        LaunchedEffect(maxHueFromStore) { maxHue = maxHueFromStore }
        LaunchedEffect(showFavFromStore) { showOnlyFavorites = showFavFromStore }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "Theme"
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.system)) },
                                    onClick = {
                                        expanded =
                                            false; uiScope.launch { settings.setTheme("system") }
                                    })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.light)) },
                                    onClick = {
                                        expanded =
                                            false; uiScope.launch { settings.setTheme("light") }
                                    })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.dark)) },
                                    onClick = {
                                        expanded =
                                            false; uiScope.launch { settings.setTheme("dark") }
                                    })
                                DropdownMenuItem(
                                    text = { Text("Restore Deleted") },
                                    onClick = {
                                        expanded =
                                            false;
                                        uiScope.launch {
                                            settings.restoreDeletedColors()
                                                .forEach { repository.insert(it) }
                                        }
                                    })
                            }
                        }
                    })
            }) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(innerPadding)
                ) {
                    val allColors by repository.allColors.collectAsState(initial = emptyList())

                    LaunchedEffect(Unit) {
                        val once = repository.getAllOnce()
                        if (once.isEmpty()) repository.insertAll(sampleColors.map {
                            ColorEntity(
                                name = it.first,
                                color = it.second
                            )
                        })
                    }

                    // Filtering: apply three checks to `allColors` —
                    // name (case-insensitive contains), hue (min ≤ hue ≤ max),
                    // and favorites (when enabled). Wrapped in `remember(...)` so
                    // it recomputes only when inputs change.
                    val filteredColors =
                        remember(allColors, nameQuery, minHue, maxHue, showOnlyFavorites) {
                            allColors.filter { color ->
                                val matchesName = nameQuery.isBlank() || color.name.contains(
                                    nameQuery,
                                    ignoreCase = true
                                )
                                val matchesHue = color.hue >= minHue && color.hue <= maxHue
                                val matchesFav = !showOnlyFavorites || color.isFavorite
                                matchesName && matchesHue && matchesFav
                            }
                        }

                    if (filteredColors.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_colors),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        ColorGrid(
                            colors = filteredColors,
                            onToggleFavorite = { id -> uiScope.launch { repository.toggleFavorite(id) } },
                            onLongClick = { color ->
                                uiScope.launch {
                                    // save deleted color to DataStore so it can be restored later
                                    settings.addDeletedColor(color)
                                    repository.delete(color)
                                }
                            })
                    }
                }

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text(text = stringResource(R.string.add_color)) }

                if (showAddDialog) {
                    AddColorDialog(onDismiss = { showAddDialog = false }, onAdd = { name, hex ->
                        uiScope.launch {
                            try {
                                var h = hex.trim()
                                if (!h.startsWith("#")) h = "#$h"
                                val colorInt = h.toColorInt(); repository.insert(
                                    ColorEntity(
                                        name = name.ifBlank { "Unnamed" },
                                        color = colorInt
                                    )
                                )
                            } catch (_: Exception) {
                            }
                            showAddDialog = false
                        }
                    })
                }

                FilterBar(
                    name = nameQuery,
                    onNameChange = { newName ->
                        nameQuery = newName; uiScope.launch { settings.setNameQuery(newName) }
                    },
                    minHue = minHue,
                    maxHue = maxHue,
                    onHueChange = { start, end ->
                        minHue = start
                        maxHue = end
                        if (minHue > maxHue) {
                            val tmp = minHue
                            minHue = maxHue
                            maxHue = tmp
                        }
                        uiScope.launch { settings.setHueRange(minHue, maxHue) }
                    },
                    showOnlyFavorites = showOnlyFavorites,
                    onToggleFavorites = {
                        showOnlyFavorites = !showOnlyFavorites; uiScope.launch {
                        settings.setShowFavorites(
                            showOnlyFavorites
                        )
                    }
                    })
            }
        }
    }
}




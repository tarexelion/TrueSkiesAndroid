package com.trueskies.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * Utility for loading images from the Android assets folder.
 * Assets are organized in subdirectories mirroring the iOS Assets.xcassets structure:
 *
 * - `aircraft/`         — Airline-specific aircraft images (e.g., `aal_b738.png`)
 * - `aircraft_generic/` — Generic aircraft type images (e.g., `a350.png`)
 * - `airline_logos/`    — Airline logo images (e.g., `turkish_airlines_logo.png`)
 * - `passport_emblems/` — Country emblems for passport/log (e.g., `turkey_emblem.png`)
 * - `seat_maps/`        — Aircraft seat map images
 */
object AssetImageLoader {

    /**
     * Load a Bitmap from assets, returning null if not found.
     */
    fun loadBitmap(context: Context, path: String): Bitmap? {
        return try {
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load an aircraft image by airline ICAO code and aircraft type ICAO code.
     * Falls back to generic aircraft image if airline-specific one is not found.
     *
     * @param airlineIcao e.g., "AAL" (American Airlines), "THY" (Turkish Airlines)
     * @param aircraftIcao e.g., "B738", "A321", "B789"
     */
    fun loadAircraftBitmap(context: Context, airlineIcao: String?, aircraftIcao: String?): Bitmap? {
        if (aircraftIcao == null) return null
        val typeCode = aircraftIcao.lowercase()

        // Try airline-specific image first
        if (airlineIcao != null) {
            val airlineCode = airlineIcao.lowercase()
            val specific = loadBitmap(context, "aircraft/${airlineCode}_${typeCode}.png")
            if (specific != null) return specific
        }

        // Fall back to generic aircraft type
        return loadBitmap(context, "aircraft_generic/${typeCode}.png")
    }

    /**
     * Load an airline logo by airline name.
     * Tries multiple name variations to find a match:
     * - Full name (e.g., "Turkish Airlines" → "turkish_airlines_logo.png")
     * - Without common suffixes like "Airlines", "Airways", "Air Lines"
     * - First word only (e.g., "Pegasus Airlines" → "pegasus_logo.png")
     */
    fun loadAirlineLogoBitmap(context: Context, airlineName: String?): Bitmap? {
        if (airlineName == null) return null

        val candidates = buildList {
            // Full name as-is
            add(airlineName)
            // Remove common suffixes
            val stripped = airlineName
                .replace(Regex("\\s+(Airlines?|Airways?|Air Lines?)\\s*$", RegexOption.IGNORE_CASE), "")
                .trim()
            if (stripped != airlineName) add(stripped)
            // First word only (e.g., "Pegasus" from "Pegasus Airlines")
            val firstWord = airlineName.split(" ").firstOrNull()
            if (firstWord != null && firstWord != stripped) add(firstWord)
        }

        for (candidate in candidates) {
            val sanitized = candidate.lowercase()
                .replace(Regex("[^a-z0-9]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
            val bitmap = loadBitmap(context, "airline_logos/${sanitized}_logo.png")
            if (bitmap != null) return bitmap
        }
        return null
    }

    /**
     * Load a country emblem by country name.
     * The emblem filenames follow the pattern: `{country_name}_emblem.png`
     */
    fun loadPassportEmblemBitmap(context: Context, countryName: String?): Bitmap? {
        if (countryName == null) return null
        val sanitized = countryName.lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return loadBitmap(context, "passport_emblems/${sanitized}_emblem.png")
    }

    /**
     * List all available files in an asset subdirectory.
     */
    fun listAssets(context: Context, directory: String): List<String> {
        return try {
            context.assets.list(directory)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Composable helper to load an asset image as ImageBitmap.
 * Returns null if the asset is not found.
 */
@Composable
fun rememberAssetImage(path: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(path) {
        AssetImageLoader.loadBitmap(context, path)?.asImageBitmap()
    }
}

/**
 * Composable helper to load an aircraft image.
 */
@Composable
fun rememberAircraftImage(airlineIcao: String?, aircraftIcao: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(airlineIcao, aircraftIcao) {
        AssetImageLoader.loadAircraftBitmap(context, airlineIcao, aircraftIcao)?.asImageBitmap()
    }
}

/**
 * Composable helper to load an airline logo.
 */
@Composable
fun rememberAirlineLogo(airlineName: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(airlineName) {
        AssetImageLoader.loadAirlineLogoBitmap(context, airlineName)?.asImageBitmap()
    }
}

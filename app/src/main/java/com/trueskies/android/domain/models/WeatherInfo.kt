package com.trueskies.android.domain.models

/**
 * Domain weather model — matches iOS WeatherInfo struct.
 */
data class WeatherInfo(
    val airportCode: String,
    val temperature: Int,
    val feelsLike: Int,
    val conditions: String,
    val windSpeed: Int?,
    val windDirection: String?,
    val humidity: Int?
) {
    companion object {
        /**
         * Map WMO weather codes to human-readable conditions.
         * https://open-meteo.com/en/docs#weathervariables
         */
        fun conditionsFromWmoCode(code: Int?): String = when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with Hail"
            else -> "Unknown"
        }

        /**
         * Convert wind direction degrees to cardinal direction.
         */
        fun cardinalDirection(degrees: Double?): String? {
            if (degrees == null) return null
            val dirs = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
            val index = ((degrees + 11.25) / 22.5).toInt() % 16
            return dirs[index]
        }
    }
}

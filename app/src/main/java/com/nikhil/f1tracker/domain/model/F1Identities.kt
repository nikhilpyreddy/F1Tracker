package com.nikhil.f1tracker.domain.model

/** How a driver is shown: 3-letter code badge, current team colour, optional headshot. */
data class DriverLook(val code: String?, val colorArgb: Long?, val headshotUrl: String?)

/**
 * Colours and faces for drivers and teams. A result row's own team colour wins over the driver's
 * current team, so history shows the car they actually drove.
 */
data class F1Identities(
    val drivers: Map<String, DriverLook> = emptyMap(),
    val teamColors: Map<String, Long> = emptyMap(),
) {
    fun teamColor(constructorId: String?): Long? =
        constructorId?.let { teamColors[it] ?: FALLBACK_TEAM_COLORS[it] }

    fun driverColor(driverId: String, constructorId: String? = null): Long? =
        teamColor(constructorId) ?: drivers[driverId]?.colorArgb

    fun driver(driverId: String): DriverLook? = drivers[driverId]
}

/** Recent livery colours, for teams no longer on the current grid (or before OpenF1 loads). */
private val FALLBACK_TEAM_COLORS: Map<String, Long> = mapOf(
    "red_bull" to 0xFF4781D7,
    "ferrari" to 0xFFED1131,
    "mercedes" to 0xFF00D7B6,
    "mclaren" to 0xFFF47600,
    "aston_martin" to 0xFF229971,
    "alpine" to 0xFF00A1E8,
    "williams" to 0xFF1868DB,
    "rb" to 0xFF6C98FF,
    "haas" to 0xFF9C9FA2,
    "audi" to 0xFFF50537,
    "cadillac" to 0xFF909090,
    "sauber" to 0xFF52E252,
    "alfa" to 0xFF9B0000,
    "alphatauri" to 0xFF5E8FAA,
    "toro_rosso" to 0xFF469BFF,
    "racing_point" to 0xFFF596C8,
    "force_india" to 0xFFF596C8,
    "renault" to 0xFFFFF500,
    "lotus_f1" to 0xFFFFB800,
)

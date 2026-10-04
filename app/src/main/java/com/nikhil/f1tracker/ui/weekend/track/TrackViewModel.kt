package com.nikhil.f1tracker.ui.weekend.track

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.remote.openmeteo.OpenMeteoApiService
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.data.repository.OpenF1Repository
import com.nikhil.f1tracker.domain.model.CIRCUIT_TYPES
import com.nikhil.f1tracker.domain.model.CircuitType
import com.nikhil.f1tracker.domain.stats.TrackProfile
import com.nikhil.f1tracker.domain.stats.trackProfile
import com.nikhil.f1tracker.ui.common.syncCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherDay(val date: LocalDate, val rainChance: Int?, val maxTemperature: Double?, val maxWind: Double?)

data class TrackUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val circuitType: CircuitType? = null,
    val profile: TrackProfile? = null,
    val poleLapLabel: String? = null,
    val raceLaps: Int? = null,
    val weather: List<WeatherDay> = emptyList(),
    val weatherNote: String? = null,
)

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val f1Repository: F1Repository,
    private val openF1Repository: OpenF1Repository,
    private val openMeteo: OpenMeteoApiService,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val season: Int = checkNotNull(savedStateHandle["season"])
    private val round: Int = checkNotNull(savedStateHandle["round"])
    private val circuitId: String = checkNotNull(savedStateHandle["circuitId"])

    private val state = MutableStateFlow(TrackUiState(circuitType = CIRCUIT_TYPES[circuitId]))
    val uiState: StateFlow<TrackUiState> = state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, loadErrorMessage = null) }
            state.update { it.copy(raceLaps = latestRaceLaps()) }
            loadWeather()
            syncCatching {
                val lap = openF1Repository.poleLap(circuitId)
                state.update {
                    it.copy(
                        profile = lap?.let { data -> trackProfile(data.car, data.location) },
                        poleLapLabel = lap?.let { data ->
                            "${data.driverCode ?: "Pole"}'s ${data.year} pole lap · ${formatLapTime(data.lapSeconds)}"
                        },
                    )
                }
            }.onFailure {
                state.update { it.copy(loadErrorMessage = "Couldn't load track telemetry. Check your connection and try again.") }
            }
            state.update { it.copy(isLoading = false) }
        }
    }

    /** Race distance in laps, from the most recent race here (already cached for the Circuit tab). */
    private suspend fun latestRaceLaps(): Int? = f1Repository.getResultsAtCircuit(circuitId).first()
        .filter { it.positionText == "1" }
        .maxByOrNull { it.season }
        ?.laps

    private suspend fun loadWeather() {
        val race = f1Repository.getRacesForSeason(season).first().find { it.round == round } ?: return
        val circuit = f1Repository.getCircuit(circuitId).first() ?: return
        val raceDay = runCatching { LocalDate.parse(race.date) }.getOrNull() ?: return
        val daysAway = ChronoUnit.DAYS.between(LocalDate.now(clock), raceDay)
        when {
            daysAway < 0 -> return
            daysAway > OpenMeteoApiService.FORECAST_DAYS -> {
                state.update { it.copy(weatherNote = "Forecast available from ${raceDay.minusDays(OpenMeteoApiService.FORECAST_DAYS)}") }
                return
            }
        }
        syncCatching {
            val daily = openMeteo.getDailyForecast(
                circuit.latitude, circuit.longitude,
                startDate = raceDay.minusDays(2).toString(), endDate = raceDay.toString(),
            ).daily ?: return@syncCatching
            val days = daily.time.mapIndexed { i, date ->
                WeatherDay(LocalDate.parse(date), daily.rainChance.getOrNull(i), daily.maxTemperature.getOrNull(i), daily.maxWind.getOrNull(i))
            }
            state.update { it.copy(weather = days) }
        }.onFailure { state.update { it.copy(weatherNote = "Forecast unavailable right now") } }
    }

    private fun formatLapTime(seconds: Double): String {
        val minutes = (seconds / SECONDS_PER_MINUTE).toInt()
        return "%d:%06.3f".format(java.util.Locale.US, minutes, seconds - minutes * SECONDS_PER_MINUTE)
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60
    }
}

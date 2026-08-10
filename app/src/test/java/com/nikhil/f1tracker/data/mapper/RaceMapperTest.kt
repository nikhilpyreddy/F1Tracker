package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.remote.dto.CircuitDto
import com.nikhil.f1tracker.data.remote.dto.LocationDto
import com.nikhil.f1tracker.data.remote.dto.RaceDto
import org.junit.Assert.assertEquals
import org.junit.Test

class RaceMapperTest {

    @Test
    fun `toEntity parses season and round to int and takes circuit id`() {
        // Arrange
        val dto = RaceDto(
            season = "2024",
            round = "1",
            raceName = "Bahrain Grand Prix",
            circuit = CircuitDto(
                circuitId = "bahrain",
                circuitName = "Bahrain International Circuit",
                location = LocationDto(lat = "26.0325", long = "50.5106", locality = "Sakhir", country = "Bahrain"),
            ),
            date = "2024-03-02",
            time = "15:00:00Z",
        )

        // Act
        val entity = dto.toEntity()

        // Assert
        assertEquals(2024, entity.season)
        assertEquals(1, entity.round)
        assertEquals("Bahrain Grand Prix", entity.raceName)
        assertEquals("bahrain", entity.circuitId)
        assertEquals("2024-03-02", entity.date)
        assertEquals("15:00:00Z", entity.time)
    }
}

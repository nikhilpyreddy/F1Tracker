package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.remote.dto.CircuitDto
import com.nikhil.f1tracker.data.remote.dto.LocationDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CircuitMapperTest {

    @Test
    fun `toEntity flattens nested location fields and parses coordinates`() {
        // Arrange
        val dto = CircuitDto(
            circuitId = "bahrain",
            circuitName = "Bahrain International Circuit",
            location = LocationDto(lat = "26.0325", long = "50.5106", locality = "Sakhir", country = "Bahrain"),
        )

        // Act
        val entity = dto.toEntity()

        // Assert
        assertEquals("bahrain", entity.circuitId)
        assertEquals("Bahrain International Circuit", entity.circuitName)
        assertEquals("Sakhir", entity.locality)
        assertEquals("Bahrain", entity.country)
        assertEquals(26.0325, entity.latitude, 0.0001)
        assertEquals(50.5106, entity.longitude, 0.0001)
    }

    @Test
    fun `toEntity defaults unparseable coordinates to zero`() {
        // Arrange
        val dto = CircuitDto(
            circuitId = "unknown",
            circuitName = "Unknown Circuit",
            location = LocationDto(lat = "n/a", long = "n/a", locality = "Nowhere", country = "Nowhere"),
        )

        // Act
        val entity = dto.toEntity()

        // Assert
        assertEquals(0.0, entity.latitude, 0.0001)
        assertEquals(0.0, entity.longitude, 0.0001)
    }
}

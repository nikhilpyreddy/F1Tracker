package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.remote.dto.DriverDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriverMapperTest {

    @Test
    fun `toEntity maps all fields and parses permanent number to int`() {
        // Arrange
        val dto = DriverDto(
            driverId = "max_verstappen",
            permanentNumber = "3",
            code = "VER",
            url = "http://example.com",
            givenName = "Max",
            familyName = "Verstappen",
            dateOfBirth = "1997-09-30",
            nationality = "Dutch",
        )

        // Act
        val entity = dto.toEntity()

        // Assert
        assertEquals("max_verstappen", entity.driverId)
        assertEquals(3, entity.permanentNumber)
        assertEquals("VER", entity.code)
        assertEquals("Max", entity.givenName)
        assertEquals("Verstappen", entity.familyName)
        assertEquals("1997-09-30", entity.dateOfBirth)
        assertEquals("Dutch", entity.nationality)
    }

    @Test
    fun `toEntity maps null permanent number when absent`() {
        // Arrange
        val dto = DriverDto(
            driverId = "de_vries",
            permanentNumber = null,
            code = "DEV",
            givenName = "Nyck",
            familyName = "de Vries",
            dateOfBirth = "1995-02-06",
            nationality = "Dutch",
        )

        // Act
        val entity = dto.toEntity()

        // Assert
        assertNull(entity.permanentNumber)
    }

    @Test
    fun `toEntity maps a reserve driver with no bio data at all`() {
        // Arrange: the season roster endpoint lists reserve/test drivers who have never
        // raced with only driverId, givenName and familyName populated.
        val dto = DriverDto(
            driverId = "paul_aron",
            givenName = "Paul",
            familyName = "Aron",
        )

        // Act
        val entity = dto.toEntity()

        // Assert
        assertEquals("paul_aron", entity.driverId)
        assertNull(entity.permanentNumber)
        assertNull(entity.code)
        assertNull(entity.dateOfBirth)
        assertNull(entity.nationality)
    }
}

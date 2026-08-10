package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.remote.dto.ConstructorDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorStandingDto
import com.nikhil.f1tracker.data.remote.dto.DriverDto
import com.nikhil.f1tracker.data.remote.dto.DriverStandingDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StandingMapperTest {

    private val driver = DriverDto(
        driverId = "max_verstappen",
        givenName = "Max",
        familyName = "Verstappen",
        dateOfBirth = "1997-09-30",
        nationality = "Dutch",
    )
    private val constructor = ConstructorDto(constructorId = "red_bull", name = "Red Bull", nationality = "Austrian")

    @Test
    fun `driver standing toEntity takes the first constructor as primary`() {
        // Arrange
        val dto = DriverStandingDto(
            position = "1",
            positionText = "1",
            points = "437",
            wins = "9",
            driver = driver,
            constructors = listOf(constructor),
        )

        // Act
        val entity = dto.toEntity(season = 2024)

        // Assert
        assertEquals(2024, entity.season)
        assertEquals("max_verstappen", entity.driverId)
        assertEquals("red_bull", entity.constructorId)
        assertEquals(1, entity.position)
        assertEquals(437.0, entity.points, 0.0001)
        assertEquals(9, entity.wins)
    }

    @Test
    fun `driver standing toEntity maps null constructor when list is empty`() {
        // Arrange
        val dto = DriverStandingDto(
            position = "1",
            positionText = "1",
            points = "0",
            wins = "0",
            driver = driver,
            constructors = emptyList(),
        )

        // Act
        val entity = dto.toEntity(season = 2024)

        // Assert
        assertNull(entity.constructorId)
    }

    @Test
    fun `constructor standing toEntity maps fields`() {
        // Arrange
        val dto = ConstructorStandingDto(
            position = "1",
            positionText = "1",
            points = "666",
            wins = "6",
            constructor = constructor,
        )

        // Act
        val entity = dto.toEntity(season = 2024)

        // Assert
        assertEquals(2024, entity.season)
        assertEquals("red_bull", entity.constructorId)
        assertEquals(1, entity.position)
        assertEquals(666.0, entity.points, 0.0001)
        assertEquals(6, entity.wins)
    }
}

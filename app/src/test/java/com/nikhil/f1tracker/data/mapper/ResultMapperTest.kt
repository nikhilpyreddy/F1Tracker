package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.remote.dto.ConstructorDto
import com.nikhil.f1tracker.data.remote.dto.DriverDto
import com.nikhil.f1tracker.data.remote.dto.FastestLapDto
import com.nikhil.f1tracker.data.remote.dto.FastestLapTimeDto
import com.nikhil.f1tracker.data.remote.dto.ResultDto
import com.nikhil.f1tracker.data.remote.dto.ResultTimeDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResultMapperTest {

    private val driver = DriverDto(
        driverId = "max_verstappen",
        givenName = "Max",
        familyName = "Verstappen",
        dateOfBirth = "1997-09-30",
        nationality = "Dutch",
    )
    private val constructor = ConstructorDto(constructorId = "red_bull", name = "Red Bull", nationality = "Austrian")

    @Test
    fun `toEntity maps a race winner including fastest lap and finish time`() {
        // Arrange
        val dto = ResultDto(
            position = "1",
            positionText = "1",
            points = "26",
            driver = driver,
            constructor = constructor,
            grid = "1",
            laps = "57",
            status = "Finished",
            time = ResultTimeDto(millis = "5504742", time = "1:31:44.742"),
            fastestLap = FastestLapDto(rank = "1", lap = "39", time = FastestLapTimeDto(time = "1:32.608")),
        )

        // Act
        val entity = dto.toEntity(season = 2024, round = 1)

        // Assert
        assertEquals(2024, entity.season)
        assertEquals(1, entity.round)
        assertEquals("max_verstappen", entity.driverId)
        assertEquals("red_bull", entity.constructorId)
        assertEquals(1, entity.position)
        assertEquals(26.0, entity.points, 0.0001)
        assertEquals(1, entity.grid)
        assertEquals(57, entity.laps)
        assertEquals("Finished", entity.status)
        assertEquals(5504742L, entity.finishTimeMillis)
        assertEquals(1, entity.fastestLapRank)
        assertEquals("1:32.608", entity.fastestLapTime)
    }

    @Test
    fun `toEntity maps a retired driver with no time or fastest lap`() {
        // Arrange
        val dto = ResultDto(
            position = "20",
            positionText = "R",
            points = "0",
            driver = driver,
            constructor = constructor,
            grid = "10",
            laps = "12",
            status = "Retired",
            time = null,
            fastestLap = null,
        )

        // Act
        val entity = dto.toEntity(season = 2024, round = 1)

        // Assert
        assertEquals("R", entity.positionText)
        assertEquals(0.0, entity.points, 0.0001)
        assertNull(entity.finishTimeMillis)
        assertNull(entity.fastestLapRank)
        assertNull(entity.fastestLapTime)
    }
}

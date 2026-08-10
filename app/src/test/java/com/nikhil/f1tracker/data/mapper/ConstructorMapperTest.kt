package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.remote.dto.ConstructorDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ConstructorMapperTest {

    @Test
    fun `toEntity maps constructor fields`() {
        // Arrange
        val dto = ConstructorDto(
            constructorId = "red_bull",
            url = "http://example.com",
            name = "Red Bull",
            nationality = "Austrian",
        )

        // Act
        val entity = dto.toEntity()

        // Assert
        assertEquals("red_bull", entity.constructorId)
        assertEquals("Red Bull", entity.name)
        assertEquals("Austrian", entity.nationality)
    }
}

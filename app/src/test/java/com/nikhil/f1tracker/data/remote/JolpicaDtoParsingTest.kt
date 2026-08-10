package com.nikhil.f1tracker.data.remote

import com.nikhil.f1tracker.data.remote.dto.ConstructorStandingsResponseDto
import com.nikhil.f1tracker.data.remote.dto.DriverStandingsResponseDto
import com.nikhil.f1tracker.data.remote.dto.RaceResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Decodes real sample payloads captured from https://api.jolpi.ca/ergast/f1/ to guard
 * against DTO/schema drift from the live Jolpica-F1 API.
 */
class JolpicaDtoParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes race results response with a finisher and fastest lap`() {
        // Arrange
        val body = """
            {"MRData":{"xmlns":"","series":"f1","url":"https://api.jolpi.ca/ergast/f1/2024/1/results.json",
            "limit":"2","offset":"0","total":"20","RaceTable":{"season":"2024","round":"1","Races":[
            {"season":"2024","round":"1","url":"https://en.wikipedia.org/wiki/2024_Bahrain_Grand_Prix",
            "raceName":"Bahrain Grand Prix","Circuit":{"circuitId":"bahrain",
            "url":"https://en.wikipedia.org/wiki/Bahrain_International_Circuit",
            "circuitName":"Bahrain International Circuit",
            "Location":{"lat":"26.0325","long":"50.5106","locality":"Sakhir","country":"Bahrain"}},
            "date":"2024-03-02","time":"15:00:00Z","Results":[{"number":"1","position":"1","positionText":"1",
            "points":"26","Driver":{"driverId":"max_verstappen","permanentNumber":"3","code":"VER",
            "url":"http://en.wikipedia.org/wiki/Max_Verstappen","givenName":"Max","familyName":"Verstappen",
            "dateOfBirth":"1997-09-30","nationality":"Dutch"},"Constructor":{"constructorId":"red_bull",
            "url":"https://en.wikipedia.org/wiki/Red_Bull_Racing","name":"Red Bull","nationality":"Austrian"},
            "grid":"1","laps":"57","status":"Finished","Time":{"millis":"5504742","time":"1:31:44.742"},
            "FastestLap":{"rank":"1","lap":"39","Time":{"time":"1:32.608"},
            "AverageSpeed":{"units":"kph","speed":"210.383"}}}]}]}}}
        """.trimIndent()

        // Act
        val response = json.decodeFromString<RaceResponseDto>(body)
        val race = response.mrData.raceTable.races.single()
        val result = race.results.single()

        // Assert
        assertEquals("bahrain", race.circuit.circuitId)
        assertEquals("max_verstappen", result.driver.driverId)
        assertEquals("red_bull", result.constructor.constructorId)
        assertEquals("1:32.608", result.fastestLap?.time?.time)
    }

    @Test
    fun `decodes driver standings response`() {
        // Arrange
        val body = """
            {"MRData":{"xmlns":"","series":"f1","url":"https://api.jolpi.ca/ergast/f1/2024/driverstandings.json",
            "limit":"2","offset":"0","total":"24","StandingsTable":{"season":"2024","round":"24",
            "StandingsLists":[{"season":"2024","round":"24","DriverStandings":[{"position":"1",
            "positionText":"1","points":"437","wins":"9","Driver":{"driverId":"max_verstappen",
            "permanentNumber":"3","code":"VER","url":"http://en.wikipedia.org/wiki/Max_Verstappen",
            "givenName":"Max","familyName":"Verstappen","dateOfBirth":"1997-09-30","nationality":"Dutch"},
            "Constructors":[{"constructorId":"red_bull","url":"https://en.wikipedia.org/wiki/Red_Bull_Racing",
            "name":"Red Bull","nationality":"Austrian"}]}]}]}}}
        """.trimIndent()

        // Act
        val response = json.decodeFromString<DriverStandingsResponseDto>(body)
        val standing = response.mrData.standingsTable.standingsLists.single().driverStandings.single()

        // Assert
        assertEquals("max_verstappen", standing.driver.driverId)
        assertEquals("437", standing.points)
        assertEquals("red_bull", standing.constructors.single().constructorId)
    }

    @Test
    fun `decodes constructor standings response`() {
        // Arrange
        val body = """
            {"MRData":{"xmlns":"","series":"f1",
            "url":"https://api.jolpi.ca/ergast/f1/2024/constructorstandings.json","limit":"2","offset":"0",
            "total":"10","StandingsTable":{"season":"2024","round":"24","StandingsLists":[{"season":"2024",
            "round":"24","ConstructorStandings":[{"position":"1","positionText":"1","points":"666","wins":"6",
            "Constructor":{"constructorId":"mclaren","url":"https://en.wikipedia.org/wiki/McLaren",
            "name":"McLaren","nationality":"British"}}]}]}}}
        """.trimIndent()

        // Act
        val response = json.decodeFromString<ConstructorStandingsResponseDto>(body)
        val standing = response.mrData.standingsTable.standingsLists.single().constructorStandings.single()

        // Assert
        assertEquals("mclaren", standing.constructor.constructorId)
        assertEquals("666", standing.points)
    }
}

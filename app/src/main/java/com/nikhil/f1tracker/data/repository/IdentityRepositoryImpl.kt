package com.nikhil.f1tracker.data.repository

import com.nikhil.f1tracker.data.local.dao.DriverDao
import com.nikhil.f1tracker.data.local.dao.DriverIdentityDao
import com.nikhil.f1tracker.data.local.dao.DriverStandingDao
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.DriverIdentityEntity
import com.nikhil.f1tracker.data.remote.openf1.OpenF1ApiService
import com.nikhil.f1tracker.domain.model.DriverLook
import com.nikhil.f1tracker.domain.model.F1Identities
import java.text.Normalizer
import java.time.Clock
import java.time.Year
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class IdentityRepositoryImpl @Inject constructor(
    private val openF1: OpenF1ApiService,
    private val identityDao: DriverIdentityDao,
    driverDao: DriverDao,
    driverStandingDao: DriverStandingDao,
    clock: Clock,
) : IdentityRepository {

    // Once per process is plenty: liveries only change between seasons, and the cached table
    // keeps colours working offline.
    private var hasSynced = false

    override val identities: Flow<F1Identities> = combine(
        identityDao.getAll(),
        driverDao.getAll(),
        driverStandingDao.getBySeason(Year.now(clock).value),
    ) { identities, drivers, standings ->
        val looks = matchLooks(identities, drivers)
        val teamColors = standings
            .mapNotNull { standing ->
                val color = looks[standing.driverId]?.colorArgb
                val constructorId = standing.constructorId
                if (color != null && constructorId != null) constructorId to color else null
            }
            .toMap()
        F1Identities(looks, teamColors)
    }

    override suspend fun syncIdentities() {
        if (hasSynced) return
        val drivers = openF1.getDrivers()
        identityDao.upsertAll(
            drivers.map {
                DriverIdentityEntity(it.nameAcronym, it.lastName, it.teamName, it.teamColour, it.headshotUrl)
            },
        )
        hasSynced = true
    }

    private fun matchLooks(
        identities: List<DriverIdentityEntity>,
        drivers: List<DriverEntity>,
    ): Map<String, DriverLook> {
        val identitiesByCode = identities.associateBy { it.code }
        return drivers.associate { driver ->
            val identity = driver.code?.let { identitiesByCode[it] }?.takeIf { it.isSamePersonAs(driver) }
            driver.driverId to DriverLook(
                code = driver.code,
                colorArgb = identity?.teamColour?.toArgbOrNull(),
                headshotUrl = identity?.headshotUrl,
            )
        }
    }

    // Codes get reused across eras (two Schumachers were both "MSC"), so the surname must agree too.
    private fun DriverIdentityEntity.isSamePersonAs(driver: DriverEntity): Boolean =
        lastName == null || lastName.normalizedName() == driver.familyName.normalizedName()

    private fun String.normalizedName(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase()

    private fun String.toArgbOrNull(): Long? =
        toLongOrNull(HEX_RADIX)?.takeIf { length == RGB_HEX_LENGTH }?.let { OPAQUE_ALPHA or it }

    private companion object {
        val DIACRITICS = Regex("\\p{Mn}+")
        const val HEX_RADIX = 16
        const val RGB_HEX_LENGTH = 6
        const val OPAQUE_ALPHA = 0xFF000000
    }
}

package com.nikhil.f1tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nikhil.f1tracker.data.local.dao.CircuitDao
import com.nikhil.f1tracker.data.local.dao.ConstructorDao
import com.nikhil.f1tracker.data.local.dao.ConstructorStandingDao
import com.nikhil.f1tracker.data.local.dao.DriverDao
import com.nikhil.f1tracker.data.local.dao.DriverStandingDao
import com.nikhil.f1tracker.data.local.dao.RaceDao
import com.nikhil.f1tracker.data.local.dao.ResultDao
import com.nikhil.f1tracker.data.local.entity.CircuitEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorStandingEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.DriverStandingEntity
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity

@Database(
    entities = [
        DriverEntity::class,
        ConstructorEntity::class,
        CircuitEntity::class,
        RaceEntity::class,
        ResultEntity::class,
        DriverStandingEntity::class,
        ConstructorStandingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class F1Database : RoomDatabase() {
    abstract fun driverDao(): DriverDao
    abstract fun constructorDao(): ConstructorDao
    abstract fun circuitDao(): CircuitDao
    abstract fun raceDao(): RaceDao
    abstract fun resultDao(): ResultDao
    abstract fun driverStandingDao(): DriverStandingDao
    abstract fun constructorStandingDao(): ConstructorStandingDao

    companion object {
        const val DATABASE_NAME = "f1_tracker.db"
    }
}

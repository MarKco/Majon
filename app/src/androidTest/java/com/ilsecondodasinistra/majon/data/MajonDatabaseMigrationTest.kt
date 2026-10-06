package com.ilsecondodasinistra.majon.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import org.junit.Rule
import org.junit.Test

/**
 * Add one test per schema bump: create the old version from schemas/<n>.json,
 * insert sample rows, then runMigrationsAndValidate against the new version.
 * Never use fallbackToDestructiveMigration: it would wipe the user's projects.
 */
class MajonDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MajonDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun createsVersion1Schema() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, 1, true).close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}

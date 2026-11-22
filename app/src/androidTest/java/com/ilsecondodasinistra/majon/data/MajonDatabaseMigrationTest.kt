package com.ilsecondodasinistra.majon.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.ilsecondodasinistra.majon.data.db.MIGRATION_1_2
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import org.junit.Assert.assertEquals
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

    @Test
    fun migrate1To2KeepsPartsAndAddsExtraRowColumns() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO projects (id, name, icon, color, yarnType, needleSize, createdAt, updatedAt) " +
                    "VALUES (1, 'Maglione', 'SWEATER', 'SAGE', 'Merino', '4', 10, 20)",
            )
            execSQL(
                "INSERT INTO parts (id, projectId, name, totalRows, completedRows, createdAt) " +
                    "VALUES (1, 1, 'Davanti', 120, 42, 30)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        db.query("SELECT name, totalRows, completedRows, hasPrepRow, hasFinalRow, prepNote, finalNote FROM parts").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals("Davanti", c.getString(0))
            assertEquals(120, c.getInt(1))
            assertEquals(42, c.getInt(2))
            assertEquals(0, c.getInt(3))
            assertEquals(0, c.getInt(4))
            assertEquals(true, c.isNull(5))
            assertEquals(true, c.isNull(6))
        }
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}

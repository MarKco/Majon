package com.ilsecondodasinistra.majon.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.ilsecondodasinistra.majon.data.db.MIGRATION_1_2
import com.ilsecondodasinistra.majon.data.db.MIGRATION_2_3
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

    @Test
    fun migrate2To3KeepsProjectsAndCreatesTheStashTables() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO projects (id, name, icon, color, yarnType, needleSize, createdAt, updatedAt) " +
                    "VALUES (1, 'Sciarpa', 'SCARF', 'SAGE', 'Merino', '4', 10, 20)",
            )
            execSQL(
                "INSERT INTO parts (id, projectId, name, totalRows, completedRows, createdAt, hasPrepRow, hasFinalRow) " +
                    "VALUES (1, 1, 'Unica', 200, 12, 30, 1, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
        db.query("SELECT name, yarnType FROM projects").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals("Sciarpa", c.getString(0))
            assertEquals("Merino", c.getString(1))
        }
        db.query("SELECT completedRows, hasPrepRow FROM parts").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals(12, c.getInt(0))
            assertEquals(1, c.getInt(1))
        }
        // The new tables are usable, and a yarn's blend and swatches follow it when deleted
        db.execSQL(
            "INSERT INTO yarns (id, name, stockSkeins, createdAt, updatedAt) VALUES (1, 'Bamboo', 2.5, 1, 1)",
        )
        db.execSQL("INSERT INTO yarn_materials (yarnId, material, position) VALUES (1, 'BAMBOO', 0)")
        db.execSQL("INSERT INTO swatches (yarnId, stitches, rows, pattern) VALUES (1, 26, 34, 'GARTER')")
        db.execSQL("INSERT INTO needles (type, quantity) VALUES ('CIRCULAR', 1)")
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM yarns WHERE id = 1")
        db.query("SELECT COUNT(*) FROM swatches").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}

package com.ilsecondodasinistra.majon.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.ilsecondodasinistra.majon.data.db.MIGRATION_1_2
import com.ilsecondodasinistra.majon.data.db.MIGRATION_2_3
import com.ilsecondodasinistra.majon.data.db.MIGRATION_3_4
import com.ilsecondodasinistra.majon.data.db.MIGRATION_4_5
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

    @Test
    fun migrate3To4KeepsPartsAndAddsMeasuresAndLinks() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                "INSERT INTO projects (id, name, icon, color, yarnType, needleSize, createdAt, updatedAt) " +
                    "VALUES (1, 'Sciarpa', 'SCARF', 'SAGE', NULL, NULL, 10, 20)",
            )
            execSQL(
                "INSERT INTO parts (id, projectId, name, totalRows, completedRows, createdAt, hasPrepRow, hasFinalRow) " +
                    "VALUES (1, 1, 'Unica', 200, 12, 30, 0, 0)",
            )
            execSQL("INSERT INTO yarns (id, name, stockSkeins, createdAt, updatedAt) VALUES (1, 'Bamboo', 2.5, 1, 1)")
            execSQL("INSERT INTO needles (id, type, quantity) VALUES (1, 'CIRCULAR', 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)
        db.query("SELECT completedRows, stitchMeasure, stitchCm, rowMeasure, rowCm, swatchId FROM parts").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals(12, c.getInt(0))
            for (i in 1..5) assertEquals(true, c.isNull(i))
        }
        db.execSQL("UPDATE parts SET stitchMeasure = 'WIDTH', stitchCm = 20.0, rowMeasure = 'LENGTH', rowCm = 150.0 WHERE id = 1")
        db.execSQL("INSERT INTO project_yarns (projectId, yarnId, isPrimary) VALUES (1, 1, 1)")
        db.execSQL("INSERT INTO project_needles (projectId, needleId) VALUES (1, 1)")
        // Links follow their project and their yarn / needle when those are deleted
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM yarns WHERE id = 1")
        db.query("SELECT COUNT(*) FROM project_yarns").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.execSQL("DELETE FROM projects WHERE id = 1")
        db.query("SELECT COUNT(*) FROM project_needles").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate4To5KeepsPartsAndAddsCopiesStitchesAndFinishingTasks() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO projects (id, name, icon, color, yarnType, needleSize, createdAt, updatedAt) " +
                    "VALUES (1, 'Maglione', 'SWEATER', 'SAGE', NULL, NULL, 10, 20)",
            )
            execSQL(
                "INSERT INTO parts (id, projectId, name, totalRows, completedRows, createdAt, hasPrepRow, hasFinalRow) " +
                    "VALUES (1, 1, 'Manica', 190, 12, 30, 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)
        db.query("SELECT completedRows, copies, currentCopy, castOnStitches, endStitchCm FROM parts").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals(12, c.getInt(0))
            assertEquals(1, c.getInt(1))
            assertEquals(1, c.getInt(2))
            assertEquals(true, c.isNull(3))
            assertEquals(true, c.isNull(4))
        }
        db.execSQL("INSERT INTO finishing_tasks (projectId, title) VALUES (1, 'Fili')")
        db.query("SELECT done, position FROM finishing_tasks").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals(0, c.getInt(0))
            assertEquals(0, c.getInt(1))
        }
        // Tasks follow their project
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM projects WHERE id = 1")
        db.query("SELECT COUNT(*) FROM finishing_tasks").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}

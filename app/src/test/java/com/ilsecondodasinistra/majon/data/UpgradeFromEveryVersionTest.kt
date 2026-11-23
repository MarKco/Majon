package com.ilsecondodasinistra.majon.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ilsecondodasinistra.majon.data.db.MIGRATIONS
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Updating the app must never lose or break a project. This builds the database as every earlier version
 * of the app left it (from the exported schemas), puts a project, a part with progress and a note in it, then opens
 * it with the current database and all the migrations: Room itself checks the result against the current schema,
 * and the data has to be there, with the new fields at their neutral values.
 */
@RunWith(RobolectricTestRunner::class)
class UpgradeFromEveryVersionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "upgrade-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun schemaDir(): File {
        // Gradle runs unit tests from the module folder
        val dir = File("schemas").listFiles { f -> f.isDirectory }?.firstOrNull()
        return checkNotNull(dir) { "schemas folder not found from ${File(".").absolutePath}" }
    }

    /** Creates the database exactly as version [version] of the app did, with some data in it. */
    private fun createOldDatabase(version: Int) {
        val schema = JSONObject(File(schemaDir(), "$version.json").readText()).getJSONObject("database")
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        file.delete()
        val sqlite = SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices")
            if (indices != null) {
                for (j in 0 until indices.length()) {
                    sqlite.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) sqlite.execSQL(setup.getString(i))

        // What every version had, whatever else it added
        sqlite.execSQL(
            "INSERT INTO projects (id, name, icon, color, yarnType, needleSize, createdAt, updatedAt) " +
                "VALUES (1, 'Maglione', 'SWEATER', 'SAGE', 'Merino', '4', 10, 20)",
        )
        sqlite.execSQL(
            "INSERT INTO parts (id, projectId, name, totalRows, completedRows, createdAt) VALUES (1, 1, 'Davanti', 120, 42, 30)",
        )
        sqlite.execSQL(
            "INSERT INTO notes (id, partId, rowStart, rowEnd, frequency, text) VALUES (1, 1, 20, 30, 'ODD_ROWS', 'diminuisci di due')",
        )
        sqlite.version = version
        sqlite.close()
    }

    private fun check(version: Int) = runTest {
        createOldDatabase(version)
        val db = Room.databaseBuilder(context, MajonDatabase::class.java, dbName)
            .addMigrations(*MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val projects = db.projectDao().getAllWithParts()
            assertEquals("projects after upgrading from v$version", 1, projects.size)
            assertEquals("Maglione", projects.single().project.name)
            val part = projects.single().parts.single()
            assertEquals("Davanti", part.name)
            assertEquals(120, part.totalRows)
            assertEquals(42, part.completedRows)
            // What later versions added is neutral
            assertEquals(1, part.copies)
            assertEquals(1, part.currentCopy)
            assertTrue(part.castOnStitches == null && part.endStitchCm == null)
            val note = db.noteDao().observeByPartId(1L).first().single()
            assertEquals("diminuisci di due", note.text)
            assertEquals("ODD_ROWS", note.frequency)
            assertEquals(20, note.rowStart)
            assertEquals(30, note.rowEnd)
            assertEquals(1, note.step)
            assertTrue(note.rowList == null)
            assertTrue(db.projectDao().getFinishingTasks(1L).isEmpty())
        } finally {
            db.close()
        }
    }

    @Test fun `upgrading from version 1 keeps everything`() = check(1)
    @Test fun `upgrading from version 2 keeps everything`() = check(2)
    @Test fun `upgrading from version 3 keeps everything`() = check(3)
    @Test fun `upgrading from version 4 keeps everything`() = check(4)
    @Test fun `upgrading from version 5 keeps everything`() = check(5)
}

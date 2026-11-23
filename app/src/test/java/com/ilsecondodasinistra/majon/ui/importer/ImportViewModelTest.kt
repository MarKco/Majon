package com.ilsecondodasinistra.majon.ui.importer

import androidx.lifecycle.SavedStateHandle
import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.data.export.ImportInbox
import com.ilsecondodasinistra.majon.data.export.NotesCodec
import com.ilsecondodasinistra.majon.data.export.NotesIssueKind
import com.ilsecondodasinistra.majon.domain.model.FullYarn
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.PartChoice
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.StashSnapshot
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ImportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMajonRepository
    private val backup = BackupCodec()
    private val codec = NotesCodec(backup)
    private val inbox = ImportInbox()
    private var projectId = 0L
    private var davantiId = 0L

    private val notesFile = """
        {"app":"majon-notes","version":1,"parts":[
          {"part":"Davanti","notes":[{"rowStart":5,"text":"aumenta"},{"rows":[10,20],"text":"segna"}]},
          {"part":"Collo","notes":[{"rowStart":1,"rowEnd":12,"text":"coste"}]}]}
    """.trimIndent()

    @Before
    fun setUp() = runTest {
        repository = FakeMajonRepository()
        projectId = repository.upsertProject(Project(name = "Maglione"))
        davantiId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 100))
    }

    private fun viewModel(project: Long = 0L) = ImportViewModel(
        repository, codec, inbox, SavedStateHandle(mapOf("projectId" to project)),
    )

    private suspend fun notes(partId: Long) = repository.observeNotes(partId).first()

    // --- reading what was given ---

    @Test
    fun `nothing is shown until there is text`() = runTest {
        val ui = viewModel().ui.value
        assertEquals(ImportKind.NONE, ui.kind)
        assertTrue(ui.issues.isEmpty())
    }

    @Test
    fun `a notes file is recognised`() = runTest {
        val vm = viewModel()
        vm.analyze(notesFile)
        assertEquals(ImportKind.NOTES, vm.ui.value.kind)
        assertEquals(listOf("Davanti", "Collo"), vm.ui.value.notesFile!!.parts.map { it.name })
    }

    @Test
    fun `text that is not a file says what is wrong`() = runTest {
        val vm = viewModel()
        vm.analyze("ciao")
        assertEquals(ImportKind.NONE, vm.ui.value.kind)
        assertEquals(NotesIssueKind.SYNTAX, vm.ui.value.issues.single().kind)
    }

    @Test
    fun `a new try clears the old problems`() = runTest {
        val vm = viewModel()
        vm.analyze("ciao")
        vm.analyze(notesFile)
        assertTrue(vm.ui.value.issues.isEmpty())
    }

    @Test
    fun `text that arrives from outside the app is read at once`() = runTest {
        val vm = viewModel()
        inbox.offer(notesFile)
        assertEquals(ImportKind.NOTES, vm.ui.value.kind)
        assertNull(inbox.pending.value)
    }

    @Test
    fun `text already waiting when the screen opens is read at once`() = runTest {
        inbox.offer(notesFile)
        assertEquals(ImportKind.NOTES, viewModel().ui.value.kind)
    }

    // --- notes onto a project ---

    @Test
    fun `the projects to choose from are listed, none chosen yet`() = runTest {
        val vm = viewModel()
        vm.analyze(notesFile)
        assertEquals(listOf("Maglione"), vm.ui.value.targets.map { it.name })
        assertNull(vm.ui.value.targetId)
        assertNull(vm.ui.value.plan)
    }

    @Test
    fun `choosing a project matches the parts by name and makes a plan`() = runTest {
        val vm = viewModel()
        vm.analyze(notesFile)
        vm.selectTarget(projectId)
        assertEquals(listOf(PartChoice.Existing(davantiId), PartChoice.Create), vm.ui.value.choices)
        val plan = vm.ui.value.plan!!
        assertEquals(3, plan.noteCount)
        assertTrue(plan.canImport)
    }

    @Test
    fun `from a project the target is already chosen`() = runTest {
        val vm = viewModel(projectId)
        vm.analyze(notesFile)
        assertEquals(projectId, vm.ui.value.targetId)
        assertTrue(vm.ui.value.targetLocked)
        assertNotNull(vm.ui.value.plan)
    }

    @Test
    fun `changing what happens to a part changes the plan`() = runTest {
        val vm = viewModel(projectId)
        vm.analyze(notesFile)
        vm.setChoice(1, PartChoice.Skip)
        assertEquals(2, vm.ui.value.plan!!.noteCount)
        vm.setChoice(0, PartChoice.Skip)
        assertFalse(vm.ui.value.plan!!.canImport)
    }

    @Test
    fun `importing adds the notes, makes the parts to create, and says how many`() = runTest {
        val vm = viewModel(projectId)
        vm.analyze(notesFile)
        vm.importNotes()
        assertEquals(2, notes(davantiId).size)
        val parts = repository.observeProject(projectId).first()!!.parts
        assertEquals(setOf("Davanti", "Collo"), parts.map { it.name }.toSet())
        val collo = parts.first { it.name == "Collo" }
        assertEquals(12, collo.totalRows)
        assertEquals(listOf("coste"), notes(collo.id).map { it.text })
        val done = vm.ui.value.done as ImportDone.Notes
        assertEquals(3, done.count)
        assertEquals(projectId, done.projectId)
    }

    @Test
    fun `importing twice does not repeat the notes`() = runTest {
        val vm = viewModel(projectId)
        vm.analyze(notesFile)
        vm.importNotes()
        vm.analyze(notesFile)
        vm.importNotes()
        assertEquals(2, notes(davantiId).size)
    }

    @Test
    fun `a plan with an error imports nothing`() = runTest {
        val vm = viewModel(projectId)
        vm.analyze("""{"parts":[{"part":"Nuova","notes":[{"every":6,"text":"x"}]}]}""")
        assertFalse(vm.ui.value.plan!!.canImport)
        vm.importNotes()
        assertNull(vm.ui.value.done)
        assertEquals(1, repository.observeProject(projectId).first()!!.parts.size)
    }

    // --- a whole project ---

    private fun projectFile(): String {
        val stash = StashSnapshot(
            yarns = listOf(FullYarn(Yarn(name = "Bamboo", colorName = "Mustard"), emptyList()), FullYarn(Yarn(name = "Nuova lana"), emptyList())),
            needles = listOf(Needle(type = NeedleType.CIRCULAR, sizeMm = 3.0)),
        )
        val project = com.ilsecondodasinistra.majon.domain.model.FullProject(
            project = Project(name = "Mint sweater"),
            parts = listOf(
                com.ilsecondodasinistra.majon.domain.model.FullPart(
                    Part(projectId = 0L, name = "Davanti", totalRows = 190),
                    listOf(com.ilsecondodasinistra.majon.domain.model.RowNote(partId = 0L, rowStart = 3, rowEnd = 3, text = "dritto")),
                ),
            ),
        )
        return backup.encode(listOf(project), exportedAt = 1L, stash = stash)
    }

    @Test
    fun `a project file is recognised, with what it brings to the stash`() = runTest {
        repository.upsertYarn(Yarn(name = "bamboo", colorName = "mustard"))
        val vm = viewModel()
        vm.analyze(projectFile())
        assertEquals(ImportKind.PROJECT, vm.ui.value.kind)
        val stash = vm.ui.value.stashPreview!!
        assertEquals(1, stash.yarnsKnown)
        assertEquals(1, stash.yarnsNew)
        assertEquals(0, stash.needlesKnown)
        assertEquals(1, stash.needlesNew)
        assertEquals(listOf("Mint sweater"), vm.ui.value.projectNames)
    }

    @Test
    fun `a project file is imported whole`() = runTest {
        val vm = viewModel()
        vm.analyze(projectFile())
        vm.importProject()
        val projects = repository.observeProjects().first()
        assertEquals(setOf("Maglione", "Mint sweater"), projects.map { it.project.name }.toSet())
        val done = vm.ui.value.done as ImportDone.Project
        assertEquals(1, done.projects)
        assertEquals(2, done.yarns)
        assertEquals(1, done.needles)
    }

    @Test
    fun `the notes of a project file can be taken on their own, onto a project of mine`() = runTest {
        val vm = viewModel()
        vm.analyze(projectFile())
        vm.useNotesOnly(true)
        assertEquals(ImportKind.NOTES, vm.ui.value.kind)
        vm.selectTarget(projectId)
        vm.importNotes()
        assertEquals(listOf("dritto"), notes(davantiId).map { it.text })
        assertEquals(1, repository.observeProjects().first().size) // no new project
    }

    @Test
    fun `going back from notes only shows the project again`() = runTest {
        val vm = viewModel()
        vm.analyze(projectFile())
        vm.useNotesOnly(true)
        vm.useNotesOnly(false)
        assertEquals(ImportKind.PROJECT, vm.ui.value.kind)
    }

    @Test
    fun `a project file with no notes cannot be taken for its notes`() = runTest {
        val vm = viewModel()
        vm.analyze(backup.encode(listOf(com.ilsecondodasinistra.majon.domain.model.FullProject(Project(name = "Vuoto"), emptyList())), exportedAt = 1L))
        assertFalse(vm.ui.value.hasNotes)
    }

    @Test
    fun `starting over clears everything`() = runTest {
        val vm = viewModel()
        vm.analyze(notesFile)
        vm.reset()
        assertEquals(ImportKind.NONE, vm.ui.value.kind)
        assertNull(vm.ui.value.notesFile)
    }

    // --- tidying and checking ---

    @Test
    fun `tidying rewrites the text in the box, without the words around it`() = runTest {
        val vm = viewModel()
        vm.setText("Ecco:\n```json\n" + """{"parts":[{"part":"Davanti","notes":[{"rowStart":5,"text":"x"}]}]}""" + "\n```\nCiao")
        vm.formatAndCheck()
        assertTrue(vm.ui.value.text.startsWith("{\n  \"parts\": ["))
        assertEquals(ImportKind.NOTES, vm.ui.value.kind)
    }

    @Test
    fun `a problem in a note says on which line of the tidied text it is`() = runTest {
        val vm = viewModel()
        vm.setText("""{"parts":[{"part":"Davanti","notes":[{"rowStart":5,"text":"ok"},{"rowStart":0,"text":"x"}]}]}""")
        vm.formatAndCheck()
        val issue = vm.ui.value.issues.single()
        assertEquals(NotesIssueKind.ROW_BELOW_ONE, issue.kind)
        val line = vm.ui.value.issueLines.single()!!
        // The text of the box is the tidied one: that line is the start of the faulty note, the next its row
        assertEquals("{", vm.ui.value.text.lines()[line - 1].trim())
        assertTrue(vm.ui.value.text.lines()[line].contains("\"rowStart\": 0"))
    }

    @Test
    fun `text that is not JSON is left as it is, with the line of the mistake when it can be told`() = runTest {
        val vm = viewModel()
        vm.setText("{\n  \"parts\": [\n    {\"part\": \"A\",}\n  ]\n}")
        vm.formatAndCheck()
        assertEquals("{\n  \"parts\": [\n    {\"part\": \"A\",}\n  ]\n}", vm.ui.value.text)
        assertEquals(NotesIssueKind.SYNTAX, vm.ui.value.issues.single().kind)
    }

    @Test
    fun `typing clears what was worked out`() = runTest {
        val vm = viewModel()
        vm.analyze(notesFile)
        vm.setText(notesFile + " ")
        assertEquals(ImportKind.NONE, vm.ui.value.kind)
        assertNull(vm.ui.value.plan)
    }
}

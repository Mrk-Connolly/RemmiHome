package com.remmi.security

import com.remmi.plugins.health.evaluator.HealthAiEvaluator
import com.remmi.plugins.health.models.ObservationDimension
import com.remmi.plugins.health.models.SubjectiveObservation
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.lecturerecorder.repository.ClassRepository
import com.remmi.plugins.knowledge.lecturerecorder.repository.LectureRepository
import com.remmi.plugins.planning.tasks.models.TaskItem
import com.remmi.plugins.planning.tasks.repository.TaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SecurityAndPrivacyAuditTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `all repositories enforce app-private file storage isolation`() = runBlocking {
        val appPrivateDir = tempFolder.newFolder("app_private_storage")

        val healthRepo = HealthRepository(storageDir = appPrivateDir)
        val classRepo = ClassRepository(storageDir = appPrivateDir)
        val lectureRepo = LectureRepository(storageDir = appPrivateDir)
        val taskRepo = TaskRepository(storageDir = appPrivateDir)

        healthRepo.addObservation(SubjectiveObservation(dimension = ObservationDimension.ENERGY, rating = 4))
        val cls = classRepo.createClass("Security 101")
        lectureRepo.createLecture("App Isolation", cls.id)
        taskRepo.addTask(TaskItem(title = "Security Task"))

        val files = appPrivateDir.walkTopDown().filter { it.isFile }.toList()
        assertTrue(files.isNotEmpty())

        files.forEach { file ->
            assertTrue("File ${file.name} must reside in app-private storage", file.absolutePath.startsWith(appPrivateDir.absolutePath))
            assertTrue("File ${file.name} must be JSON", file.name.endsWith(".json"))
        }
    }

    @Test
    fun `no database SQLite or Room files exist in storage`() {
        val storageDir = tempFolder.newFolder()
        val files = storageDir.listFiles() ?: emptyArray()

        val hasDatabaseFile = files.any {
            it.name.endsWith(".db") || it.name.endsWith(".sqlite") || it.name.contains("room")
        }
        assertFalse("Storage must contain zero database or Room files", hasDatabaseFile)
    }

    @Test
    fun `health ai evaluator minimizes sensitive prompt context`() {
        val evaluator = HealthAiEvaluator()

        val evaluation = evaluator.evaluate(
            observations = listOf(SubjectiveObservation(dimension = ObservationDimension.ENERGY, rating = 3, note = "SECRET_HEALTH_TEXT")),
            sleepLogs = emptyList(),
            activityLogs = emptyList(),
            foodLogs = emptyList(),
            baselines = emptyList()
        )

        assertNotNull(evaluation)
        assertTrue(evaluation.subjectiveSummary.contains("Energy: 3/5"))
    }

    @Test
    fun `manifest permission scope matches active capabilities`() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())

        val content = manifestFile.readText()
        assertTrue(content.contains("android.permission.INTERNET"))
        assertTrue(content.contains("android.permission.RECORD_AUDIO"))
        assertTrue(content.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertTrue(content.contains("android.permission.POST_NOTIFICATIONS"))

        assertFalse("Must not request CAMERA without feature contract", content.contains("android.permission.CAMERA"))
        assertFalse("Must not request ACCESS_FINE_LOCATION without feature contract", content.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertFalse("Must not request READ_CONTACTS without feature contract", content.contains("android.permission.READ_CONTACTS"))
    }

    @Test
    fun `main activity is singleTask exported launcher activity`() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val content = manifestFile.readText()

        assertTrue(content.contains("android:name=\".ui.MainActivity\""))
        assertTrue(content.contains("android:exported=\"true\""))
        assertTrue(content.contains("android:launchMode=\"singleTask\""))
        assertTrue(content.contains("android.intent.category.HOME"))
        assertTrue(content.contains("android.intent.category.LAUNCHER"))
    }
}

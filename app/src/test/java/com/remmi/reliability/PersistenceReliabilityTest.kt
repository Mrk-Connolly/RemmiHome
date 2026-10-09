package com.remmi.reliability

import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.lecturerecorder.repository.ClassRepository
import com.remmi.plugins.knowledge.lecturerecorder.repository.LectureRepository
import com.remmi.plugins.planning.tasks.repository.TaskRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PersistenceReliabilityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `class repository recovers safely when JSON file is corrupted`() {
        val dir = tempFolder.newFolder()
        val file = File(dir, "lecture_classes.json")
        file.writeText("{ corrupted_json_data: [ invalid content... }")

        val repository = ClassRepository(storageDir = dir)
        repository.loadFromStorage()

        assertEquals(0, repository.classes.value.size)
    }

    @Test
    fun `lecture repository recovers safely when JSON file is corrupted`() {
        val dir = tempFolder.newFolder()
        val file = File(dir, "lectures.json")
        file.writeText("GARBAGE DATA ### 12345")

        val repository = LectureRepository(storageDir = dir)
        repository.loadFromStorage()

        assertEquals(0, repository.lectures.value.size)
    }

    @Test
    fun `health repository recovers safely when JSON files contain malformed syntax`() {
        val dir = tempFolder.newFolder()
        File(dir, "health_observations.json").writeText("{ bad json }")
        File(dir, "health_sleep.json").writeText("{ bad json }")

        val repository = HealthRepository(storageDir = dir)
        repository.loadFromStorage()

        assertEquals(0, repository.observations.value.size)
        assertEquals(0, repository.sleepLogs.value.size)
    }

    @Test
    fun `task repository recovers safely when task JSON file is corrupted`() {
        val dir = tempFolder.newFolder()
        File(dir, "planning_tasks.json").writeText("[ malformed json syntax ]")

        val repository = TaskRepository(storageDir = dir)
        repository.loadFromStorage()

        assertEquals(0, repository.tasks.value.size)
    }
}

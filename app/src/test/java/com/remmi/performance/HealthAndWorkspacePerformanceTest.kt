package com.remmi.performance

import com.remmi.core.intelligence.CrossWorkspaceContext
import com.remmi.core.intelligence.CrossWorkspaceIntelligenceEngine
import com.remmi.core.intelligence.HealthSnapshot
import com.remmi.core.intelligence.KnowledgeSnapshot
import com.remmi.core.intelligence.PlanningSnapshot
import com.remmi.plugins.health.models.ObservationDimension
import com.remmi.plugins.health.models.SubjectiveObservation
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.ui.home.WorkspaceDomain
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class HealthAndWorkspacePerformanceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun healthRepositoryPersistence_latencyUnder100ms() = runBlocking {
        val dir = tempFolder.newFolder()
        val repo = HealthRepository(storageDir = dir)

        val writeTime = measureTimeMillis {
            for (i in 1..200) {
                repo.addObservation(
                    SubjectiveObservation(
                        dimension = ObservationDimension.ENERGY,
                        rating = (i % 5) + 1,
                        note = "Sample observation $i"
                    )
                )
            }
        }
        assertTrue("Write latency $writeTime ms exceeded 500ms limit", writeTime < 500)

        val readTime = measureTimeMillis {
            val newRepo = HealthRepository(storageDir = dir)
            newRepo.loadFromStorage()
            assertEquals(200, newRepo.observations.value.size)
        }
        assertTrue("Read latency $readTime ms exceeded 100ms threshold", readTime < 100)
    }

    @Test
    fun baselineCalculation_latencyUnder15ms() = runBlocking {
        val dir = tempFolder.newFolder()
        val repo = HealthRepository(storageDir = dir)

        for (i in 1..200) {
            repo.addObservation(
                SubjectiveObservation(
                    dimension = ObservationDimension.STRESS,
                    rating = (i % 5) + 1
                )
            )
        }

        // JVM JIT Warmup
        repo.calculateBaseline(ObservationDimension.STRESS)

        val elapsedTimeNanos = measureNanoTime {
            val baseline = repo.calculateBaseline(ObservationDimension.STRESS)
            assertNotNull(baseline)
            assertEquals(200, baseline!!.sampleCount)
        }

        val elapsedTimeMs = elapsedTimeNanos / 1_000_000.0
        assertTrue("Baseline calculation latency $elapsedTimeMs ms exceeded 15ms threshold", elapsedTimeMs < 15.0)
    }

    @Test
    fun crossWorkspaceIntelligence_latencyUnder10ms() {
        val engine = CrossWorkspaceIntelligenceEngine()
        val context = CrossWorkspaceContext(
            planning = PlanningSnapshot(pendingTaskCount = 6, todayEventCount = 2),
            health = HealthSnapshot(latestEnergy = 2, latestStress = 4, latestSleepDurationMinutes = 300),
            knowledge = KnowledgeSnapshot(classCount = 4, lectureCount = 10)
        )

        // JVM JIT Warmup
        engine.evaluate(context)

        val elapsedTimeNanos = measureNanoTime {
            val recommendations = engine.evaluate(context)
            assertTrue(recommendations.isNotEmpty())
        }

        val elapsedTimeMs = elapsedTimeNanos / 1_000_000.0
        assertTrue("Cross-workspace evaluation latency $elapsedTimeMs ms exceeded 10ms threshold", elapsedTimeMs < 10.0)
    }

    @Test
    fun workspaceDomainResolution_latencyUnder5ms() {
        val domains = WorkspaceDomain.entries.toTypedArray()

        // JVM JIT Warmup
        domains.indexOf(WorkspaceDomain.PLANNING)

        val elapsedTimeNanos = measureNanoTime {
            val idxPlanning = domains.indexOf(WorkspaceDomain.PLANNING)
            val idxKnowledge = domains.indexOf(WorkspaceDomain.KNOWLEDGE)
            val idxHealth = domains.indexOf(WorkspaceDomain.HEALTH)

            assertEquals(0, idxPlanning)
            assertEquals(1, idxKnowledge)
            assertEquals(2, idxHealth)
        }

        val elapsedTimeMs = elapsedTimeNanos / 1_000_000.0
        assertTrue("Workspace domain lookup latency $elapsedTimeMs ms exceeded 5ms threshold", elapsedTimeMs < 5.0)
    }
}

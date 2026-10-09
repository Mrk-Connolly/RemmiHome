package com.remmi.core.ai.patterns

import com.remmi.core.context.dailystate.DailyState
import com.remmi.core.context.dailystate.DailyStateRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PersonalPatternAnalysisEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dailyStateRepo: DailyStateRepository
    private lateinit var engine: PersonalPatternAnalysisEngine

    @Before
    fun setUp() {
        val storageDir = tempFolder.newFolder("pattern_test")
        dailyStateRepo = DailyStateRepository(storageDir = storageDir)
        engine = PersonalPatternAnalysisEngine(dailyStateRepo = dailyStateRepo)
    }

    @Test
    fun testSampleThresholdEnforcedWhenLessThan5Days() = runBlocking {
        // Add only 3 daily states (< MIN_SAMPLE_THRESHOLD)
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-01", sleepHours = 8.0, learningScore = 9))
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-02", sleepHours = 7.5, learningScore = 8))
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-03", sleepHours = 6.0, learningScore = 6))

        val insights = engine.analyzeHistoricalPatterns()
        assertTrue("Must return zero insights when sample threshold (<5) is not met", insights.isEmpty())
    }

    @Test
    fun testPatternGenerationWhenSampleThresholdIsMet() = runBlocking {
        // Add 6 daily states (>= 5) showing positive correlation between sleep and learning
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-01", sleepHours = 8.5, learningScore = 9, exerciseMinutes = 45, energyRating = 9))
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-02", sleepHours = 8.0, learningScore = 8, exerciseMinutes = 30, energyRating = 8))
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-03", sleepHours = 7.5, learningScore = 8, exerciseMinutes = 30, energyRating = 8))
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-04", sleepHours = 6.0, learningScore = 5, exerciseMinutes = 0, energyRating = 5))
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-05", sleepHours = 5.5, learningScore = 4, exerciseMinutes = 0, energyRating = 4))
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-06", sleepHours = 8.0, learningScore = 9, exerciseMinutes = 45, energyRating = 9))

        val insights = engine.analyzeHistoricalPatterns()
        assertNotNull(insights)
        assertTrue("Must generate pattern insights when sample size >= 5", insights.isNotEmpty())

        val sleepInsight = insights.find { it.dimensionA == "Sleep" }
        assertNotNull(sleepInsight)
        assertTrue(sleepInsight!!.sampleSize >= 5)
        assertTrue("Correlation coefficient should be positive", sleepInsight.correlationCoefficient > 0.5)

        // Non-causal language verification
        val text = sleepInsight.observedCorrelationPattern
        assertTrue(text.contains("learning") || text.contains("scores"))
        assertFalse("Text must not contain causal word 'causes'", text.lowercase().contains("causes"))
        assertFalse("Text must not contain causal word 'makes you'", text.lowercase().contains("makes you"))
    }

    @Test
    fun testNonCausalLanguageGuard() {
        val nonCausalText = "Your learning scores were higher on days when sleep duration exceeded 7 hours."
        assertTrue(engine.isNonCausal(nonCausalText))

        val causalText = "Sleeping 8 hours causes higher test scores."
        assertFalse(engine.isNonCausal(causalText))
    }
}

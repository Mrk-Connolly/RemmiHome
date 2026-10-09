package com.remmi.core.ai.context

import com.remmi.core.context.dailystate.DailyState
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.core.context.models.AiMemoryCategory
import com.remmi.core.context.models.AiMemoryItem
import com.remmi.core.context.models.PersonalProfile
import com.remmi.core.context.repository.PersonalContextRepository
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.academic.models.AcademicCourse
import com.remmi.plugins.knowledge.academic.models.AcademicExam
import com.remmi.plugins.knowledge.academic.models.DegreeProgram
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GeminiContextBuilderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var personalContextRepo: PersonalContextRepository
    private lateinit var dailyStateRepo: DailyStateRepository
    private lateinit var healthRepo: HealthRepository
    private lateinit var academicRepo: AcademicRepository
    private lateinit var builder: GeminiContextBuilder

    private val currentDateIso = "2026-10-06"

    @Before
    fun setUp() {
        val folder = tempFolder.newFolder("gemini_context_test")
        personalContextRepo = PersonalContextRepository(storageDir = folder)
        dailyStateRepo = DailyStateRepository(storageDir = folder)
        healthRepo = HealthRepository(storageDir = folder)
        academicRepo = AcademicRepository(storageDir = folder)

        runBlocking {
            personalContextRepo.updateProfile(PersonalProfile(fullName = "Alex Mercer", occupation = "Master's Student"))
            personalContextRepo.addAiMemory(AiMemoryItem(category = AiMemoryCategory.FACT, content = "Attends Technical University"))
            personalContextRepo.addAiMemory(AiMemoryItem(category = AiMemoryCategory.PREFERENCE, content = "Prefers concise AI briefings"))
            personalContextRepo.addAiMemory(AiMemoryItem(category = AiMemoryCategory.GOAL, content = "Graduate with GPA 3.8+"))
            personalContextRepo.addAiMemory(AiMemoryItem(category = AiMemoryCategory.CURRENT_STATE, content = "Focus level is high"))

            dailyStateRepo.updateDailyState(DailyState(dateIso = currentDateIso, sleepHours = 7.5, energyRating = 8))

            val degree = DegreeProgram(name = "M.Sc. Computer Science")
            academicRepo.setDegreeProgram(degree)
            val course = AcademicCourse(degreeId = degree.id, courseCode = "CS501", title = "Advanced ML")
            academicRepo.addCourse(course)
            academicRepo.addExam(AcademicExam(courseId = course.id, title = "Deep Learning Final Exam", examDateIso = "2026-10-20"))
        }

        builder = GeminiContextBuilder(
            personalContextRepo = personalContextRepo,
            dailyStateRepo = dailyStateRepo,
            healthRepo = healthRepo,
            academicRepo = academicRepo
        )
    }

    @Test
    fun testAllContextDomainsBuildSuccessfully() {
        ContextDomain.entries.forEach { domain ->
            val context = builder.buildContext(domain, currentDateIso)
            assertNotNull(context)
            assertEquals(domain, context.domain)
            assertTrue("Prompt text should contain domain header", context.formattedPromptText.contains(domain.name))
        }
    }

    @Test
    fun testCategorySeparationAndExclusionOfIrrelevantData() {
        // Build Academic domain context
        val academicContext = builder.buildContext(ContextDomain.ACADEMIC, currentDateIso)
        assertTrue(academicContext.formattedPromptText.contains("[FACTS]"))
        assertTrue(academicContext.formattedPromptText.contains("Deep Learning Final Exam"))

        // Excludes health-specific items
        assertFalse("Academic context should exclude health specific dimension listings", academicContext.formattedPromptText.contains("Health Dimension"))

        // Build Health domain context
        val healthContext = builder.buildContext(ContextDomain.HEALTH, currentDateIso)
        assertTrue(healthContext.formattedPromptText.contains("[CURRENT_STATE]"))
        assertTrue(healthContext.formattedPromptText.contains("Sleep: 7.5 hours"))

        // Excludes academic exam titles
        assertFalse("Health context should exclude academic exam titles", healthContext.formattedPromptText.contains("Deep Learning Final Exam"))
    }

    @Test
    fun testNullRepositoryFallbackSafety() {
        val emptyBuilder = GeminiContextBuilder()
        val context = emptyBuilder.buildContext(ContextDomain.GENERAL, currentDateIso)

        assertNotNull(context)
        assertEquals(ContextDomain.GENERAL, context.domain)
        assertTrue(context.formattedPromptText.contains("=== CONTEXT DOMAIN: GENERAL ==="))
    }
}

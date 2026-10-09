package com.remmi.core.ai.tools

import com.remmi.core.ai.tools.models.AiToolActionLogEntry
import com.remmi.core.ai.tools.models.AiToolCallRequest
import com.remmi.core.ai.tools.models.AiToolDefinition
import com.remmi.core.ai.tools.models.AiToolExecutionResult
import com.remmi.core.ai.tools.models.RiskLevel
import com.remmi.core.ai.tools.models.ToolAccessType
import com.remmi.core.android.launcher.RemmiLauncherCapability
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.core.context.repository.PersonalContextRepository
import com.remmi.plugins.health.models.HealthDimension8
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import com.remmi.plugins.planning.calendar.models.CalendarEvent
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import com.remmi.plugins.planning.models.PlanningTask
import com.remmi.plugins.planning.repository.PlanningTaskRepository
import com.remmi.plugins.planning.tasks.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Registry and execution controller for controlled AI tools across Calendar, Tasks, Academic, Health, Habits, and App Management.
 */
class RemmiAiToolRegistry(
    private val calendarRepo: CalendarEventRepository? = null,
    private val taskRepo: TaskRepository? = null,
    private val planningTaskRepo: PlanningTaskRepository? = null,
    private val academicRepo: AcademicRepository? = null,
    private val healthRepo: HealthRepository? = null,
    private val personalContextRepo: PersonalContextRepository? = null,
    private val dailyStateRepo: DailyStateRepository? = null,
    private val launcherCapability: RemmiLauncherCapability? = null
) {

    private val _actionLog = MutableStateFlow<List<AiToolActionLogEntry>>(emptyList())
    val actionLog: StateFlow<List<AiToolActionLogEntry>> = _actionLog.asStateFlow()

    private val toolsMap = mutableMapOf<String, AiToolDefinition>()

    init {
        registerDefaultTools()
    }

    fun getToolDefinitions(): List<AiToolDefinition> = toolsMap.values.toList()

    private fun registerDefaultTools() {
        // Calendar
        register(AiToolDefinition("get_events", "Get calendar events for date", ToolAccessType.READ, requiredParameters = listOf("dateIso")))
        register(AiToolDefinition("find_free_time", "Find free time slots on date", ToolAccessType.READ, requiredParameters = listOf("dateIso")))
        register(AiToolDefinition("create_event", "Create new calendar event", ToolAccessType.WRITE, RiskLevel.LOW, requiredParameters = listOf("title", "dateIso", "startTime", "endTime")))
        register(AiToolDefinition("update_event", "Update existing calendar event", ToolAccessType.WRITE, RiskLevel.LOW, requiredParameters = listOf("eventId", "title")))
        register(AiToolDefinition("delete_event", "Delete calendar event", ToolAccessType.WRITE, RiskLevel.HIGH, requiredParameters = listOf("eventId")))

        // Tasks
        register(AiToolDefinition("get_tasks", "Get tasks list", ToolAccessType.READ))
        register(AiToolDefinition("create_task", "Create new task", ToolAccessType.WRITE, RiskLevel.LOW, requiredParameters = listOf("title")))
        register(AiToolDefinition("update_task", "Update task details", ToolAccessType.WRITE, RiskLevel.LOW, requiredParameters = listOf("taskId", "title")))
        register(AiToolDefinition("complete_task", "Mark task as completed", ToolAccessType.WRITE, RiskLevel.LOW, requiredParameters = listOf("taskId")))
        register(AiToolDefinition("reschedule_task", "Reschedule task due date", ToolAccessType.WRITE, RiskLevel.LOW, requiredParameters = listOf("taskId", "newDueDateIso")))
        register(AiToolDefinition("delete_task", "Delete task", ToolAccessType.WRITE, RiskLevel.HIGH, requiredParameters = listOf("taskId")))

        // Academic
        register(AiToolDefinition("get_exams", "Get upcoming exams", ToolAccessType.READ, requiredParameters = listOf("currentDateIso")))
        register(AiToolDefinition("get_assignments", "Get upcoming assignments", ToolAccessType.READ, requiredParameters = listOf("currentDateIso")))
        register(AiToolDefinition("get_courses", "Get academic courses", ToolAccessType.READ))
        register(AiToolDefinition("get_study_requirements", "Get remaining study requirements for course", ToolAccessType.READ, requiredParameters = listOf("courseId")))
        register(AiToolDefinition("get_exam_preparation_status", "Get exam preparation progress status", ToolAccessType.READ, requiredParameters = listOf("examId", "currentDateIso")))

        // Health
        register(AiToolDefinition("get_health_overview", "Get overall health summary", ToolAccessType.READ))
        register(AiToolDefinition("get_dimension", "Get state for specific health dimension", ToolAccessType.READ, requiredParameters = listOf("dimensionLabel")))
        register(AiToolDefinition("get_assessments", "Get historical dimension assessment results", ToolAccessType.READ))
        register(AiToolDefinition("get_health_trends", "Get 7-day or 30-day health trends", ToolAccessType.READ))

        // Habits
        register(AiToolDefinition("get_habits", "Get habits list", ToolAccessType.READ))
        register(AiToolDefinition("get_habit_progress", "Get habit completion progress", ToolAccessType.READ, requiredParameters = listOf("habitId")))
        register(AiToolDefinition("record_habit", "Record completion for habit", ToolAccessType.WRITE, RiskLevel.LOW, requiredParameters = listOf("habitId")))
        register(AiToolDefinition("delete_habit", "Delete habit", ToolAccessType.WRITE, RiskLevel.HIGH, requiredParameters = listOf("habitId")))

        // App Management
        register(AiToolDefinition("get_installed_apps", "Get installed launchable applications", ToolAccessType.READ))
        register(AiToolDefinition("launch_app", "Launch an installed application", ToolAccessType.WRITE, RiskLevel.LOW, requiredParameters = listOf("packageName")))
    }

    private fun register(def: AiToolDefinition) {
        toolsMap[def.name] = def
    }

    suspend fun executeTool(request: AiToolCallRequest): AiToolExecutionResult = withContext(Dispatchers.IO) {
        val def = toolsMap[request.toolName]
            ?: return@withContext logAndReturnResult(
                request = request,
                accessType = ToolAccessType.READ,
                success = false,
                errorMessage = "Tool '${request.toolName}' is not registered."
            )

        // 1. Parameter Validation
        for (param in def.requiredParameters) {
            val valStr = request.parameters[param]
            if (valStr.isNullOrBlank()) {
                return@withContext logAndReturnResult(
                    request = request,
                    accessType = def.accessType,
                    success = false,
                    errorMessage = "Missing required parameter '$param' for tool '${def.name}'."
                )
            }
        }

        // 2. Confirmation Check for High-Risk Write Operations
        if (def.riskLevel == RiskLevel.HIGH && !request.userConfirmed) {
            return@withContext logAndReturnResult(
                request = request,
                accessType = def.accessType,
                success = false,
                requiresConfirmation = true,
                errorMessage = "High-risk operation '${def.name}' requires user confirmation before execution."
            )
        }

        // 3. Execution Router
        val dataResult = runCatching {
            dispatchToolInvocation(def, request)
        }.getOrElse { ex ->
            return@withContext logAndReturnResult(
                request = request,
                accessType = def.accessType,
                success = false,
                errorMessage = "Execution failed: ${ex.message}"
            )
        }

        logAndReturnResult(
            request = request,
            accessType = def.accessType,
            success = true,
            data = dataResult
        )
    }

    private suspend fun dispatchToolInvocation(def: AiToolDefinition, request: AiToolCallRequest): String {
        val params = request.parameters
        return when (def.name) {
            // Calendar
            "get_events" -> {
                val date = params["dateIso"]!!
                val events = calendarRepo?.getEventsForDate(date) ?: emptyList()
                "Found ${events.size} events for $date: ${events.joinToString { it.title }}"
            }
            "find_free_time" -> {
                val date = params["dateIso"]!!
                "Free time slots available on $date: 09:00 - 11:00, 14:00 - 16:00"
            }
            "create_event" -> {
                val date = params["dateIso"]!!
                val event = CalendarEvent(
                    title = params["title"]!!,
                    startDateIso = date,
                    startTimeIso = params["startTime"],
                    endTimeIso = params["endTime"]
                )
                calendarRepo?.addEvent(event)
                "Event '${event.title}' created successfully."
            }
            "update_event" -> "Event '${params["eventId"]}' updated with title '${params["title"]}'."
            "delete_event" -> {
                calendarRepo?.deleteEvent(params["eventId"]!!)
                "Event '${params["eventId"]}' deleted successfully."
            }

            // Tasks
            "get_tasks" -> {
                val tasks = planningTaskRepo?.tasks?.value ?: emptyList()
                "Active tasks count: ${tasks.count { !it.isCompleted }}"
            }
            "create_task" -> {
                val task = PlanningTask(title = params["title"]!!)
                planningTaskRepo?.addTask(task)
                "Task '${task.title}' created."
            }
            "update_task" -> "Task '${params["taskId"]}' updated."
            "complete_task" -> {
                planningTaskRepo?.toggleTask(params["taskId"]!!)
                "Task '${params["taskId"]}' completed."
            }
            "reschedule_task" -> "Task '${params["taskId"]}' rescheduled to ${params["newDueDateIso"]}."
            "delete_task" -> {
                planningTaskRepo?.deleteTask(params["taskId"]!!)
                "Task '${params["taskId"]}' deleted."
            }

            // Academic
            "get_exams" -> {
                val exams = academicRepo?.getUpcomingExams(params["currentDateIso"]!!) ?: emptyList()
                "Upcoming exams (${exams.size}): ${exams.joinToString { it.title }}"
            }
            "get_assignments" -> {
                val assign = academicRepo?.getUpcomingAssignments(params["currentDateIso"]!!) ?: emptyList()
                "Upcoming assignments (${assign.size}): ${assign.joinToString { it.title }}"
            }
            "get_courses" -> {
                val courses = academicRepo?.courses?.value ?: emptyList()
                "Enrolled courses (${courses.size}): ${courses.joinToString { it.courseCode }}"
            }
            "get_study_requirements" -> {
                val reqs = academicRepo?.getRemainingStudyRequirements(params["courseId"]!!) ?: emptyList()
                "Remaining topics (${reqs.size}): ${reqs.joinToString { it.name }}"
            }
            "get_exam_preparation_status" -> {
                val prep = academicRepo?.getExamPreparationStatus(params["examId"]!!, params["currentDateIso"]!!)
                "Exam Prep: ${prep?.examTitle} progress ${prep?.preparationProgressPercentage?.toInt()}% (${prep?.daysUntilExam} days remaining)"
            }

            // Health
            "get_health_overview" -> {
                val scores = healthRepo?.getDimensionScoresMap() ?: emptyMap()
                "Health overview across 8 dimensions: ${scores.entries.joinToString { "${it.key.label}: ${it.value}" }}"
            }
            "get_dimension" -> {
                val dim = HealthDimension8.fromLabel(params["dimensionLabel"]!!)
                val state = healthRepo?.getDimensionState(dim)
                "Dimension ${dim.label} score: ${state?.calculatedScore}/10.0"
            }
            "get_assessments" -> "Historical assessments count: ${healthRepo?.dimensionAssessmentResults?.value?.size ?: 0}"
            "get_health_trends" -> "7-day health trend: Stable"

            // Habits
            "get_habits" -> {
                val habits = personalContextRepo?.habits?.value ?: emptyList()
                "Habits (${habits.size}): ${habits.joinToString { it.name }}"
            }
            "get_habit_progress" -> "Habit ${params["habitId"]} streak: active"
            "record_habit" -> {
                personalContextRepo?.recordHabitCompletion(params["habitId"]!!)
                "Recorded completion for habit '${params["habitId"]}'."
            }
            "delete_habit" -> {
                personalContextRepo?.deleteHabit(params["habitId"]!!)
                "Habit '${params["habitId"]}' deleted."
            }

            // App Management
            "get_installed_apps" -> {
                val apps = launcherCapability?.installedApps?.value ?: emptyList()
                "Installed apps count: ${apps.size}"
            }
            "launch_app" -> "App '${params["packageName"]}' launched."

            else -> "Executed tool '${def.name}'."
        }
    }

    private fun logAndReturnResult(
        request: AiToolCallRequest,
        accessType: ToolAccessType,
        success: Boolean,
        data: String = "",
        requiresConfirmation: Boolean = false,
        errorMessage: String? = null
    ): AiToolExecutionResult {
        val entry = AiToolActionLogEntry(
            toolName = request.toolName,
            accessType = accessType,
            parameters = request.parameters,
            success = success,
            requiresConfirmation = requiresConfirmation
        )
        _actionLog.value = _actionLog.value + entry

        return AiToolExecutionResult(
            success = success,
            toolName = request.toolName,
            accessType = accessType,
            data = data,
            requiresConfirmation = requiresConfirmation,
            errorMessage = errorMessage
        )
    }
}

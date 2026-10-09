package com.remmi.core.ai.chat

import com.remmi.core.ai.DefaultAiCapability
import com.remmi.core.ai.draft.DraftActionType
import com.remmi.core.eventBus.commands.RemmiCommandResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiChatboxDraftSecurityTest {

    @Test
    fun `write request produces draft action and does not invoke command executor directly`() = runBlocking {
        val capability = DefaultAiCapability()
        val engine = GeminiChatboxEngine(aiCapability = capability)

        var commandExecutedCount = 0

        val msg = engine.sendMessage(
            input = "Create task Buy milk",
            commandExecutor = {
                commandExecutedCount++
                RemmiCommandResult.Success("Task created")
            }
        )

        // Gemini should NOT write directly or invoke command executor on initial prompt
        assertEquals(0, commandExecutedCount)

        // Draft action should be created and active
        val activeDraft = engine.activeDraft.value
        assertNotNull(activeDraft)
        assertEquals(DraftActionType.CREATE_TASK, activeDraft?.type)
        assertEquals("Buy milk", activeDraft?.title)

        // AI message informs user about draft template popup
        assertTrue(msg.content.contains("Draft Template Created"))
    }

    @Test
    fun `user draft confirmation triggers Core save command execution`() = runBlocking {
        val capability = DefaultAiCapability()
        val engine = GeminiChatboxEngine(aiCapability = capability)

        engine.sendMessage("Create task Finish project")

        val draft = engine.activeDraft.value
        assertNotNull(draft)

        var executedCommandTitle: String? = null

        val result = engine.confirmAndSaveDraft(draft!!) { cmd ->
            executedCommandTitle = draft.title
            RemmiCommandResult.Success("Saved")
        }

        assertTrue(result is RemmiCommandResult.Success)
        assertEquals("Finish project", executedCommandTitle)

        // Draft is cleared after confirmation
        assertNull(engine.activeDraft.value)
    }
}

package com.remmi.core.ai.chat

import com.remmi.core.ai.DefaultAiCapability
import com.remmi.core.eventBus.commands.NavigateCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiChatboxEngineTest {

    @Test
    fun `log reader captures formatted snapshot across categories`() {
        val reader = GeminiLogReader()
        val snapshot = reader.captureSnapshot(LogCategory.ALL)

        assertNotNull(snapshot)
        assertEquals(LogCategory.ALL, snapshot.category)
        assertTrue(snapshot.detailedLines.isNotEmpty())

        val prefix = reader.buildLogPromptPrefix(snapshot)
        assertTrue(prefix.contains("LOG CATEGORY: All Logs & Diagnostics"))
    }

    @Test
    fun `chatbox engine handles user messages and resolves system navigation command`() = runBlocking {
        val capability = DefaultAiCapability()
        val engine = GeminiChatboxEngine(aiCapability = capability)

        val aiMsg = engine.sendMessage("Open settings")

        assertEquals(ChatSender.AI, aiMsg.sender)
        assertNotNull(aiMsg.resolvedCommand)
        assertTrue(aiMsg.resolvedCommand is NavigateCommand)
        assertEquals("SETTINGS", (aiMsg.resolvedCommand as NavigateCommand).destinationId)
    }

    @Test
    fun `chatbox engine attaches requested log category snapshot to user message`() = runBlocking {
        val engine = GeminiChatboxEngine()

        engine.sendMessage("Check my diagnostics", attachLogsCategory = LogCategory.DIAGNOSTICS)

        val messages = engine.messages.value
        val userMsg = messages.find { it.sender == ChatSender.USER }

        assertNotNull(userMsg)
        assertNotNull(userMsg?.attachedLogSnapshot)
        assertEquals(LogCategory.DIAGNOSTICS, userMsg?.attachedLogSnapshot?.category)
    }
}

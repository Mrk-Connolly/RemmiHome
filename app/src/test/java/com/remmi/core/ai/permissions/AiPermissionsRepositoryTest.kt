package com.remmi.core.ai.permissions

import com.remmi.core.ai.permissions.models.AiActionHistoryEntry
import com.remmi.core.ai.permissions.models.AiPermissionsConfig
import com.remmi.core.ai.permissions.repository.AiPermissionsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AiPermissionsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: AiPermissionsRepository

    @Before
    fun setUp() {
        val storageDir = tempFolder.newFolder("ai_perm_test")
        repository = AiPermissionsRepository(storageDir = storageDir)
    }

    @Test
    fun testDomainReadAndWritePermissions() {
        assertTrue(repository.isReadAllowed("Calendar"))
        assertTrue(repository.isReadAllowed("Health"))
        assertTrue(repository.isWriteAllowed("Tasks"))

        assertFalse(repository.isReadAllowed("UnregisteredDomain"))
    }

    @Test
    fun testHighRiskConfirmationActions() {
        assertTrue(repository.isConfirmationRequired("delete_event"))
        assertTrue(repository.isConfirmationRequired("delete_task"))
        assertTrue(repository.isConfirmationRequired("delete_habit"))

        assertFalse(repository.isConfirmationRequired("get_events"))
    }

    @Test
    fun testUnrestrictedAndroidApisAreStrictlyBlocked() {
        assertTrue("Unrestricted shell API must be blocked", repository.isUnrestrictedApiBlocked("shell_execute"))
        assertTrue("Unrestricted system setting write must be blocked", repository.isUnrestrictedApiBlocked("write_secure_settings"))

        val configWithUnrestricted = AiPermissionsConfig(allowUnrestrictedAndroidApis = true)
        runBlocking {
            repository.updatePermissionsConfig(configWithUnrestricted)
        }

        // Hard-block policy is enforced regardless of config overrides
        assertFalse(repository.permissionsConfig.value.allowUnrestrictedAndroidApis)
        assertTrue(repository.isUnrestrictedApiBlocked("shell_execute"))
    }

    @Test
    fun testActionHistoryLogging() = runBlocking {
        val entry1 = AiActionHistoryEntry(actionName = "get_events", domain = "Calendar", accessType = "READ")
        val entry2 = AiActionHistoryEntry(actionName = "delete_task", domain = "Tasks", accessType = "WRITE", userConfirmed = true)

        repository.recordActionHistory(entry1)
        repository.recordActionHistory(entry2)

        val history = repository.actionHistory.value
        assertEquals(2, history.size)
        assertEquals("get_events", history[0].actionName)
        assertEquals("delete_task", history[1].actionName)
        assertTrue(history[1].userConfirmed)
    }

    @Test
    fun testPersistenceAndReload() = runBlocking {
        val storageDir = tempFolder.newFolder("perm_reload")
        val repo1 = AiPermissionsRepository(storageDir = storageDir)

        repo1.recordActionHistory(AiActionHistoryEntry(actionName = "create_event", domain = "Calendar"))

        val repo2 = AiPermissionsRepository(storageDir = storageDir)
        repo2.loadFromStorage()

        val history = repo2.actionHistory.value
        assertEquals(1, history.size)
        assertEquals("create_event", history.first().actionName)
    }
}

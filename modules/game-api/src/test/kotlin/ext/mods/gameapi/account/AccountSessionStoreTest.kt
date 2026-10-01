package ext.mods.gameapi.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountSessionStoreTest {
    @Test
    fun `issues and resolves opaque session`() {
        val store = AccountSessionStore(ttlMs = { 60_000L })
        val (token, issued) = store.issue("magicarrow", 8, 1)

        assertTrue(token.length >= 64)
        assertEquals("magicarrow", store.find(token)?.login)
        assertEquals(8, issued.accessLevel)
        assertEquals(1, store.size())
    }

    @Test
    fun `revokes session without storing the raw token`() {
        val store = AccountSessionStore(ttlMs = { 60_000L })
        val (token, _) = store.issue("magicarrow", 0, 1)

        assertNotNull(store.find(token))
        assertTrue(store.revoke(token))
        assertNull(store.find(token))
        assertEquals(0, store.size())
    }

    @Test
    fun `expires session`() {
        var now = 1_000L
        val store = AccountSessionStore({ 60_000L }) { now }
        val (token, _) = store.issue("magicarrow", 0, 1)

        assertNotNull(store.find(token))
        now = 61_000L
        assertNull(store.find(token))
    }
}

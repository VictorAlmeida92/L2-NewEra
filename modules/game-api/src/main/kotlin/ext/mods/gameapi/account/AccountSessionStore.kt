package ext.mods.gameapi.account

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.HexFormat
import java.util.concurrent.ConcurrentHashMap

class AccountSessionStore(
    private val ttlMs: () -> Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    data class Session(val login: String, val accessLevel: Int, val lastServer: Int, val expiresAt: Long)

    private data class StoredSession(val session: Session)

    private val random = SecureRandom()
    private val sessions = ConcurrentHashMap<String, StoredSession>()

    fun issue(login: String, accessLevel: Int, lastServer: Int): Pair<String, Session> {
        val tokenBytes = ByteArray(32)
        random.nextBytes(tokenBytes)
        val token = HexFormat.of().formatHex(tokenBytes)
        val now = clock()
        val session = Session(login, accessLevel, lastServer, now + ttlMs().coerceAtLeast(60_000L))
        sessions[hash(token)] = StoredSession(session)
        return token to session
    }

    fun find(token: String?): Session? {
        if (token.isNullOrBlank()) return null
        val key = hash(token)
        val stored = sessions[key] ?: return null
        if (stored.session.expiresAt <= clock()) {
            sessions.remove(key, stored)
            return null
        }
        return stored.session
    }

    fun revoke(token: String?): Boolean = token?.takeIf(String::isNotBlank)?.let { sessions.remove(hash(it)) != null } ?: false

    fun clearExpired() {
        val now = clock()
        sessions.entries.removeIf { it.value.session.expiresAt <= now }
    }

    fun size(): Int = sessions.size

    private fun hash(token: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
    )
}

package ext.mods.gameapi.account

import java.io.File
import java.util.Properties

/** Configuration for the server-side account BFF. Disabled by default. */
object AccountApiConfig {
    @Volatile var enabled: Boolean = false
        private set
    @Volatile var host: String = "127.0.0.1"
        private set
    @Volatile var port: Int = 9090
        private set
    @Volatile var rateLimitPerMinute: Int = 20
        private set
    @Volatile var sessionTtlMs: Long = 86_400_000L
        private set
    @Volatile var allowedOrigins: Set<String> = emptySet()
        private set

    fun load() {
        val props = Properties()
        findServerProperties()?.inputStream()?.use(props::load)
        enabled = props.bool("AccountApiEnabled", false)
        host = props.getProperty("AccountApiHost", "127.0.0.1").trim()
        port = props.int("AccountApiPort", 9090).coerceIn(1, 65535)
        rateLimitPerMinute = props.int("AccountApiRateLimit", 20).coerceAtLeast(1)
        sessionTtlMs = props.long("AccountApiSessionTtlMs", 86_400_000L).coerceIn(60_000L, 7 * 86_400_000L)
        allowedOrigins = props.getProperty("AccountApiAllowedOrigins", "")
            .split(',', ';')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
    }

    private fun findServerProperties(): File? = listOf(
        System.getProperty("brproject.server.properties"),
        "game/config/server.properties",
        "config/server.properties",
        "../game/config/server.properties",
        "../../game/config/server.properties"
    ).filterNotNull().map(::File).firstOrNull { it.isFile }

    private fun Properties.bool(key: String, default: Boolean): Boolean =
        getProperty(key)?.trim()?.equals("true", ignoreCase = true) ?: default

    private fun Properties.int(key: String, default: Int): Int =
        getProperty(key)?.trim()?.toIntOrNull() ?: default

    private fun Properties.long(key: String, default: Long): Long =
        getProperty(key)?.trim()?.toLongOrNull() ?: default
}

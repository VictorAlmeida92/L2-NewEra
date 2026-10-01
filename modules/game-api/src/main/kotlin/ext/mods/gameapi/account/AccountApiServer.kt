package ext.mods.gameapi.account

import com.google.gson.Gson
import ext.mods.commons.logging.CLogger
import ext.mods.gameapi.db.SiteApiRepository
import ext.mods.gameapi.security.RateLimiter
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpUtil
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Public-facing account BFF. It is disabled by default and never exposes JDBC or GameApiSecret. */
class AccountApiServer private constructor() : AutoCloseable {
    private val bossGroup: EventLoopGroup = NioEventLoopGroup(1)
    private val workerGroup: EventLoopGroup = NioEventLoopGroup()
    private val sessions = AccountSessionStore(ttlMs = { AccountApiConfig.sessionTtlMs })
    private val limiter = RateLimiter(AccountApiConfig.rateLimitPerMinute)
    private var channel: Channel? = null
    private var cleaner: ScheduledExecutorService? = null

    fun start() {
        if (!AccountApiConfig.enabled) {
            LOGGER.info("[account-api] disabled")
            return
        }
        require(AccountApiConfig.host in SAFE_HOSTS || AccountApiConfig.allowInsecureBind) {
            "Refusing unsafe AccountApiHost='${AccountApiConfig.host}'. Use loopback behind a TLS reverse proxy, or explicitly enable AccountApiAllowInsecureBind for local development."
        }
        if (AccountApiConfig.host !in SAFE_HOSTS) {
            LOGGER.warn("[account-api] insecure bind enabled for local development: {}", AccountApiConfig.host)
        }

        cleaner = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "account-api-cleaner").apply { isDaemon = true }
        }
        cleaner?.scheduleAtFixedRate({
            limiter.gc()
            sessions.clearExpired()
        }, 60, 60, TimeUnit.SECONDS)

        val bootstrap = ServerBootstrap()
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(ReadTimeoutHandler(30, TimeUnit.SECONDS))
                    ch.pipeline().addLast(HttpServerCodec())
                    ch.pipeline().addLast(HttpObjectAggregator(128 * 1024))
                    ch.pipeline().addLast(Handler(limiter, sessions))
                }
            })

        channel = bootstrap.bind(AccountApiConfig.host, AccountApiConfig.port).sync().channel()
        LOGGER.info("[account-api] listening on http://{}:{}", AccountApiConfig.host, AccountApiConfig.port)
    }

    override fun close() {
        channel?.close()?.syncUninterruptibly()
        cleaner?.shutdownNow()
        cleaner = null
        sessions.clearExpired()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
        LOGGER.info("[account-api] stopped")
    }

    private class Handler(
        private val limiter: RateLimiter,
        private val sessions: AccountSessionStore
    ) : SimpleChannelInboundHandler<FullHttpRequest>() {
        private val gson = Gson()

        override fun channelRead0(ctx: ChannelHandlerContext, req: FullHttpRequest) {
            ctx.channel().attr(KEEP_ALIVE).set(HttpUtil.isKeepAlive(req))
            val path = req.uri().substringBefore('?')
            val origin = req.headers().get(HttpHeaderNames.ORIGIN)
            val ip = remoteIp(ctx)

            if (req.method() == HttpMethod.OPTIONS && path.startsWith("/api/account/")) {
                return respond(ctx, HttpResponseStatus.NO_CONTENT, emptyMap<String, Any?>(), origin)
            }
            if (!limiter.tryAcquire(ip)) {
                return respond(ctx, HttpResponseStatus.TOO_MANY_REQUESTS, mapOf("ok" to false, "message" to "rate limit"), origin)
            }

            val bodyBytes = ByteArray(req.content().readableBytes())
            req.content().getBytes(0, bodyBytes)
            try {
                when {
                    req.method() == HttpMethod.GET && path == "/api/account/health" ->
                        respond(ctx, HttpResponseStatus.OK, mapOf("ok" to true, "service" to "account-api"), origin)
                    req.method() == HttpMethod.POST && path == "/api/account/register" -> handleRegister(ctx, bodyBytes, origin)
                    req.method() == HttpMethod.POST && path == "/api/account/login" -> handleLogin(ctx, bodyBytes, origin)
                    req.method() == HttpMethod.GET && path == "/api/account/me" -> handleMe(ctx, req, origin)
                    req.method() == HttpMethod.POST && path == "/api/account/logout" -> handleLogout(ctx, req, origin)
                    req.method() == HttpMethod.POST && path == "/api/account/change-password" -> handleChangePassword(ctx, req, bodyBytes, origin)
                    else -> respond(ctx, HttpResponseStatus.NOT_FOUND, mapOf("ok" to false, "message" to "not found"), origin)
                }
            } catch (e: Exception) {
                LOGGER.error("[account-api] request failed for {}", path, e)
                respond(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, mapOf("ok" to false, "message" to "Erro interno"), origin)
            }
        }

        private fun handleRegister(ctx: ChannelHandlerContext, body: ByteArray, origin: String?) {
            val request = gson.fromJson(String(body, Charsets.UTF_8), RegisterPayload::class.java)
            val validation = AccountApiValidation.validateLogin(request.login)
                ?: AccountApiValidation.validatePassword(request.password)
            if (validation != null) return respond(ctx, HttpResponseStatus.BAD_REQUEST, failure(validation), origin)
            val password = request.password!!.toCharArray()
            try {
                val result = when (SiteApiRepository.register(request.login!!, password)) {
                    SiteApiRepository.RegisterResult.CREATED -> HttpResponseStatus.CREATED to mapOf("ok" to true, "message" to "Conta criada")
                    SiteApiRepository.RegisterResult.DUPLICATE -> HttpResponseStatus.CONFLICT to failure("Login já existe")
                    SiteApiRepository.RegisterResult.ERROR -> HttpResponseStatus.INTERNAL_SERVER_ERROR to failure("Erro interno")
                }
                respond(ctx, result.first, result.second, origin)
            } finally {
                password.fill('\u0000')
            }
        }

        private fun handleLogin(ctx: ChannelHandlerContext, body: ByteArray, origin: String?) {
            val request = gson.fromJson(String(body, Charsets.UTF_8), LoginPayload::class.java)
            val validation = AccountApiValidation.validateLogin(request.login)
                ?: AccountApiValidation.validatePassword(request.password)
            if (validation != null) return respond(ctx, HttpResponseStatus.BAD_REQUEST, failure(validation), origin)
            val login = request.login!!
            val password = request.password!!.toCharArray()
            try {
                val result = SiteApiRepository.login(login, password)
                if (!result.ok) return respond(ctx, HttpResponseStatus.UNAUTHORIZED, failure("Login ou senha inválidos"), origin)
                val (token, session) = sessions.issue(login.trim().lowercase(), result.accessLevel, result.lastServer)
                respond(ctx, HttpResponseStatus.OK, mapOf("ok" to true, "accessToken" to token, "expiresAt" to session.expiresAt), origin)
            } finally {
                password.fill('\u0000')
            }
        }

        private fun handleMe(ctx: ChannelHandlerContext, req: FullHttpRequest, origin: String?) {
            val session = session(req)
                ?: return respond(ctx, HttpResponseStatus.UNAUTHORIZED, failure("Sessão inválida"), origin)
            respond(ctx, HttpResponseStatus.OK, mapOf("ok" to true, "login" to session.login, "accessLevel" to session.accessLevel, "lastServer" to session.lastServer, "expiresAt" to session.expiresAt), origin)
        }

        private fun handleLogout(ctx: ChannelHandlerContext, req: FullHttpRequest, origin: String?) {
            val token = bearer(req)
            sessions.revoke(token)
            respond(ctx, HttpResponseStatus.OK, mapOf("ok" to true), origin)
        }

        private fun handleChangePassword(ctx: ChannelHandlerContext, req: FullHttpRequest, body: ByteArray, origin: String?) {
            val session = session(req)
                ?: return respond(ctx, HttpResponseStatus.UNAUTHORIZED, failure("Sessão inválida"), origin)
            val request = gson.fromJson(String(body, Charsets.UTF_8), ChangePasswordPayload::class.java)
            val validation = AccountApiValidation.validatePasswordPair(request.currentPassword, request.newPassword)
            if (validation != null) return respond(ctx, HttpResponseStatus.BAD_REQUEST, failure(validation), origin)
            val current = request.currentPassword!!.toCharArray()
            val next = request.newPassword!!.toCharArray()
            try {
                val result = SiteApiRepository.changePassword(session.login, current, next)
                respond(ctx, if (result.ok) HttpResponseStatus.OK else HttpResponseStatus.BAD_REQUEST, result, origin)
            } finally {
                current.fill('\u0000')
                next.fill('\u0000')
            }
        }

        private fun session(req: FullHttpRequest): AccountSessionStore.Session? = sessions.find(bearer(req))

        private fun bearer(req: FullHttpRequest): String? {
            val value = req.headers().get(HttpHeaderNames.AUTHORIZATION) ?: return null
            return value.takeIf { it.startsWith("Bearer ", ignoreCase = true) }?.substring(7)?.trim()
        }

        private fun failure(message: String) = mapOf("ok" to false, "message" to message)

        private fun respond(ctx: ChannelHandlerContext, status: HttpResponseStatus, payload: Any, origin: String?) {
            val json = gson.toJson(payload)
            val content = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content)
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
            if (origin != null && origin in AccountApiConfig.allowedOrigins) {
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type")
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS")
                response.headers().set(HttpHeaderNames.VARY, "Origin")
            }
            if (ctx.channel().attr(KEEP_ALIVE).get() == true) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                ctx.writeAndFlush(response)
            } else {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                ctx.writeAndFlush(response).addListener { ctx.close() }
            }
        }

        private fun remoteIp(ctx: ChannelHandlerContext): String =
            ((ctx.channel().remoteAddress() as? InetSocketAddress)?.address?.hostAddress) ?: "unknown"

        private data class RegisterPayload(val login: String? = null, val password: String? = null)
        private data class LoginPayload(val login: String? = null, val password: String? = null)
        private data class ChangePasswordPayload(val currentPassword: String? = null, val newPassword: String? = null)

        override fun channelReadComplete(ctx: ChannelHandlerContext) {
            ctx.flush()
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            LOGGER.debug("[account-api] channel exception: {}", cause.message)
            ctx.close()
        }

        companion object {
            private val KEEP_ALIVE = io.netty.util.AttributeKey.valueOf<Boolean>("account_api_keep_alive")
        }
    }

    companion object {
        private val LOGGER = CLogger(AccountApiServer::class.java.name)
        private val SAFE_HOSTS = setOf("127.0.0.1", "localhost", "::1")
        @Volatile private var instance: AccountApiServer? = null

        @JvmStatic
        fun startFromGameServer() {
            AccountApiConfig.load()
            if (!AccountApiConfig.enabled) {
                LOGGER.info("[account-api] disabled")
                return
            }
            val server = AccountApiServer()
            server.start()
            instance = server
        }

        @JvmStatic
        fun stopFromGameServer() {
            instance?.close()
            instance = null
        }
    }
}

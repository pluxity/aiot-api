package com.pluxity.aiot.global.messaging.interceptor

import com.pluxity.aiot.authentication.security.JwtProvider
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.messaging.dto.StompPrincipal
import com.pluxity.aiot.global.properties.JwtProperties
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal

@Component
class MyDefaultHandshakeHandler(
    private val jwtProperties: JwtProperties,
    private val jwtProvider: JwtProvider,
) : DefaultHandshakeHandler() {
    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Principal? {
        val servletRequest = (request as ServletServerHttpRequest).servletRequest
        val token = servletRequest.cookies?.find { it.name == jwtProperties.accessToken.name }?.value
        token?.let {
            attributes["username"] = jwtProvider.extractUsername(it)
        } ?: throw CustomException(ErrorCode.NOT_FOUND_USER)
        return StompPrincipal()
    }
}

package com.pluxity.aiot.authentication.security

import com.pluxity.aiot.global.constant.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/** 지정하지 않으면 Http403ForbiddenEntryPoint로 떨어져 미인증에 403이 나간다. */
@Component
class RestAuthenticationEntryPoint : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = AuthenticationErrorWriter.write(response, ErrorCode.UNAUTHENTICATED)
}

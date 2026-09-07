package com.pluxity.aiot.authentication.security

import com.pluxity.aiot.global.exception.CustomException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

private val log = KotlinLogging.logger {}

class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val userDetailsService: UserDetailsService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        runCatching {
            if (authenticationRequired(request)) {
                authenticateRequest(request)
            }
        }.onFailure { exception ->
            if (exception is CustomException) {
                AuthenticationErrorWriter.write(response, exception.errorCode, exception.message)
                return
            }
            log.error(exception) { "인증 필터에서 처리하지 못한 예외 (uri=${request.requestURI})" }
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticateRequest(request: HttpServletRequest) {
        val token = jwtProvider.getAccessTokenFromRequest(request)

        if (token == null) return

        jwtProvider.validateAccessToken(token)
        val username = jwtProvider.extractUsername(token)
        val userDetails = userDetailsService.loadUserByUsername(username)
        setAuthenticationContext(request, userDetails)
    }

    private fun setAuthenticationContext(
        request: HttpServletRequest,
        userDetails: UserDetails,
    ) {
        val authToken = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authToken
    }

    private fun authenticationRequired(request: HttpServletRequest): Boolean {
        val path = request.requestURI.substring(request.contextPath.length)
        return !WhiteListPath.isWhiteListed(path)
    }
}

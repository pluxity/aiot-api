package com.pluxity.aiot.authentication.service

import com.pluxity.aiot.authentication.dto.SignInRequest
import com.pluxity.aiot.authentication.dto.SignUpRequest
import com.pluxity.aiot.authentication.entity.RefreshToken
import com.pluxity.aiot.authentication.repository.RefreshTokenRepository
import com.pluxity.aiot.authentication.security.JwtProvider
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.JwtProperties
import com.pluxity.aiot.user.entity.User
import com.pluxity.aiot.user.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
@Transactional(readOnly = true)
class AuthenticationService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val authenticationManager: AuthenticationManager,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProperties: JwtProperties,
) {
    @Transactional
    fun signUp(signUpRequest: SignUpRequest): Long {
        validateUserDoesNotExist(signUpRequest.username)

        val user =
            User(
                username = signUpRequest.username,
                password = requireNotNull(passwordEncoder.encode(signUpRequest.password)),
                name = signUpRequest.name,
                code = signUpRequest.code,
            )

        return userRepository.save(user).requiredId
    }

    fun signIn(
        signInRequest: SignInRequest,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        authenticateUser(signInRequest)
        val user = findUserByUsername(signInRequest.username)
        publishToken(user, request, response)
    }

    fun signOut(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val refreshToken = jwtProvider.getJwtFromRequest(jwtProperties.refreshToken.name, request)
        refreshToken
            ?.let { refreshTokenRepository.findByToken(it) }
            ?.let { refreshTokenRepository.delete(it) }

        // 저장소에 지울 토큰이 없어도 브라우저 쿠키는 지워야 한다
        clearAllCookies(request, response)
    }

    fun refreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val refreshToken =
            jwtProvider.getJwtFromRequest(jwtProperties.refreshToken.name, request)
                ?: throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

        jwtProvider.validateRefreshToken(refreshToken)

        val username = jwtProvider.extractUsername(refreshToken, true)
        val user = findUserByUsername(username)
        publishToken(user, request, response)
    }

    private fun validateUserDoesNotExist(username: String) {
        if (userRepository.findByUsername(username) != null) {
            throw CustomException(ErrorCode.DUPLICATE_USERNAME, "사용자가 이미 존재합니다: $username")
        }
    }

    private fun authenticateUser(signInRequest: SignInRequest) {
        runCatching {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(signInRequest.username, signInRequest.password),
            )
        }.getOrElse {
            throw CustomException(ErrorCode.INVALID_ID_OR_PASSWORD)
        }
    }

    private fun findUserByUsername(username: String): User =
        userRepository
            .findByUsername(username)
            ?: throw CustomException(ErrorCode.NOT_FOUND_USER, username)

    private fun clearAllCookies(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        listOf(jwtProperties.accessToken.name, jwtProperties.refreshToken.name, EXPIRY_COOKIE_NAME)
            .forEach { name -> expireCookie(name, cookiePath(request), response) }
    }

    /** 요청 쿠키를 찾아 지우면 쿠키가 안 실려온 요청에는 Set-Cookie가 하나도 안 나간다. */
    private fun expireCookie(
        name: String,
        path: String,
        response: HttpServletResponse,
    ) {
        val cookie =
            ResponseCookie
                .from(name, "")
                .secure(false)
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(0)
                .path(path)
                .build()
                .toString()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie)
    }

    private fun publishToken(
        user: User,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val newAccessToken = jwtProvider.generateAccessToken(user.username)
        val newRefreshToken = jwtProvider.generateRefreshToken(user.username)

        createAuthCookie(
            jwtProperties.accessToken.name,
            newAccessToken,
            jwtProperties.accessToken.expiration,
            cookiePath(request),
            response,
        )
        createAuthCookie(
            jwtProperties.refreshToken.name,
            newRefreshToken,
            jwtProperties.refreshToken.expiration,
            cookiePath(request),
            response,
        )
        createExpiryCookie(request, response)

        refreshTokenRepository.save(RefreshToken(user.username, newRefreshToken, jwtProperties.refreshToken.expiration.toSeconds()))
    }

    private fun createAuthCookie(
        name: String,
        value: String,
        expiry: Duration,
        path: String,
        response: HttpServletResponse,
    ) {
        val cookie =
            ResponseCookie
                .from(name, value)
                .secure(false)
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(expiry)
                .path(path)
                .build()
                .toString()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie)
    }

    private fun createExpiryCookie(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val expiryTimeMillis = System.currentTimeMillis() + jwtProperties.refreshToken.expiration.toMillis()

        val cookie =
            ResponseCookie
                .from(EXPIRY_COOKIE_NAME, expiryTimeMillis.toString())
                .secure(false)
                .path(cookiePath(request))
                .build()
                .toString()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie)
    }

    /** 발급과 삭제의 경로가 어긋나면 브라우저는 삭제 지시를 무시한다. */
    private fun cookiePath(request: HttpServletRequest): String = request.contextPath.takeIf { it.isNotBlank() } ?: "/"

    companion object {
        private const val EXPIRY_COOKIE_NAME = "expiry"
    }
}

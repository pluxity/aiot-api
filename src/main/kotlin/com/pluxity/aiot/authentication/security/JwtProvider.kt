package com.pluxity.aiot.authentication.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.pluxity.aiot.authentication.repository.RefreshTokenRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.JwtProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import org.springframework.web.util.WebUtils
import java.time.Duration
import java.util.Base64
import java.util.Date

@Service
class JwtProvider(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
) {
    fun extractUsername(
        token: String,
        isRefreshToken: Boolean = false,
    ): String = extractAllClaims(token, isRefreshToken).subject

    /** SignedJWT.verify()는 서명만 본다. 아래 exp·nbf 검사를 빼면 만료·미개시 토큰이 통과한다. */
    fun extractAllClaims(
        token: String,
        isRefreshToken: Boolean = false,
    ): JWTClaimsSet {
        val signedJwt =
            runCatching { SignedJWT.parse(token) }
                .getOrElse { throw invalidTokenException(isRefreshToken) }

        val verified =
            runCatching { signedJwt.verify(MACVerifier(secretKeyBytes(isRefreshToken))) }
                .getOrElse { throw invalidTokenException(isRefreshToken) }

        if (!verified) throw invalidTokenException(isRefreshToken)

        val claims = signedJwt.jwtClaimsSet
        val now = Date()

        val expiresAt = claims.expirationTime ?: throw invalidTokenException(isRefreshToken)
        if (expiresAt.before(now)) throw expiredTokenException(isRefreshToken)

        val notBefore = claims.notBeforeTime
        if (notBefore != null && now.before(notBefore)) throw invalidTokenException(isRefreshToken)

        return claims
    }

    fun generateAccessToken(
        username: String,
        extraClaims: Map<String, Any> = emptyMap(),
    ): String = buildToken(extraClaims, username, jwtProperties.accessToken.expiration, false)

    fun generateRefreshToken(username: String): String = buildToken(emptyMap(), username, jwtProperties.refreshToken.expiration, true)

    private fun buildToken(
        extraClaims: Map<String, Any>,
        username: String,
        expiration: Duration,
        isRefreshToken: Boolean,
    ): String {
        val issuedAt = System.currentTimeMillis()
        val claimsBuilder = JWTClaimsSet.Builder()
        extraClaims.forEach { (key, value) -> claimsBuilder.claim(key, value) }

        val claims =
            claimsBuilder
                .subject(username)
                .issueTime(Date(issuedAt))
                .expirationTime(Date(issuedAt + expiration.toMillis()))
                .build()

        val signedJwt = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
        signedJwt.sign(MACSigner(secretKeyBytes(isRefreshToken)))
        return signedJwt.serialize()
    }

    fun validateAccessToken(token: String) {
        extractAllClaims(token, false)
    }

    fun validateRefreshToken(token: String?) {
        if (token.isNullOrBlank()) throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

        val stored =
            refreshTokenRepository.findByToken(token)
                ?: throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

        if (!stored.isValidToken()) throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

        extractAllClaims(stored.token, true)
    }

    /** jjwt가 쓰던 키 유도 그대로다. 바꾸면 이미 발급한 토큰이 전부 무효가 된다. */
    private fun secretKeyBytes(isRefreshToken: Boolean): ByteArray =
        Base64.getDecoder().decode(
            if (isRefreshToken) jwtProperties.refreshToken.secretKey else jwtProperties.accessToken.secretKey,
        )

    private fun invalidTokenException(isRefreshToken: Boolean) =
        CustomException(if (isRefreshToken) ErrorCode.INVALID_REFRESH_TOKEN else ErrorCode.INVALID_ACCESS_TOKEN)

    private fun expiredTokenException(isRefreshToken: Boolean) =
        CustomException(if (isRefreshToken) ErrorCode.EXPIRED_REFRESH_TOKEN else ErrorCode.EXPIRED_ACCESS_TOKEN)

    fun getAccessTokenFromRequest(request: HttpServletRequest): String? = getJwtFromRequest(jwtProperties.accessToken.name, request)

    fun getJwtFromRequest(
        name: String,
        request: HttpServletRequest,
    ): String? = WebUtils.getCookie(request, name)?.value
}

package com.pluxity.aiot.authentication.service

import com.pluxity.aiot.authentication.dto.SignInRequest
import com.pluxity.aiot.authentication.entity.RefreshToken
import com.pluxity.aiot.authentication.repository.RefreshTokenRepository
import com.pluxity.aiot.authentication.security.JwtProvider
import com.pluxity.aiot.global.properties.JwtProperties
import com.pluxity.aiot.global.properties.TokenProperties
import com.pluxity.aiot.user.repository.UserRepository
import com.pluxity.aiot.user.service.entity.dummyUser
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Duration

/** Redis TTL과 쿠키 Max-Age는 둘 다 초를 받는다. toMillis()가 들어가면 27년이 된다. */
class AuthenticationServiceKoTest :
    BehaviorSpec({
        val refreshTokenRepository: RefreshTokenRepository = mockk()
        val userRepository: UserRepository = mockk()
        val jwtProvider: JwtProvider = mockk()
        val authenticationManager: AuthenticationManager = mockk()
        val passwordEncoder: PasswordEncoder = mockk()
        val jwtProperties =
            JwtProperties(
                accessToken = TokenProperties("AccessToken", "access-secret", Duration.ofHours(10)),
                refreshToken = TokenProperties("RefreshToken", "refresh-secret", Duration.ofDays(10)),
            )
        val authenticationService =
            AuthenticationService(
                refreshTokenRepository,
                userRepository,
                jwtProvider,
                authenticationManager,
                passwordEncoder,
                jwtProperties,
            )

        Given("쿠키를 하나도 들고 오지 않은 로그아웃 요청") {
            When("로그아웃하면") {
                every { jwtProvider.getJwtFromRequest(any(), any()) } returns null

                val response = MockHttpServletResponse()
                authenticationService.signOut(MockHttpServletRequest(), response)

                val setCookies = response.getHeaders(HttpHeaders.SET_COOKIE)

                Then("세 쿠키 모두 만료 지시가 나간다") {
                    setCookies.count { it.contains("Max-Age=0") } shouldBe 3
                }

                Then("브라우저가 지울 수 있도록 이름이 모두 실린다") {
                    setCookies.any { it.startsWith("AccessToken=") } shouldBe true
                    setCookies.any { it.startsWith("RefreshToken=") } shouldBe true
                    setCookies.any { it.startsWith("expiry=") } shouldBe true
                }
            }
        }

        Given("액세스 10시간·리프레시 10일로 설정된 상태") {
            When("로그인에 성공해 토큰을 발급하면") {
                every { authenticationManager.authenticate(any()) } returns mockk<Authentication>()
                every { userRepository.findByUsername("tester") } returns dummyUser(username = "tester")
                every { jwtProvider.generateAccessToken("tester") } returns "access-token"
                every { jwtProvider.generateRefreshToken("tester") } returns "refresh-token"

                val savedRefreshToken = slot<RefreshToken>()
                every { refreshTokenRepository.save(capture(savedRefreshToken)) } answers { savedRefreshToken.captured }

                val response = MockHttpServletResponse()
                authenticationService.signIn(
                    SignInRequest("tester", "password"),
                    MockHttpServletRequest(),
                    response,
                )

                val setCookies = response.getHeaders(HttpHeaders.SET_COOKIE)

                Then("Redis TTL은 10일을 초로 환산한 값이다") {
                    savedRefreshToken.captured.timeToLive shouldBe 864_000L
                }

                Then("액세스 토큰 쿠키의 Max-Age는 10시간을 초로 환산한 값이다") {
                    setCookies.first { it.startsWith("AccessToken=") } shouldContainMaxAge 36_000L
                }

                Then("리프레시 토큰 쿠키의 Max-Age는 10일을 초로 환산한 값이다") {
                    setCookies.first { it.startsWith("RefreshToken=") } shouldContainMaxAge 864_000L
                }
            }
        }
    })

private infix fun String.shouldContainMaxAge(seconds: Long) {
    val maxAge =
        Regex("Max-Age=(-?\\d+)")
            .find(this)
            ?.groupValues
            ?.get(1)
            ?.toLong()
    maxAge shouldBe seconds
}

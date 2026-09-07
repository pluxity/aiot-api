package com.pluxity.aiot.authentication.security

import com.pluxity.aiot.authentication.repository.RefreshTokenRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.JwtProperties
import com.pluxity.aiot.global.properties.TokenProperties
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.time.Duration

/** nimbus의 SignedJWT.verify()는 서명만 본다. 만료 검사를 빠뜨리면 여기서 걸린다. */
class JwtProviderValidationKoTest :
    BehaviorSpec({
        val accessSecret = "access-secret-key-that-is-long-enough-32"
        val refreshSecret = "refresh-secret-key-that-is-long-enough-32"

        fun providerWith(
            accessLifetime: Duration,
            refreshLifetime: Duration = Duration.ofDays(10),
        ) = JwtProvider(
            mockk<RefreshTokenRepository>(),
            JwtProperties(
                accessToken = TokenProperties("AccessToken", accessSecret, accessLifetime),
                refreshToken = TokenProperties("RefreshToken", refreshSecret, refreshLifetime),
            ),
        )

        Given("이미 만료된 액세스 토큰") {
            val provider = providerWith(accessLifetime = Duration.ofSeconds(-60))
            val expiredToken = provider.generateAccessToken("tester")

            When("검증하면") {
                Then("만료로 거절한다") {
                    val exception = shouldThrow<CustomException> { provider.validateAccessToken(expiredToken) }
                    exception.errorCode shouldBe ErrorCode.EXPIRED_ACCESS_TOKEN
                }
            }
        }

        Given("서명이 위조된 액세스 토큰") {
            val provider = providerWith(accessLifetime = Duration.ofHours(10))
            val tampered = provider.generateAccessToken("tester").dropLast(4) + "AAAA"

            When("검증하면") {
                Then("유효하지 않다고 거절한다") {
                    val exception = shouldThrow<CustomException> { provider.validateAccessToken(tampered) }
                    exception.errorCode shouldBe ErrorCode.INVALID_ACCESS_TOKEN
                }
            }
        }

        Given("리프레시 키로 서명된 토큰") {
            val provider = providerWith(accessLifetime = Duration.ofHours(10))
            val refreshToken = provider.generateRefreshToken("tester")

            When("액세스 토큰으로 검증하면") {
                Then("키가 달라 거절한다") {
                    shouldThrow<CustomException> { provider.validateAccessToken(refreshToken) }
                }
            }
        }

        Given("정상 액세스 토큰") {
            val provider = providerWith(accessLifetime = Duration.ofHours(10))
            val token = provider.generateAccessToken("tester")

            When("사용자명을 꺼내면") {
                Then("발급 때 넣은 값이 나온다") {
                    provider.extractUsername(token) shouldBe "tester"
                }
            }
        }
    })

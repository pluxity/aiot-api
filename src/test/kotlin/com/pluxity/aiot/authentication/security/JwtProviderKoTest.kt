package com.pluxity.aiot.authentication.security

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

/** 간격을 밖에서 찍은 시각으로 재면 경과분이 섞여 초 단위 버림에서 흔들린다. */
@SpringBootTest
@ActiveProfiles("test")
class JwtProviderKoTest(
    private val jwtProvider: JwtProvider,
) : BehaviorSpec({
        extension(SpringExtension(SpringTestLifecycleMode.Root))

        fun lifetimeOf(
            token: String,
            isRefreshToken: Boolean,
        ): Duration {
            val claims = jwtProvider.extractAllClaims(token, isRefreshToken)
            return Duration.between(claims.issueTime.toInstant(), claims.expirationTime.toInstant())
        }

        Given("액세스 토큰 수명이 10시간으로 설정된 상태") {
            When("액세스 토큰을 발급하면") {
                val lifetime = lifetimeOf(jwtProvider.generateAccessToken("tester"), false)

                Then("10시간 뒤에 만료된다") {
                    lifetime.toMinutes() shouldBe Duration.ofHours(10).toMinutes()
                }
            }
        }

        Given("리프레시 토큰 수명이 10일로 설정된 상태") {
            When("리프레시 토큰을 발급하면") {
                val lifetime = lifetimeOf(jwtProvider.generateRefreshToken("tester"), true)

                Then("10일 뒤에 만료된다") {
                    lifetime.toHours() shouldBe Duration.ofDays(10).toHours()
                }
            }
        }
    })

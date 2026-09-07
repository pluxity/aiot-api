package com.pluxity.aiot.authentication.security

import io.jsonwebtoken.Claims
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

/**
 * 설정에 적힌 수명이 토큰의 exp 클레임에 그대로 반영되는지 확인한다.
 *
 * yml의 숫자는 밀리초 의도로 작성됐는데 코드가 초로 읽어 ×1000 하고 있었다.
 * 설정과 코드 어느 한쪽만 봐서는 드러나지 않아 실제 발급 결과로 단언한다.
 *
 * 간격은 벽시계가 아니라 토큰 자신의 iat↔exp로 잰다. 발급 시각을 밖에서 찍으면
 * 그 사이 경과분이 섞여 초 단위 버림에서 흔들린다.
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtProviderKoTest(
    private val jwtProvider: JwtProvider,
) : BehaviorSpec({
        extension(SpringExtension)

        fun lifetimeOf(
            token: String,
            isRefreshToken: Boolean,
        ): Duration {
            val issuedAt = jwtProvider.extractClaim(token, Claims::getIssuedAt, isRefreshToken)
            val expiresAt = jwtProvider.extractClaim(token, Claims::getExpiration, isRefreshToken)
            return Duration.between(issuedAt.toInstant(), expiresAt.toInstant())
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

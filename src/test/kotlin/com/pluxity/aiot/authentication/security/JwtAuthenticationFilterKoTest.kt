package com.pluxity.aiot.authentication.security

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService

/** 유효하지 않은 토큰을 들려보내, 화이트리스트를 건너뛰었는지를 응답 코드로 가른다. */
class JwtAuthenticationFilterKoTest :
    BehaviorSpec({
        val jwtProvider: JwtProvider = mockk()
        val userDetailsService: UserDetailsService = mockk()
        val filter = JwtAuthenticationFilter(jwtProvider, userDetailsService)

        every { jwtProvider.getAccessTokenFromRequest(any()) } returns "invalid-token"
        every { jwtProvider.validateAccessToken("invalid-token") } throws CustomException(ErrorCode.INVALID_ACCESS_TOKEN)

        fun callFilter(path: String): Pair<MockHttpServletResponse, MockFilterChain> {
            SecurityContextHolder.clearContext()
            val request = MockHttpServletRequest("GET", path)
            val response = MockHttpServletResponse()
            val chain = MockFilterChain()
            filter.doFilter(request, response, chain)
            return response to chain
        }

        Given("화이트리스트에 health가 있을 때") {
            When("/health로 요청하면") {
                val (response, _) = callFilter("/health")

                Then("토큰 검사 없이 통과한다") {
                    response.status shouldBe 200
                }
            }

            When("/health-actions로 요청하면") {
                val (response, _) = callFilter("/health-actions")

                Then("이름이 겹칠 뿐 다른 경로이므로 토큰을 검사한다") {
                    response.status shouldBe 401
                }
            }
        }

        Given("화이트리스트에 info가 있을 때") {
            When("/information으로 요청하면") {
                val (response, _) = callFilter("/information")

                Then("토큰을 검사한다") {
                    response.status shouldBe 401
                }
            }
        }

        Given("화이트리스트 경로의 하위 경로와 확장자") {
            When("/actuator/metrics로 요청하면") {
                val (response, _) = callFilter("/actuator/metrics")

                Then("하위 경로도 통과한다") {
                    response.status shouldBe 200
                }
            }

            When("/swagger-ui.html로 요청하면") {
                val (response, _) = callFilter("/swagger-ui.html")

                Then("확장자가 붙어도 통과한다") {
                    response.status shouldBe 200
                }
            }
        }
    })

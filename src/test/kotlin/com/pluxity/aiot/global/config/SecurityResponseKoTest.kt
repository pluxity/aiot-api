package com.pluxity.aiot.global.config

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.web.FilterChainProxy
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/** 프론트는 401을 토큰 갱신 신호로, 403을 권한 부족으로 나눠 처리한다. */
@SpringBootTest
@ActiveProfiles("test")
class SecurityResponseKoTest(
    private val context: WebApplicationContext,
    private val springSecurityFilterChain: FilterChainProxy,
) : BehaviorSpec({
        extension(SpringExtension)

        val mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurityFilterChain)
                .build()

        Given("인증 쿠키 없이") {
            When("인증이 필요한 엔드포인트에 요청하면") {
                val result =
                    mockMvc.post("/sites") {
                        contentType = MediaType.APPLICATION_JSON
                        content = "{}"
                    }

                Then("권한 부족이 아니라 미인증이므로 401이다") {
                    result.andExpect { status { isUnauthorized() } }
                }
            }

            When("GET /users/me를 요청하면") {
                val result = mockMvc.get("/users/me")

                Then("GET permitAll보다 앞에서 걸려 401이다") {
                    result.andExpect { status { isUnauthorized() } }
                }
            }
        }
    })

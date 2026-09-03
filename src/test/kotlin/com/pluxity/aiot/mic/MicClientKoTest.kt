package com.pluxity.aiot.mic

import com.pluxity.aiot.global.config.WebClientFactory
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.MicProperties
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.client.WebClient
import java.net.ServerSocket

/** 아무도 듣지 않는 포트를 잡아 연결 거부를 만든다 */
private fun closedPort(): Int = ServerSocket(0).use { it.localPort }

private fun client(port: Int): MicClient {
    val properties =
        MicProperties(enabled = true, baseUrl = "http://127.0.0.1:$port", username = "api", password = "pw")
    val factory = WebClientFactory(WebClient.builder())
    return MicClient(factory, properties)
}

class MicClientKoTest :
    BehaviorSpec({

        Given("업체 서버가 응답하지 않음") {
            When("토큰은 있으나 목록 조회가 연결 거부로 끝남") {
                val micClient = client(closedPort())
                // 로그인 단계를 건너뛰고 목록 조회 경로만 확인한다
                ReflectionTestUtils.setField(micClient, "accessToken", "token")

                Then("전송 계층 실패도 도메인 예외로 변환한다") {
                    // WebClientRequestException은 응답 예외의 하위 타입이 아니라 따로 잡지 않으면 500으로 샌다
                    val exception = shouldThrowExactly<CustomException> { micClient.getMicList() }
                    exception.errorCode shouldBe ErrorCode.MIC_API_ERROR
                }
            }
        }
    })

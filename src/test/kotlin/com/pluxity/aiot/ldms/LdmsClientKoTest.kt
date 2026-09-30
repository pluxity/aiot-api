package com.pluxity.aiot.ldms

import com.pluxity.aiot.global.config.RestClientFactory
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.LdmsProperties
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

private fun closedPort(): Int = ServerSocket(0).use { it.localPort }

private fun client(port: Int) =
    LdmsClient(
        RestClientFactory(),
        LdmsProperties(enabled = true, baseUrl = "http://127.0.0.1:$port", userId = "user", password = "pw"),
    )

private fun HttpExchange.respond(
    status: Int,
    body: String = "",
) {
    val bytes = body.toByteArray()
    responseHeaders.add("Content-Type", "application/json")
    sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
    if (bytes.isNotEmpty()) responseBody.use { it.write(bytes) }
    close()
}

/** 로그인할 때마다 새 세션을 발급하고, 마지막으로 발급한 세션만 유효하게 취급하는 가짜 LDMS */
private class FakeLdms {
    val loginCount = AtomicInteger()
    val listCookies = mutableListOf<String?>()

    @Volatile
    var validSession: String? = null

    val server: HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/api/auth/login") { exchange ->
                val session = "s${loginCount.incrementAndGet()}"
                validSession = session
                exchange.responseHeaders.add("Set-Cookie", "id=$session; HttpOnly; SameSite=Lax; Path=/; Max-Age=1800")
                exchange.respond(200, """{"user_id":"user","grp_id":"R105"}""")
            }
            createContext("/api/ebrd/list") { exchange ->
                val cookie = exchange.requestHeaders.getFirst("Cookie")
                listCookies += cookie
                if (cookie != "id=$validSession") {
                    exchange.respond(401)
                } else {
                    exchange.respond(200, """[{"ebrd_seq":1,"ebrd_id":"C16LD25004DC","ebrd_nm":"BNS 전광판","unknown":1}]""")
                }
            }
            start()
        }

    val port get() = server.address.port
}

class LdmsClientKoTest :
    BehaviorSpec({

        Given("세션이 없음") {
            val fake = FakeLdms()
            val ldmsClient = client(fake.port)

            When("전광판 목록을 두 번 조회함") {
                val first = ldmsClient.getEbrdList()
                ldmsClient.getEbrdList()

                Then("처음에만 로그인하고 받은 세션 쿠키를 계속 쓴다") {
                    first.map { it.ebrdId } shouldContainExactly listOf("C16LD25004DC")
                    fake.loginCount.get() shouldBe 1
                    fake.listCookies shouldContainExactly listOf("id=s1", "id=s1")
                }
            }
            fake.server.stop(0)
        }

        Given("세션이 만료됨") {
            val fake = FakeLdms()
            val ldmsClient = client(fake.port)
            ldmsClient.getEbrdList()
            fake.validSession = "expired"

            When("전광판 목록을 조회함") {
                val result = ldmsClient.getEbrdList()

                Then("401을 받으면 재로그인 후 한 번 다시 호출한다") {
                    result.size shouldBe 1
                    fake.loginCount.get() shouldBe 2
                    fake.listCookies shouldContainExactly listOf("id=s1", "id=s1", "id=s2")
                }
            }
            fake.server.stop(0)
        }

        Given("LDMS 서버가 응답하지 않음") {
            When("전광판 목록을 조회함") {
                Then("로그인 단계의 연결 거부를 로그인 실패로 변환한다") {
                    val exception = shouldThrowExactly<CustomException> { client(closedPort()).getEbrdList() }
                    exception.errorCode shouldBe ErrorCode.LDMS_LOGIN_FAILED
                }
            }
        }
    })

package com.pluxity.aiot.global.properties

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.convert.DurationStyle
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import java.time.Duration

/** 나머지 테스트는 test/resources/application.yml만 읽어, 복제본인 운영 파일은 여기서만 지켜진다. */
class JwtPropertiesKoTest :
    BehaviorSpec({
        val yaml =
            YamlPropertySourceLoader()
                .load("application-common", ClassPathResource("application-common.yml"))
                .first()

        fun rawExpirationOf(tokenType: String) = yaml.getProperty("jwt.$tokenType.expiration").toString()

        Given("운영 설정 파일 application-common.yml") {
            When("액세스 토큰 수명을 읽으면") {
                val raw = rawExpirationOf("access-token")

                Then("10시간이다") {
                    DurationStyle.detectAndParse(raw) shouldBe Duration.ofHours(10)
                }

                Then("단위를 명시한 표기다") {
                    raw shouldBe "10h"
                }
            }

            When("리프레시 토큰 수명을 읽으면") {
                val raw = rawExpirationOf("refresh-token")

                Then("10일이다") {
                    DurationStyle.detectAndParse(raw) shouldBe Duration.ofDays(10)
                }

                Then("단위를 명시한 표기다") {
                    raw shouldBe "10d"
                }
            }
        }
    })

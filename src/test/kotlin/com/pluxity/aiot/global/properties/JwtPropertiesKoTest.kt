package com.pluxity.aiot.global.properties

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.convert.DurationStyle
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import java.time.Duration

/**
 * 운영 설정의 토큰 수명을 값으로 고정한다.
 *
 * 나머지 테스트는 test/resources/application.yml만 읽는다. 두 파일은 복제 관계라
 * 한쪽만 고쳐도 테스트가 통과하므로, 운영 파일은 여기서만 지켜진다.
 *
 * Spring이 실제로 쓰는 DurationStyle로 파싱한다. 단위를 뺀 숫자는 ms로 해석되므로
 * 표기가 `10h`에서 `36000000`으로 되돌아가도 값이 같아 통과하지만, 그때는
 * 읽는 쪽이 초로 오해하기 쉬운 원래 형태로 돌아간 것이라 표기 자체를 아래에서 확인한다.
 */
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

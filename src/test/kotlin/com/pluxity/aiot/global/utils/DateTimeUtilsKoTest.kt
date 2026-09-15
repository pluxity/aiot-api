package com.pluxity.aiot.global.utils

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

/** Mobius의 Timestamp·ct는 UTC다. 이벤트 이력은 KST로 보여야 한다. */
class DateTimeUtilsKoTest :
    BehaviorSpec({
        Given("Mobius가 보낸 UTC Timestamp") {
            When("KST LocalDateTime으로 파싱하면") {
                val result = DateTimeUtils.parseUtcToKst("20260915T040324")

                Then("9시간 더한 값이다") {
                    result shouldBe LocalDateTime.of(2026, 9, 15, 13, 3, 24)
                }
            }

            When("자정 전 UTC를 파싱하면") {
                val result = DateTimeUtils.parseUtcToKst("20260915T160000")

                Then("다음 날 KST가 된다") {
                    result shouldBe LocalDateTime.of(2026, 9, 16, 1, 0, 0)
                }
            }
        }
    })

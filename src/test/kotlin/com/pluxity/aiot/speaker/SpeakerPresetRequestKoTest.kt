package com.pluxity.aiot.speaker

import com.pluxity.aiot.speaker.dto.SpeakerPresetRequest
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation

class SpeakerPresetRequestKoTest :
    BehaviorSpec({
        val validator = Validation.buildDefaultValidatorFactory().validator

        fun violatedFields(repeatCount: Int) =
            validator
                .validate(SpeakerPresetRequest("제목", "메시지", repeatCount))
                .map { it.propertyPath.toString() }

        Given("반복 횟수") {
            When("생략함") {
                Then("1회로 들어간다") {
                    SpeakerPresetRequest("제목", "메시지").repeatCount shouldBe 1
                }
            }

            When("1~10 범위 안") {
                Then("통과한다") {
                    violatedFields(1).shouldBeEmpty()
                    violatedFields(10).shouldBeEmpty()
                }
            }

            When("범위를 벗어남") {
                Then("검증에 걸린다") {
                    violatedFields(0) shouldContainExactly listOf("repeatCount")
                    violatedFields(11) shouldContainExactly listOf("repeatCount")
                }
            }
        }
    })

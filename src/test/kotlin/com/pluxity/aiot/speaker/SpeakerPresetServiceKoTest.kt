package com.pluxity.aiot.speaker

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.speaker.dto.SpeakerPresetRequest
import com.pluxity.aiot.speaker.dto.SpeakerPresetSearchRequest
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class SpeakerPresetServiceKoTest(
    private val speakerPresetService: SpeakerPresetService,
    private val speakerPresetRepository: SpeakerPresetRepository,
) : BehaviorSpec({
        extension(SpringExtension(SpringTestLifecycleMode.Root))

        afterEach { speakerPresetRepository.deleteAll() }

        Given("프리셋을 저장함") {
            When("저장 후 조회함") {
                speakerPresetRepository.deleteAll()
                val id = speakerPresetService.save(SpeakerPresetRequest("폐장 안내", "잠시 후 폐장합니다.", repeatCount = 3))
                val found = speakerPresetService.findById(id)

                Then("저장한 값을 그대로 돌려준다") {
                    found.title shouldBe "폐장 안내"
                    found.message shouldBe "잠시 후 폐장합니다."
                    found.repeatCount shouldBe 3
                }
            }
        }

        Given("프리셋 여러 개") {
            When("제목으로 검색함") {
                speakerPresetRepository.deleteAll()
                listOf("폐장 안내", "개장 안내", "폐장 10분 전").forEach {
                    speakerPresetService.save(SpeakerPresetRequest(it, "메시지"))
                }
                val searched = speakerPresetService.findAll(SpeakerPresetSearchRequest(title = "폐장"))
                val all = speakerPresetService.findAll(SpeakerPresetSearchRequest(title = " "))

                Then("검색어가 들어간 것만 최신순으로 나오고, 빈 검색어는 전체를 준다") {
                    searched.content.map { it.title } shouldContainExactly listOf("폐장 10분 전", "폐장 안내")
                    all.totalElements shouldBe 3
                }
            }
        }

        Given("저장된 프리셋") {
            When("수정함") {
                speakerPresetRepository.deleteAll()
                val id = speakerPresetService.save(SpeakerPresetRequest("제목", "메시지"))
                speakerPresetService.update(id, SpeakerPresetRequest("새 제목", "새 메시지", repeatCount = 2))

                Then("모든 값이 바뀐다") {
                    val found = speakerPresetService.findById(id)
                    found.title shouldBe "새 제목"
                    found.message shouldBe "새 메시지"
                    found.repeatCount shouldBe 2
                }
            }

            When("삭제함") {
                speakerPresetRepository.deleteAll()
                val id = speakerPresetService.save(SpeakerPresetRequest("제목", "메시지"))
                speakerPresetService.delete(id)

                Then("다시 조회하면 NOT_FOUND_SPEAKER_PRESET") {
                    shouldThrowExactly<CustomException> { speakerPresetService.findById(id) }
                        .errorCode shouldBe ErrorCode.NOT_FOUND_SPEAKER_PRESET
                }
            }
        }

        Given("없는 프리셋") {
            When("수정·삭제함") {
                Then("NOT_FOUND_SPEAKER_PRESET") {
                    shouldThrowExactly<CustomException> { speakerPresetService.update(999L, SpeakerPresetRequest("t", "m")) }
                        .errorCode shouldBe ErrorCode.NOT_FOUND_SPEAKER_PRESET
                    shouldThrowExactly<CustomException> { speakerPresetService.delete(999L) }
                        .errorCode shouldBe ErrorCode.NOT_FOUND_SPEAKER_PRESET
                }
            }
        }
    })

package com.pluxity.aiot.mic

import com.pluxity.aiot.mic.dto.MicEventData
import com.pluxity.aiot.mic.dto.MicEventLabel
import com.pluxity.aiot.mic.dto.MicEventLabelName
import com.pluxity.aiot.mic.dto.MicEventSource
import com.pluxity.aiot.mic.dto.MicLocation
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

private fun eventData(
    id: String = "23d6ec74155f4eb187b4e7301b71b5d9",
    micId: String? = "d00c8e3364a34439183e3462e773354a1",
    createdAt: String? = "2024-01-01T00:00:00Z",
    noises: List<Double>? = listOf(66.99, 65.28, 68.21),
) = MicEventData(
    id = id,
    label =
        MicEventLabel(
            id = "normal_speech_female",
            name = MicEventLabelName(ko = "대화(여성)", en = "normal speech female"),
        ),
    confidence = 0.426632,
    mic =
        micId?.let {
            MicEventSource(
                id = it,
                name = "Example Mic (715)",
                location = MicLocation(37.546344, 126.944322),
            )
        },
    createdAt = createdAt,
    noises = noises,
)

class MicEventServiceKoTest :
    BehaviorSpec({

        Given("AI 마이크 이벤트 저장") {
            When("정상 이벤트가 수신됨") {
                val repository: MicEventRepository = mockk(relaxed = true)
                every { repository.existsByEventId(any()) } returns false
                val saved = slot<MicEvent>()
                every { repository.save(capture(saved)) } answers { saved.captured }

                MicEventService(repository).saveEvent(eventData())

                Then("라벨·신뢰도·좌표가 저장된다") {
                    saved.captured.eventId shouldBe "23d6ec74155f4eb187b4e7301b71b5d9"
                    saved.captured.micId shouldBe "d00c8e3364a34439183e3462e773354a1"
                    saved.captured.micName shouldBe "Example Mic (715)"
                    saved.captured.labelId shouldBe "normal_speech_female"
                    saved.captured.labelNameKo shouldBe "대화(여성)"
                    saved.captured.labelNameEn shouldBe "normal speech female"
                    saved.captured.confidence shouldBe 0.426632
                    saved.captured.latitude shouldBe 37.546344
                    saved.captured.longitude shouldBe 126.944322
                }

                Then("noises 원본과 최대·평균 요약이 함께 저장된다") {
                    saved.captured.noises shouldBe listOf(66.99, 65.28, 68.21)
                    saved.captured.maxNoise shouldBe 68.21
                    saved.captured.avgNoise!! shouldBe (66.826 plusOrMinus 0.001)
                }

                Then("created_at이 KST로 변환되어 저장된다") {
                    saved.captured.occurredAt shouldBe LocalDateTime.of(2024, 1, 1, 9, 0, 0)
                }
            }

            When("created_at이 없는 이벤트가 수신됨") {
                val repository: MicEventRepository = mockk(relaxed = true)
                every { repository.existsByEventId(any()) } returns false
                val saved = slot<MicEvent>()
                every { repository.save(capture(saved)) } answers { saved.captured }

                val before = LocalDateTime.now()
                MicEventService(repository).saveEvent(eventData(createdAt = null))

                Then("수신 시각으로 대체된다") {
                    (saved.captured.occurredAt >= before) shouldBe true
                }
            }

            When("created_at 형식이 잘못된 이벤트가 수신됨") {
                val repository: MicEventRepository = mockk(relaxed = true)
                every { repository.existsByEventId(any()) } returns false
                val saved = slot<MicEvent>()
                every { repository.save(capture(saved)) } answers { saved.captured }

                val before = LocalDateTime.now()
                MicEventService(repository).saveEvent(eventData(createdAt = "not-a-date"))

                Then("예외 없이 수신 시각으로 대체된다") {
                    (saved.captured.occurredAt >= before) shouldBe true
                }
            }

            When("이미 저장된 이벤트 id가 다시 수신됨") {
                val repository: MicEventRepository = mockk(relaxed = true)
                every { repository.existsByEventId(any()) } returns true

                MicEventService(repository).saveEvent(eventData())

                Then("중복 저장하지 않는다") {
                    verify(exactly = 0) { repository.save(any()) }
                }
            }

            When("마이크 정보가 없는 이벤트가 수신됨") {
                val repository: MicEventRepository = mockk(relaxed = true)

                MicEventService(repository).saveEvent(eventData(micId = null))

                Then("저장하지 않고 무시한다") {
                    verify(exactly = 0) { repository.save(any()) }
                    verify(exactly = 0) { repository.existsByEventId(any()) }
                }
            }

            When("noises가 비어 있는 이벤트가 수신됨") {
                val repository: MicEventRepository = mockk(relaxed = true)
                every { repository.existsByEventId(any()) } returns false
                val saved = slot<MicEvent>()
                every { repository.save(capture(saved)) } answers { saved.captured }

                MicEventService(repository).saveEvent(eventData(noises = emptyList()))

                Then("평균 계산에서 NaN이 나오지 않는다") {
                    saved.captured.maxNoise shouldBe null
                    saved.captured.avgNoise shouldBe null
                }
            }
        }
    })

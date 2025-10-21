package com.pluxity.aiot.cctv

import com.pluxity.aiot.cctv.dto.CctvRequest
import com.pluxity.aiot.cctv.entity.dummyCctv
import com.pluxity.aiot.cctv.repository.CctvRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.MediaMtxProperties
import com.pluxity.aiot.site.SiteRepository
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class CctvServiceTest :
    BehaviorSpec({
        val cctvRepository: CctvRepository = mockk()
        val siteRepository: SiteRepository = mockk(relaxed = true)
        val mediaMtxService: MediaMtxService = mockk(relaxed = true)
        val mediaMtxProperties: MediaMtxProperties = mockk(relaxed = true)

        val cctvService =
            CctvService(
                cctvRepository,
                siteRepository,
                mediaMtxService,
                mediaMtxProperties,
            )
        Given("CCTV 생성을 진행할 때") {
            When("유효한 요청으로 CCTV 생성 요청") {
                val id = 1L
                val createRequest = CctvRequest("cctv-name", "url", 127.1, 37.1, 5.0)

                every {
                    cctvRepository.save(any())
                } returns dummyCctv(id = id)
                Then("성공") {
                    val saveId = cctvService.create(createRequest)
                    saveId shouldBe id
                }
            }
        }

        Given("CCTV 목록 조회를 진행할 때") {
            When("정상 요청이 오면") {
                every {
                    cctvRepository.findAllBySiteId(any())
                } returns mutableListOf(dummyCctv())

                Then("정상 조회") {
                    cctvService.findAll().size shouldBe 1
                }
            }
        }

        Given("CCTV 상세 조회를 진행할 때") {
            When("유효한 아이디로 조회 요청") {
                val cctv = dummyCctv()
                every {
                    cctvRepository.findByIdOrNull(any())
                } returns cctv
                Then("정상 조회") {
                    val res = cctvService.findById(cctv.id!!)
                    res.id shouldBe cctv.id
                    res.name shouldBe cctv.name
                }
            }

            When("없는 아이디로 조회 요청") {
                every {
                    cctvRepository.findByIdOrNull(any())
                } returns null
                Then("NOT_FOUND_CCTV 예외 발생") {
                    val searchId = 1L
                    shouldThrowExactly<CustomException> {
                        cctvService.findById(searchId)
                    }.message shouldBe ErrorCode.NOT_FOUND_CCTV.getMessage().format(searchId)
                }
            }
        }

        Given("CCTV 수정을 진행할 때") {
            When("정상 수정 요청") {
                val cctv = dummyCctv()
                every {
                    cctvRepository.findByIdOrNull(any())
                } returns cctv
                Then("정상 수정") {
                    val updateName = "updated Cctv"
                    cctvService.update(cctv.id!!, CctvRequest(updateName, "", 0.0, 0.0, 5.0))
                    cctv.name shouldBe updateName
                }
            }
        }

        Given("CCTV 삭제를 진행할 때") {
            When("정상 삭제 요청") {
                val cctv = dummyCctv()
                val slot = slot<Long>()
                every {
                    cctvRepository.findByIdOrNull(any())
                } returns cctv
                every {
                    cctvRepository.deleteById(capture(slot))
                } just runs
                Then("정상 삭제") {
                    cctvService.delete(cctv.id!!)
                    verify(exactly = 1) { cctvRepository.deleteById(any()) }
                    slot.captured shouldBe cctv.id
                }
            }
        }
    })

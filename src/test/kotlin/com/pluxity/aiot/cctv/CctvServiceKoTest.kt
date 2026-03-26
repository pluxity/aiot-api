package com.pluxity.aiot.cctv

import com.pluxity.aiot.cctv.entity.dummyCctv
import com.pluxity.aiot.cctv.repository.CctvRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.repository.findByIdOrNull

class CctvServiceKoTest :
    BehaviorSpec({
        val cctvRepository: CctvRepository = mockk()

        val cctvService = CctvService(cctvRepository)

        Given("CCTV 목록 조회를 진행할 때") {
            When("정상 요청이 오면") {
                every {
                    cctvRepository.findAllBySiteId(any())
                } returns
                    mutableListOf(
                        dummyCctv(),
                    )

                val result = cctvService.findAll()

                Then("정상 조회") {
                    result.size shouldBe 1
                    result.first().edsCameraId shouldBe "CAM-000001"
                }
            }
        }

        Given("CCTV 상세 조회를 진행할 때") {
            When("유효한 아이디로 조회 요청") {
                val cctv = dummyCctv()
                every {
                    cctvRepository.findByIdOrNull(any())
                } returns cctv
                val res = cctvService.getById(cctv.requiredId)
                Then("정상 조회") {
                    res.id shouldBe cctv.id
                    res.name shouldBe cctv.name
                    res.edsCameraId shouldBe cctv.edsCameraId
                }
            }

            When("없는 아이디로 조회 요청") {
                every {
                    cctvRepository.findByIdOrNull(any())
                } returns null
                val searchId = 1L
                val exception =
                    shouldThrowExactly<CustomException> {
                        cctvService.findById(searchId)
                    }
                Then("NOT_FOUND_CCTV 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_CCTV.getMessage().format(searchId)
                }
            }
        }
    })

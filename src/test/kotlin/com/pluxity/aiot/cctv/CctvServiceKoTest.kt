package com.pluxity.aiot.cctv

import com.pluxity.aiot.cctv.dto.CctvRequest
import com.pluxity.aiot.cctv.dto.MediaMtxPathResponse
import com.pluxity.aiot.cctv.entity.dummyCctv
import com.pluxity.aiot.cctv.repository.CctvRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.MediaMtxProperties
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.site.entity.dummySite
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class CctvServiceKoTest :
    BehaviorSpec({
        val cctvRepository: CctvRepository = mockk()
        val siteRepository: SiteRepository = mockk(relaxed = true)
        val mediaMtxService: MediaMtxService = mockk()
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
                every { siteRepository.findFirstByPointInPolygon(127.1, 37.1) } returns dummySite(id = 1L)
                every { mediaMtxService.addPath(any(), any()) } just runs
                val saveId = cctvService.create(createRequest)
                Then("성공") {
                    saveId shouldBe id
                }
            }

            When("위치와 URL이 없는 요청이면") {
                val id = 2L
                val createRequest = CctvRequest("cctv-no-url", null, null, null, null)
                val cctvSlot = slot<Cctv>()

                every { cctvRepository.save(capture(cctvSlot)) } returns dummyCctv(id = id)

                val saveId = cctvService.create(createRequest)

                Then("미디어 등록과 위치 조회 없이 저장된다") {
                    saveId shouldBe id
                    cctvSlot.captured.mtxName shouldBe null
                    verify(exactly = 0) { mediaMtxService.addPath(any(), any()) }
                    verify(exactly = 0) { siteRepository.findFirstByPointInPolygon(any(), any()) }
                }
            }
        }

        Given("CCTV 목록 조회를 진행할 때") {
            When("정상 요청이 오면") {
                every { mediaMtxProperties.viewUrl } returns "http://view"
                every {
                    cctvRepository.findAllBySiteId(any())
                } returns
                    mutableListOf(
                        dummyCctv(mtxName = "mtx-1"),
                    )

                val result = cctvService.findAll()

                Then("정상 조회") {
                    result.size shouldBe 1
                    result.first().viewUrl shouldBe "http://view/mtx-1"
                }
            }
        }

        Given("CCTV 상세 조회를 진행할 때") {
            When("유효한 아이디로 조회 요청") {
                val cctv = dummyCctv(mtxName = "mtx-detail")
                every { mediaMtxProperties.viewUrl } returns "http://view"
                every {
                    cctvRepository.findByIdOrNull(any())
                } returns cctv
                val res = cctvService.getById(cctv.requiredId)
                Then("정상 조회") {
                    res.id shouldBe cctv.id
                    res.name shouldBe cctv.name
                    res.viewUrl shouldBe "http://view/mtx-detail"
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

        Given("CCTV 수정을 진행할 때") {
            When("정상 수정 요청") {
                val cctv = dummyCctv()
                every {
                    cctvRepository.findByIdOrNull(any())
                } returns cctv
                every { mediaMtxService.addPath(any(), any()) } just runs
                val updateName = "updated Cctv"
                cctvService.update(cctv.requiredId, CctvRequest(updateName, "", 0.0, 0.0, 5.0))
                Then("정상 수정") {
                    cctv.name shouldBe updateName
                }
            }

            When("URL 변경과 위치 변경이 함께 들어오면") {
                val cctv =
                    dummyCctv(url = "old-url", mtxName = "old-mtx")
                every { cctvRepository.findByIdOrNull(any()) } returns cctv
                every { mediaMtxService.deletePath(any()) } just runs
                every { mediaMtxService.addPath(any(), "new-url") } just runs
                every { siteRepository.findFirstByPointInPolygon(127.5, 37.5) } returns dummySite(id = 2L)

                cctvService.update(
                    cctv.requiredId,
                    CctvRequest("new-name", "new-url", 127.5, 37.5, 10.0),
                )

                Then("미디어 경로 갱신과 위치가 반영된다") {
                    verify { mediaMtxService.deletePath(any()) }
                    verify { mediaMtxService.addPath(any(), "new-url") }
                    cctv.url shouldBe "new-url"
                    cctv.name shouldBe "new-name"
                    cctv.longitude shouldBe 127.5
                    cctv.latitude shouldBe 37.5
                    cctv.site?.id shouldBe 2L
                    cctv.mtxName shouldNotBe "old-mtx"
                }
            }

            When("URL이 동일하면 미디어 경로를 변경하지 않는다") {
                val cctv =
                    dummyCctv(
                        url = "same-url",
                        mtxName = "same-mtx",
                        site = dummySite(id = 1L),
                    )
                every { cctvRepository.findByIdOrNull(any()) } returns cctv

                cctvService.update(
                    cctv.requiredId,
                    CctvRequest("same-name", "same-url", null, null, 3.0),
                )

                Then("미디어 경로 호출 없이 위치가 초기화된다") {
                    verify(exactly = 0) { mediaMtxService.deletePath(any()) }
                    verify(exactly = 0) { mediaMtxService.addPath(any(), any()) }
                    cctv.mtxName shouldBe "same-mtx"
                    cctv.longitude shouldBe null
                    cctv.latitude shouldBe null
                    cctv.site shouldBe null
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
                cctvService.delete(cctv.requiredId)
                Then("정상 삭제") {
                    verify(exactly = 1) { cctvRepository.deleteById(any()) }
                    slot.captured shouldBe cctv.id
                }
            }

            When("미디어 경로가 있는 CCTV 삭제 요청") {
                val cctv = dummyCctv(mtxName = "mtx-delete")
                every { cctvRepository.findByIdOrNull(any()) } returns cctv
                every { cctvRepository.deleteById(any()) } just runs
                every { mediaMtxService.deletePath("mtx-delete") } just runs

                cctvService.delete(cctv.requiredId)

                Then("미디어 경로 삭제 후 DB 삭제가 수행된다") {
                    verify { mediaMtxService.deletePath("mtx-delete") }
                    verify { cctvRepository.deleteById(cctv.requiredId) }
                }
            }
        }

        Given("CCTV 동기화를 진행할 때") {
            When("미디어 서버와 DB의 차이를 맞춘다") {
                val cctvList =
                    listOf(
                        dummyCctv(
                            mtxName = "mtx-2",
                            url = "url-2",
                        ),
                        dummyCctv(
                            mtxName = "mtx-3",
                            url = "url-3",
                        ),
                    )
                every { mediaMtxService.getAllPath() } returns
                    listOf(
                        MediaMtxPathResponse(name = "mtx-1", source = "src-1"),
                        MediaMtxPathResponse(name = "mtx-2", source = "src-2"),
                    )
                every { cctvRepository.findAll() } returns cctvList
                every { mediaMtxService.deletePath("mtx-1") } just runs
                every { mediaMtxService.addPath("mtx-3", "url-3") } just runs

                cctvService.synchronizeCctv()

                Then("삭제 및 추가가 호출된다") {
                    verify { mediaMtxService.deletePath("mtx-1") }
                    verify { mediaMtxService.addPath("mtx-3", "url-3") }
                }
            }
        }
    })

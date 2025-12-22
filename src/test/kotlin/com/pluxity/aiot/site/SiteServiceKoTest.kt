package com.pluxity.aiot.site

import com.pluxity.aiot.file.dto.FileResponse
import com.pluxity.aiot.file.service.FileService
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.site.dto.SiteRequest
import com.pluxity.aiot.site.entity.dummySite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class SiteServiceKoTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        val siteRepository: SiteRepository = mockk()
        val fileService: FileService = mockk()
        val siteService =
            SiteService(
                siteRepository,
                fileService,
            )

        Given("Site 생성 기능") {
            When("유효한 Polygon으로 현장을 생성하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request = SiteRequest("Test Site", validPolygon, "Test Description")
                val siteSlot = slot<Site>()

                every { siteRepository.save(capture(siteSlot)) } returns dummySite(id = 1L)

                val createdId = siteService.save(request)

                Then("성공적으로 생성되고 ID가 반환된다") {
                    createdId shouldBe 1L
                    siteSlot.captured.name shouldBe "Test Site"
                    siteSlot.captured.description shouldBe "Test Description"
                    siteSlot.captured.location shouldNotBe null
                }
            }

            When("thumbnailId가 포함된 요청으로 현장을 생성하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request = SiteRequest("Test Site", validPolygon, "Test Description", thumbnailId = 10L)
                val siteSlot = slot<Site>()

                every { siteRepository.save(capture(siteSlot)) } returns dummySite(id = 1L)
                every { fileService.finalizeUpload(10L, "sites/1/") } returns mockk()

                val createdId = siteService.save(request)

                Then("썸네일 파일이 영구 저장되고 ID가 반환된다") {
                    createdId shouldBe 1L
                    verify(exactly = 1) { fileService.finalizeUpload(10L, "sites/1/") }
                }
            }

            When("잘못된 WKT(Polygon이 아님)로 현장을 생성하면") {
                val invalidPolygon = "POINT(127.0 37.0)"
                val request = SiteRequest("Test Site", invalidPolygon, "Test Description")

                val exception =
                    shouldThrow<CustomException> {
                        siteService.save(request)
                    }

                Then("INVALID_LOCATION 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.INVALID_LOCATION
                    verify(exactly = 0) { siteRepository.save(any()) }
                }
            }
        }

        Given("Site 조회 기능") {
            When("전체 현장을 조회하면") {
                val site1 = dummySite(id = 1L, name = "Site 1", description = "Desc 1")
                val site2 = dummySite(id = 2L, name = "Site 2", description = "Desc 2")

                every { siteRepository.findAllByOrderByCreatedAtDesc() } returns listOf(site2, site1)

                val result = siteService.findAll()

                Then("생성일 내림차순으로 모든 현장이 반환된다") {
                    result shouldHaveSize 2
                    result[0].name shouldBe "Site 2"
                    result[1].name shouldBe "Site 1"
                }
            }

            When("썸네일이 있는 현장을 전체 조회하면") {
                val site1 =
                    dummySite(id = 1L, name = "Site 1", description = "Desc 1").apply {
                        updateThumbnailId(10L)
                    }
                val site2 = dummySite(id = 2L, name = "Site 2", description = "Desc 2")

                every { siteRepository.findAllByOrderByCreatedAtDesc() } returns listOf(site2, site1)
                every { fileService.getFiles(listOf(10L)) } returns listOf(FileResponse(id = 10L))

                val result = siteService.findAll()

                Then("썸네일이 매핑되어 반환된다") {
                    result[0].thumbnail shouldBe null
                    result[1].thumbnail?.id shouldBe 10L
                    verify(exactly = 1) { fileService.getFiles(listOf(10L)) }
                }
            }

            When("존재하는 ID로 현장을 조회하면") {
                val site = dummySite(id = 1L, name = "Test Site", description = "Test Description")

                every { siteRepository.findByIdOrNull(1L) } returns site

                val result = siteService.findByIdResponse(1L)

                Then("해당 현장이 반환된다") {
                    result.name shouldBe "Test Site"
                    result.description shouldBe "Test Description"
                }
            }

            When("썸네일이 있는 현장을 ID로 조회하면") {
                val site =
                    dummySite(id = 1L, name = "Test Site", description = "Test Description").apply {
                        updateThumbnailId(99L)
                    }

                every { siteRepository.findByIdOrNull(1L) } returns site
                every { fileService.getFiles(listOf(99L)) } returns listOf(FileResponse(id = 99L))

                val result = siteService.findByIdResponse(1L)

                Then("썸네일이 매핑되어 반환된다") {
                    result.thumbnail?.id shouldBe 99L
                    verify(exactly = 1) { fileService.getFiles(listOf(99L)) }
                }
            }

            When("존재하지 않는 ID로 현장을 조회하면") {
                every { siteRepository.findByIdOrNull(any()) } returns null

                val exception =
                    shouldThrow<CustomException> {
                        siteService.findByIdResponse(999999L)
                    }

                Then("NOT_FOUND_SITE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_SITE
                }
            }
        }

        Given("Site 업데이트 기능") {
            When("유효한 데이터로 현장을 업데이트하면") {
                val validPolygon1 = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val validPolygon2 = "POLYGON((128.0 38.0, 128.1 38.0, 128.1 38.1, 128.0 38.1, 128.0 38.0))"
                val request1 = SiteRequest("Original Site", validPolygon1, "Original Description")
                val updateRequest = SiteRequest("Updated Site", validPolygon2, "Updated Description")
                val site = dummySite(id = 1L, name = request1.name, description = request1.description)

                every { siteRepository.findByIdOrNull(1L) } returns site

                siteService.putUpdate(1L, updateRequest)

                Then("성공적으로 업데이트된다") {
                    site.name shouldBe "Updated Site"
                    site.description shouldBe "Updated Description"
                }
            }

            When("빈 name으로 현장을 업데이트하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request = SiteRequest("Original Site", validPolygon, "Original Description")
                val updateRequest = SiteRequest("", validPolygon, "Updated Description")
                val site = dummySite(id = 1L, name = request.name, description = request.description)

                every { siteRepository.findByIdOrNull(1L) } returns site

                Then("IllegalArgumentException이 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        siteService.putUpdate(1L, updateRequest)
                    }
                }
            }

            When("thumbnailId를 변경하여 현장을 업데이트하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val updateRequest = SiteRequest("Updated Site", validPolygon, "Updated Description", thumbnailId = 20L)
                val site =
                    dummySite(id = 1L, name = "Original Site", description = "Original Description").apply {
                        updateThumbnailId(10L)
                    }

                every { siteRepository.findByIdOrNull(1L) } returns site
                every { fileService.finalizeUpload(20L, "sites/1/") } returns mockk()

                siteService.putUpdate(1L, updateRequest)

                Then("썸네일이 교체되고 finalizeUpload가 호출된다") {
                    site.thumbnailId shouldBe 20L
                    verify(exactly = 1) { fileService.finalizeUpload(20L, "sites/1/") }
                }
            }

            When("thumbnailId를 제거하여 현장을 업데이트하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val updateRequest = SiteRequest("Updated Site", validPolygon, "Updated Description", thumbnailId = null)
                val site =
                    dummySite(id = 1L, name = "Original Site", description = "Original Description").apply {
                        updateThumbnailId(10L)
                    }

                every { siteRepository.findByIdOrNull(1L) } returns site

                siteService.putUpdate(1L, updateRequest)

                Then("썸네일이 제거된다") {
                    site.thumbnailId shouldBe null
                    verify(exactly = 0) { fileService.finalizeUpload(any(), any()) }
                }
            }
        }

        Given("Site 삭제 기능") {
            When("존재하는 현장을 삭제하면") {
                val site = dummySite(id = 1L, name = "Test Site", description = "Test Description")

                every { siteRepository.findByIdOrNull(1L) } returns site
                every { siteRepository.delete(any()) } just runs

                siteService.delete(1L)

                Then("성공적으로 삭제된다") {
                    verify(exactly = 1) { siteRepository.delete(site) }
                }
            }

            When("존재하지 않는 현장을 삭제하면") {
                every { siteRepository.findByIdOrNull(any()) } returns null

                val exception =
                    shouldThrow<CustomException> {
                        siteService.delete(999999L)
                    }

                Then("NOT_FOUND_SITE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_SITE
                    exception.message shouldBe ErrorCode.NOT_FOUND_SITE.getMessage().format(999999L)
                }
            }
        }

        Given("Site 다건 조회 기능") {
            When("모든 ID가 존재하면") {
                val site1 = dummySite(id = 1L, name = "Site 1", description = "Desc 1")
                val site2 = dummySite(id = 2L, name = "Site 2", description = "Desc 2")

                every { siteRepository.findAllById(listOf(1L, 2L)) } returns listOf(site1, site2)

                val result = siteService.findByIds(listOf(1L, 2L))

                Then("요청한 현장 리스트가 반환된다") {
                    result shouldHaveSize 2
                }
            }

            When("존재하지 않는 ID가 포함되면") {
                val site1 = dummySite(id = 1L, name = "Site 1", description = "Desc 1")

                every { siteRepository.findAllById(listOf(1L, 999L)) } returns listOf(site1)

                val exception =
                    shouldThrow<CustomException> {
                        siteService.findByIds(listOf(1L, 999L))
                    }

                Then("NOT_FOUND_SITE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_SITE
                    exception.message shouldBe ErrorCode.NOT_FOUND_SITE.getMessage().format(listOf(999L))
                }
            }
        }
    })

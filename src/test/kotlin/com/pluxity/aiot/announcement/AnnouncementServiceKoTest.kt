package com.pluxity.aiot.announcement

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.JpqlQueryable
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.pluxity.aiot.announcement.dto.BroadcastRequest
import com.pluxity.aiot.announcement.dto.SearchRequest
import com.pluxity.aiot.announcement.entity.dummyAnnouncement
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.site.SiteService
import com.pluxity.aiot.site.entity.dummySite
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AnnouncementServiceKoTest :
    BehaviorSpec({

        val announcementRepository: AnnouncementRepository = mockk()
        val siteService: SiteService = mockk()
        val announcementService = AnnouncementService(announcementRepository, siteService)

        Given("안내방송 송출 요청할때") {

            When("유효한 요청") {

                Then("여러 현장에 안내방송 송출") {
                    // given
                    val siteIds = listOf(1L, 2L, 3L)
                    val message = "테스트 공지사항입니다."
                    val request = BroadcastRequest(message = message, siteIds = siteIds)

                    val sites =
                        listOf(
                            dummySite(1L, "현장1"),
                            dummySite(2L, "현장2"),
                            dummySite(3L, "현장3"),
                        )

                    every { siteService.findByIds(siteIds) } returns sites

                    val savedAnnouncementsSlot = slot<List<Announcement>>()
                    every { announcementRepository.saveAll(capture(savedAnnouncementsSlot)) } returns emptyList()

                    // when
                    announcementService.broadcast(request)

                    // then
                    verify { siteService.findByIds(siteIds) }
                    verify { announcementRepository.saveAll(any<List<Announcement>>()) }

                    val capturedAnnouncements = savedAnnouncementsSlot.captured
                    capturedAnnouncements.size shouldBe 3
                    capturedAnnouncements.forEach { announcement ->
                        announcement.message shouldBe message
                        announcement.site shouldNotBe null
                    }
                }

                then("단일 현장에 안내방송 송출") {
                    // given
                    val siteIds = listOf(1L)
                    val message = "단일 현장 테스트 공지사항"
                    val request = BroadcastRequest(message = message, siteIds = siteIds)

                    val sites = listOf(dummySite(1L, "현장1"))

                    every { siteService.findByIds(siteIds) } returns sites

                    val savedAnnouncementsSlot = slot<List<Announcement>>()
                    every { announcementRepository.saveAll(capture(savedAnnouncementsSlot)) } returns emptyList()

                    // when
                    announcementService.broadcast(request)

                    // then
                    val capturedAnnouncements = savedAnnouncementsSlot.captured
                    capturedAnnouncements.size shouldBe 1
                    capturedAnnouncements[0].message shouldBe message
                    capturedAnnouncements[0].site.id shouldBe 1L
                }
            }

            When("잘못된 현장 아이디 요청") {
                val siteIds = listOf(1L)
                every {
                    siteService.findByIds(any())
                } throws CustomException(ErrorCode.NOT_FOUND_SITE, siteIds[0])

                Then("NOT_FOUND_SITE 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        siteService.findByIds(siteIds)
                    }.message shouldBe ErrorCode.NOT_FOUND_SITE.getMessage().format(siteIds[0])
                }
            }
        }
        Given("안내방송 이력을 조회할때") {
            `when`("필터없이 조회") {
                val searchRequest = SearchRequest()
                val mockAnnouncements =
                    listOf(
                        dummyAnnouncement(id = 1L, message = "공지사항1"),
                        dummyAnnouncement(id = 2L, message = "공지사항2"),
                    )
                then("기본 조건 목록 반환") {
                    val mockPage: Page<Announcement?> = PageImpl(mockAnnouncements, PageRequest.of(0, 10), 2)

                    every {
                        announcementRepository.findPage(any<Pageable>(), any<Jpql.() -> JpqlQueryable<SelectQuery<Announcement>>>())
                    } returns mockPage

                    // when
                    val result = announcementService.findAll(searchRequest)

                    // then
                    result.content.size shouldBe 2
                    result.totalElements shouldBe 2L
                    result.pageNumber shouldBe 1
                    result.pageSize shouldBe 10
                }

                then("날짜 범위로 조회") {
                    val searchRequest =
                        SearchRequest(
                            page = 1,
                            size = 10,
                            from = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                            to = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                        )
                    val mockAnnouncements =
                        listOf(
                            dummyAnnouncement(id = 1L, message = "공지사항1"),
                        )
                    val mockPage: Page<Announcement?> = PageImpl(mockAnnouncements, PageRequest.of(0, 10), 1)

                    every {
                        announcementRepository.findPage(any<Pageable>(), any<Jpql.() -> JpqlQueryable<SelectQuery<Announcement>>>())
                    } returns mockPage

                    // when
                    val result = announcementService.findAll(searchRequest)

                    // then
                    result.content.size shouldBe 1
                    result.totalElements shouldBe 1L
                }

                then("현장 ID로 공지사항을 필터링해야 한다") {
                    // given
                    val searchRequest = SearchRequest(page = 1, size = 10, siteId = 1L)
                    val mockAnnouncements =
                        listOf(
                            dummyAnnouncement(1L, "현장1 공지사항", siteId = 1L),
                        )
                    val mockPage: Page<Announcement?> = PageImpl(mockAnnouncements, PageRequest.of(0, 10), 1)

                    every {
                        announcementRepository.findPage(any<Pageable>(), any<Jpql.() -> JpqlQueryable<SelectQuery<Announcement>>>())
                    } returns mockPage

                    // when
                    val result = announcementService.findAll(searchRequest)

                    // then
                    result.content.size shouldBe 1
                    result.totalElements shouldBe 1L
                }

                then("사용자 ID로 공지사항을 필터링해야 한다") {
                    // given
                    val searchRequest = SearchRequest(page = 1, size = 10, userId = "admin")
                    val mockAnnouncements = listOf(dummyAnnouncement(1L, "관리자 공지사항"))
                    val mockPage: Page<Announcement?> = PageImpl(mockAnnouncements, PageRequest.of(0, 10), 1)

                    every {
                        announcementRepository.findPage(any<Pageable>(), any<Jpql.() -> JpqlQueryable<SelectQuery<Announcement>>>())
                    } returns mockPage

                    // when
                    val result = announcementService.findAll(searchRequest)

                    // then
                    result.content.size shouldBe 1
                    result.totalElements shouldBe 1L
                }

                then("페이징 정보가 올바르게 설정되어야 한다") {
                    // given
                    val searchRequest = SearchRequest(page = 2, size = 5)
                    val mockAnnouncements =
                        listOf(
                            dummyAnnouncement(1L, "공지사항1"),
                            dummyAnnouncement(2L, "공지사항2"),
                        )
                    val mockPage: Page<Announcement?> = PageImpl(mockAnnouncements, PageRequest.of(1, 5), 7)

                    every {
                        announcementRepository.findPage(any<Pageable>(), any<Jpql.() -> JpqlQueryable<SelectQuery<Announcement>>>())
                    } returns mockPage

                    // when
                    val result = announcementService.findAll(searchRequest)

                    // then
                    result.pageNumber shouldBe 2
                    result.pageSize shouldBe 5
                    result.totalElements shouldBe 7L
                }

                then("빈 결과를 반환해야 한다") {
                    // given
                    val searchRequest = SearchRequest(page = 1, size = 10)
                    val mockPage: Page<Announcement?> = PageImpl(emptyList(), PageRequest.of(0, 10), 0)

                    every {
                        announcementRepository.findPage(any<Pageable>(), any<Jpql.() -> JpqlQueryable<SelectQuery<Announcement>>>())
                    } returns mockPage

                    // when
                    val result = announcementService.findAll(searchRequest)

                    // then
                    result.content.size shouldBe 0
                    result.totalElements shouldBe 0L
                }
            }
        }
    })

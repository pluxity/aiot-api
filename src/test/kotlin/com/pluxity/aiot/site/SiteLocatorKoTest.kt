package com.pluxity.aiot.site

import com.pluxity.aiot.fixture.SiteFixture
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

private data class Device(
    val name: String,
    val lng: Double?,
    val lat: Double?,
)

class SiteLocatorKoTest :
    BehaviorSpec({
        val siteA = SiteFixture.create(id = 1, location = SiteFixture.createCustomPolygon(127.0, 37.0, 127.1, 37.1))
        val siteB = SiteFixture.create(id = 2, location = SiteFixture.createCustomPolygon(128.0, 36.0, 128.1, 36.1))
        val devices =
            listOf(
                Device("A 안", 127.05, 37.05),
                Device("B 안", 128.05, 36.05),
                Device("어디에도 없음", 126.0, 35.0),
                Device("좌표 없음", null, null),
                Device("A 경계선", 127.0, 37.05),
            )

        fun locate(
            locator: SiteLocator,
            siteId: Long?,
        ) = locator
            .locate(devices, siteId, longitude = { it.lng }, latitude = { it.lat })
            .map { (device, site) -> device.name to site?.id }

        Given("현장 A, B가 있음") {
            val repository = mockk<SiteRepository>()
            every { repository.findAll() } returns listOf(siteA, siteB)
            every { repository.findByIdOrNull(1L) } returns siteA
            every { repository.findByIdOrNull(99L) } returns null
            val locator = SiteLocator(repository)

            When("현장 아이디 없이 조회함") {
                Then("전체를 반환하고 좌표가 들어가는 현장을 붙인다") {
                    locate(locator, null) shouldContainExactly
                        listOf("A 안" to 1L, "B 안" to 2L, "어디에도 없음" to null, "좌표 없음" to null, "A 경계선" to null)
                }
            }

            When("현장 A로 조회함") {
                Then("A 폴리곤 안의 장비만 남긴다") {
                    locate(locator, 1L) shouldContainExactly listOf("A 안" to 1L)
                }
            }

            When("없는 현장으로 조회함") {
                Then("빈 목록을 반환한다") {
                    locate(locator, 99L).shouldBeEmpty()
                }
            }
        }

        Given("장비가 없음") {
            val repository = mockk<SiteRepository>()
            val locator = SiteLocator(repository)

            When("조회함") {
                val result = locator.locate(emptyList<Device>(), null, longitude = { it.lng }, latitude = { it.lat })

                Then("현장을 조회하지 않는다") {
                    result.shouldBeEmpty()
                    verify(exactly = 0) { repository.findAll() }
                }
            }
        }
    })

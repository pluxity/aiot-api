package com.pluxity.aiot.feature

import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.site.SiteRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * 구독 알림 처리는 트랜잭션 밖에서 Feature를 읽고, 프로세서 캐시에 며칠씩 들고 있는다.
 * site가 LAZY 프록시로 남아 있으면 세션이 없어 터진다. 실제 세션 경계로만 드러나므로 mock으로는 못 잡는다.
 */
@SpringBootTest
@ActiveProfiles("test")
class FeatureLazySiteIdKoTest(
    private val featureRepository: FeatureRepository,
    private val siteRepository: SiteRepository,
    transactionManager: PlatformTransactionManager,
) : BehaviorSpec({
        extension(SpringExtension(SpringTestLifecycleMode.Root))

        val transaction = TransactionTemplate(transactionManager)

        afterEach {
            featureRepository.deleteAll()
            siteRepository.deleteAll()
        }

        Given("site가 배정된 Feature") {
            val siteId =
                transaction.execute {
                    val site = siteRepository.save(SiteFixture.create(name = "현장"))
                    featureRepository.save(Feature(deviceId = "SNIOT-P-WFL-001", objectId = "34957").apply { this.site = site })
                    site.requiredId
                }!!

            When("트랜잭션 밖에서 조회해 siteId를 읽으면") {
                val feature = featureRepository.findByDeviceId("SNIOT-P-WFL-001")!!

                Then("siteId를 돌려준다") {
                    feature.requiredSiteId shouldBe siteId
                }

                Then("알림 payload에 쓰는 site 이름도 읽힌다") {
                    feature.site?.name shouldBe "현장"
                }
            }
        }
    })

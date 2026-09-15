package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.incident.IncidentRepository
import com.pluxity.aiot.incident.IncidentService
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.mockk
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 운영은 트랜잭션 없이 알림을 처리한다. 트랜잭션 테스트에서는 영속성 컨텍스트가 같은 객체를 돌려줘
 * 상태 비교 객체와 갱신 객체가 어긋나는 문제가 드러나지 않는다.
 */
@SpringBootTest
@ActiveProfiles("test")
class WasteFillLevelRepeatedNotificationKoTest(
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    eventConditionRepository: EventConditionRepository,
    incidentService: IncidentService,
    private val incidentRepository: IncidentRepository,
) : BehaviorSpec({
        extension(SpringExtension(SpringTestLifecycleMode.Root))

        afterEach {
            incidentRepository.deleteAll()
            eventHistoryRepository.deleteAll()
            featureRepository.deleteAll()
            siteRepository.deleteAll()
        }

        Given("만재 상태 쓰레기 감지기") {
            val deviceId = "WFL_REPEAT_001"
            val helper =
                WasteFillLevelProcessorTestHelper(
                    siteRepository,
                    featureRepository,
                    eventHistoryRepository,
                    mockk(relaxed = true),
                    Mockito.mock(WriteApi::class.java),
                    eventConditionRepository,
                    incidentService,
                )
            val setup = helper.setupDevice(SensorType.WASTE_FILL_LEVEL.objectId, deviceId)
            val processor = helper.createProcessor()

            When("같은 만재 값이 세 번 연속 들어오면") {
                repeat(3) {
                    processor.process(
                        deviceId,
                        setup.sensorType,
                        setup.siteId,
                        helper.createSensorData(actualFilling = 10, highThreshold = 30),
                    )
                }

                Then("이력과 incident는 한 건씩만 쌓인다") {
                    eventHistoryRepository.findByDeviceId(deviceId) shouldHaveSize 1
                    incidentRepository.findAll().filter { it.deviceId == deviceId } shouldHaveSize 1
                }
            }
        }
    })

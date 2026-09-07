package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.data.measure.OdorMonitor
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.sensor.type.DeviceProfileEnum
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OdorMonitorProcessorTest(
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    eventConditionRepository: EventConditionRepository,
) : BehaviorSpec({
        extension(SpringExtension(SpringTestLifecycleMode.Root))

        val messageSenderMock = Mockito.mock(StompMessageSender::class.java)

        fun helperWith(writeApi: WriteApi) =
            OdorMonitorProcessorTestHelper(
                siteRepository,
                featureRepository,
                eventHistoryRepository,
                messageSenderMock,
                writeApi,
                eventConditionRepository,
            )

        Given("화장실 악취 감지기: 암모니아 기준치(GE 25) 이벤트 조건") {
            When("NH3 = 30ppm - 조건 충족") {
                val deviceId = "BOS_001"
                val helper = helperWith(Mockito.mock(WriteApi::class.java))

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.ODOR_MONITOR.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "25.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.NH3.fieldKey,
                    )

                val sensorData = helper.createSensorData(nh3 = 30)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("이벤트가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "NH3"
                    eventHistories.first().value shouldBe 30.0
                    eventHistories.first().eventName shouldBe "WARNING_NH3"
                    eventHistories.first().status shouldBe EventStatus.ACTIVE
                }
            }

            When("NH3 = 10ppm - 조건 미충족") {
                val deviceId = "BOS_002"
                val helper = helperWith(Mockito.mock(WriteApi::class.java))

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.ODOR_MONITOR.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "25.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.NH3.fieldKey,
                    )

                val sensorData = helper.createSensorData(nh3 = 10)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("이벤트가 발생하지 않고 NORMAL 상태") {
                    eventHistoryRepository.findByDeviceId(deviceId) shouldHaveSize 0

                    val feature = helper.featureRepository.findByDeviceId(deviceId)
                    feature.shouldNotBeNull()
                    feature.eventStatus shouldBe "NORMAL"
                }
            }
        }

        Given("화장실 악취 감지기: InfluxDB 적재") {
            When("모든 계측 항목이 수신됨") {
                val deviceId = "BOS_003"
                val writeApiMock = Mockito.mock(WriteApi::class.java)
                val helper = helperWith(writeApiMock)

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.ODOR_MONITOR.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "25.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.NH3.fieldKey,
                    )

                val sensorData =
                    helper.createSensorData(
                        temperature = 24.0,
                        humidity = 55.0,
                        nh3 = 30,
                        h2s = 12,
                    )
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("모든 항목이 fieldKey 태그와 함께 기록된다") {
                    val captor = ArgumentCaptor.forClass(OdorMonitor::class.java)
                    Mockito
                        .verify(writeApiMock, Mockito.times(4))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), captor.capture())

                    val written = captor.allValues
                    written.map { it.fieldKey } shouldBe
                        listOf("Temperature", "Humidity", "NH3", "H2S")
                    written.map { it.value } shouldBe
                        listOf(24.0, 55.0, 30.0, 12.0)
                    written.all { it.deviceId == deviceId } shouldBe true
                    written.all { it.siteId == setup.siteId.toString() } shouldBe true
                }
            }

            When("일부 항목만 수신됨") {
                val deviceId = "BOS_004"
                val writeApiMock = Mockito.mock(WriteApi::class.java)
                val helper = helperWith(writeApiMock)

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.ODOR_MONITOR.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "25.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.NH3.fieldKey,
                    )

                val sensorData = helper.createSensorData(temperature = 24.0, h2s = 5)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("수신된 항목만 기록된다") {
                    Mockito
                        .verify(writeApiMock, Mockito.times(2))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), Mockito.any(OdorMonitor::class.java))
                }
            }
        }
    })

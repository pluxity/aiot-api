package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.data.measure.ForestFireDetection
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
class ForestFireProcessorTest(
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    eventConditionRepository: EventConditionRepository,
) : BehaviorSpec({
        extension(SpringExtension)

        val messageSenderMock = Mockito.mock(StompMessageSender::class.java)

        fun helperWith(writeApi: WriteApi) =
            ForestFireProcessorTestHelper(
                siteRepository,
                featureRepository,
                eventHistoryRepository,
                messageSenderMock,
                writeApi,
                eventConditionRepository,
            )

        Given("산불 감지기: 산불 감지(Boolean) 이벤트 조건") {
            When("fireDetection = true - 조건 충족") {
                val deviceId = "FFA_001"
                val helper = helperWith(Mockito.mock(WriteApi::class.java))

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.FOREST_FIRE.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.DANGER,
                        minValue = null,
                        maxValue = null,
                        isBoolean = true,
                        fieldKey = DeviceProfileEnum.FOREST_FIRE_DETECTION.fieldKey,
                    )

                val sensorData = helper.createSensorData(fireDetection = true)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("이벤트가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "FireDetection"
                    eventHistories.first().value shouldBe 1.0
                    eventHistories.first().eventName shouldBe "DANGER_FireDetection"
                    eventHistories.first().status shouldBe EventStatus.ACTIVE
                }
            }

            When("fireDetection = false - 조건 미충족") {
                val deviceId = "FFA_002"
                val helper = helperWith(Mockito.mock(WriteApi::class.java))

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.FOREST_FIRE.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.DANGER,
                        minValue = null,
                        maxValue = null,
                        isBoolean = true,
                        fieldKey = DeviceProfileEnum.FOREST_FIRE_DETECTION.fieldKey,
                    )

                val sensorData = helper.createSensorData(fireDetection = false)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("이벤트가 발생하지 않고 NORMAL 상태") {
                    eventHistoryRepository.findByDeviceId(deviceId) shouldHaveSize 0

                    val feature = helper.featureRepository.findByDeviceId(deviceId)
                    feature.shouldNotBeNull()
                    feature.eventStatus shouldBe "NORMAL"
                }
            }
        }

        Given("산불 감지기: InfluxDB 적재") {
            When("모든 계측 항목이 수신됨") {
                val deviceId = "FFA_003"
                val writeApiMock = Mockito.mock(WriteApi::class.java)
                val helper = helperWith(writeApiMock)

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.FOREST_FIRE.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.DANGER,
                        minValue = null,
                        maxValue = null,
                        isBoolean = true,
                        fieldKey = DeviceProfileEnum.FOREST_FIRE_DETECTION.fieldKey,
                    )

                val sensorData =
                    helper.createSensorData(
                        fireDetection = true,
                        temperature = 35.0,
                        humidity = 20.0,
                        co2 = 800,
                        co = 15,
                        tvoc = 500,
                        fireCauseMask = 3,
                    )
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("모든 항목이 fieldKey 태그와 함께 기록된다") {
                    val captor = ArgumentCaptor.forClass(ForestFireDetection::class.java)
                    Mockito
                        .verify(writeApiMock, Mockito.times(7))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), captor.capture())

                    val written = captor.allValues
                    written.map { it.fieldKey } shouldBe
                        listOf("FireDetection", "Temperature", "Humidity", "CO2", "CO", "TVOC", "FireCauseMask")
                    written.map { it.value } shouldBe
                        listOf(1.0, 35.0, 20.0, 800.0, 15.0, 500.0, 3.0)
                    written.all { it.deviceId == deviceId } shouldBe true
                    written.all { it.siteId == setup.siteId.toString() } shouldBe true
                }
            }

            When("일부 항목만 수신됨") {
                val deviceId = "FFA_004"
                val writeApiMock = Mockito.mock(WriteApi::class.java)
                val helper = helperWith(writeApiMock)

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.FOREST_FIRE.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.DANGER,
                        minValue = null,
                        maxValue = null,
                        isBoolean = true,
                        fieldKey = DeviceProfileEnum.FOREST_FIRE_DETECTION.fieldKey,
                    )

                val sensorData = helper.createSensorData(temperature = 30.0, co2 = 700)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("수신된 항목만 기록된다") {
                    Mockito
                        .verify(writeApiMock, Mockito.times(2))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), Mockito.any(ForestFireDetection::class.java))
                }
            }
        }
    })

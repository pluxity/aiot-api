package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.data.measure.CompositeAirQuality
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
class CompositeAirQualityProcessorTest(
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    eventConditionRepository: EventConditionRepository,
) : BehaviorSpec({
        extension(SpringExtension)

        val messageSenderMock = Mockito.mock(StompMessageSender::class.java)

        fun helperWith(writeApi: WriteApi) =
            CompositeAirQualityProcessorTestHelper(
                siteRepository,
                featureRepository,
                eventHistoryRepository,
                messageSenderMock,
                writeApi,
                eventConditionRepository,
            )

        Given("복합 대기질 센서: 초미세먼지 기준치(GE 35) 이벤트 조건") {
            When("PM2.5 = 75 - 조건 충족") {
                val deviceId = "CAQ_001"
                val helper = helperWith(Mockito.mock(WriteApi::class.java))

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.COMPOSITE_AIR_QUALITY.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "35.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.PM2_5.fieldKey,
                    )

                val sensorData = helper.createSensorData(pm25 = 75)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("이벤트가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "PM2.5"
                    eventHistories.first().value shouldBe 75.0
                    eventHistories.first().eventName shouldBe "WARNING_PM2.5"
                    eventHistories.first().status shouldBe EventStatus.ACTIVE
                }
            }

            When("PM2.5 = 10 - 조건 미충족") {
                val deviceId = "CAQ_002"
                val helper = helperWith(Mockito.mock(WriteApi::class.java))

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.COMPOSITE_AIR_QUALITY.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "35.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.PM2_5.fieldKey,
                    )

                val sensorData = helper.createSensorData(pm25 = 10)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("이벤트가 발생하지 않고 NORMAL 상태") {
                    eventHistoryRepository.findByDeviceId(deviceId) shouldHaveSize 0

                    val feature = helper.featureRepository.findByDeviceId(deviceId)
                    feature.shouldNotBeNull()
                    feature.eventStatus shouldBe "NORMAL"
                }
            }
        }

        Given("복합 대기질 센서: InfluxDB 적재") {
            When("모든 계측 항목이 수신됨") {
                val deviceId = "CAQ_003"
                val writeApiMock = Mockito.mock(WriteApi::class.java)
                val helper = helperWith(writeApiMock)

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.COMPOSITE_AIR_QUALITY.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "35.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.PM2_5.fieldKey,
                    )

                val sensorData =
                    helper.createSensorData(
                        temperature = 23.5,
                        humidity = 70.0,
                        pm25 = 75,
                        pm10 = 30,
                        windSpeed = 5,
                        windDirection = 175,
                        uvi = 10,
                        ledLight = 0,
                    )
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("모든 항목이 fieldKey 태그와 함께 기록된다") {
                    val captor = ArgumentCaptor.forClass(CompositeAirQuality::class.java)
                    Mockito
                        .verify(writeApiMock, Mockito.times(8))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), captor.capture())

                    val written = captor.allValues
                    written.map { it.fieldKey } shouldBe
                        listOf("Temperature", "Humidity", "PM2.5", "PM10", "WindSpeed", "WindDirection", "UVI", "LED Light")
                    written.map { it.value } shouldBe
                        listOf(23.5, 70.0, 75.0, 30.0, 5.0, 175.0, 10.0, 0.0)
                    written.all { it.deviceId == deviceId } shouldBe true
                    written.all { it.siteId == setup.siteId.toString() } shouldBe true
                }
            }

            When("일부 항목만 수신됨") {
                val deviceId = "CAQ_004"
                val writeApiMock = Mockito.mock(WriteApi::class.java)
                val helper = helperWith(writeApiMock)

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.COMPOSITE_AIR_QUALITY.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "35.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.PM2_5.fieldKey,
                    )

                val sensorData = helper.createSensorData(temperature = 23.5, uvi = 3)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("수신된 항목만 기록된다") {
                    Mockito
                        .verify(writeApiMock, Mockito.times(2))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), Mockito.any(CompositeAirQuality::class.java))
                }
            }
        }
    })

package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.data.measure.PeopleCounter
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
class PeopleCounterProcessorTest(
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    eventConditionRepository: EventConditionRepository,
) : BehaviorSpec({
        extension(SpringExtension)

        val messageSenderMock = Mockito.mock(StompMessageSender::class.java)

        fun helperWith(writeApi: WriteApi) =
            PeopleCounterProcessorTestHelper(
                siteRepository,
                featureRepository,
                eventHistoryRepository,
                messageSenderMock,
                writeApi,
                eventConditionRepository,
            )

        Given("피플카운터: 입장객수 기준치(GE 100) 이벤트 조건") {
            When("입장객수 = 150명 - 조건 충족") {
                val deviceId = "APC_001"
                val helper = helperWith(Mockito.mock(WriteApi::class.java))

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.PEOPLE_COUNTER.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.CAUTION,
                        minValue = "100.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.NUMBER_OF_VISITORS.fieldKey,
                    )

                val sensorData = helper.createSensorData(numberOfVisitors = 150)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("이벤트가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "NumberOfVisitors"
                    eventHistories.first().value shouldBe 150.0
                    eventHistories.first().eventName shouldBe "CAUTION_NumberOfVisitors"
                    eventHistories.first().status shouldBe EventStatus.ACTIVE
                }
            }

            When("입장객수 = 30명 - 조건 미충족") {
                val deviceId = "APC_002"
                val helper = helperWith(Mockito.mock(WriteApi::class.java))

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.PEOPLE_COUNTER.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.CAUTION,
                        minValue = "100.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.NUMBER_OF_VISITORS.fieldKey,
                    )

                val sensorData = helper.createSensorData(numberOfVisitors = 30)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("이벤트가 발생하지 않고 NORMAL 상태") {
                    eventHistoryRepository.findByDeviceId(deviceId) shouldHaveSize 0

                    val feature = helper.featureRepository.findByDeviceId(deviceId)
                    feature.shouldNotBeNull()
                    feature.eventStatus shouldBe "NORMAL"
                }
            }
        }

        Given("피플카운터: InfluxDB 적재") {
            When("모든 계측 항목이 수신됨") {
                val deviceId = "APC_003"
                val writeApiMock = Mockito.mock(WriteApi::class.java)
                val helper = helperWith(writeApiMock)

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.PEOPLE_COUNTER.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.CAUTION,
                        minValue = "100.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.NUMBER_OF_VISITORS.fieldKey,
                    )

                val sensorData = helper.createSensorData(numberOfVisitors = 150, numberOfLeavers = 120)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("모든 항목이 fieldKey 태그와 함께 기록된다") {
                    val captor = ArgumentCaptor.forClass(PeopleCounter::class.java)
                    Mockito
                        .verify(writeApiMock, Mockito.times(2))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), captor.capture())

                    val written = captor.allValues
                    written.map { it.fieldKey } shouldBe
                        listOf("NumberOfVisitors", "NumberOfLeavers")
                    written.map { it.value } shouldBe
                        listOf(150.0, 120.0)
                    written.all { it.deviceId == deviceId } shouldBe true
                    written.all { it.siteId == setup.siteId.toString() } shouldBe true
                }
            }

            When("일부 항목만 수신됨") {
                val deviceId = "APC_004"
                val writeApiMock = Mockito.mock(WriteApi::class.java)
                val helper = helperWith(writeApiMock)

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.PEOPLE_COUNTER.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.CAUTION,
                        minValue = "100.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.NUMBER_OF_VISITORS.fieldKey,
                    )

                val sensorData = helper.createSensorData(numberOfLeavers = 40)
                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("수신된 항목만 기록된다") {
                    Mockito
                        .verify(writeApiMock, Mockito.times(1))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), Mockito.any(PeopleCounter::class.java))
                }
            }
        }
    })

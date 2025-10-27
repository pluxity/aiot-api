package com.pluxity.aiot.data

import com.influxdb.client.QueryApi
import com.pluxity.aiot.data.dto.ClimateSensorData
import com.pluxity.aiot.data.dto.DisplacementGaugeSensorData
import com.pluxity.aiot.data.dto.dummyClimateSensorData
import com.pluxity.aiot.data.dto.dummyDisplacementGaugeSensorData
import com.pluxity.aiot.data.enum.DataInterval
import com.pluxity.aiot.feature.FeatureService
import com.pluxity.aiot.feature.dto.dummyFeatureResponse
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.InfluxdbProperties
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteService
import com.pluxity.aiot.site.dto.dummySiteResponse
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.time.ZoneOffset

class DataServiceKoTest :
    BehaviorSpec({

        val influxdbProperties: InfluxdbProperties =
            mockk {
                every { bucket } returns "test-bucket"
                every { org } returns "test-org"
            }
        val queryApi: QueryApi = mockk()
        val featureService: FeatureService = mockk()
        val siteService: SiteService = mockk()

        val dataService =
            DataService(
                influxdbProperties,
                queryApi,
                featureService,
                siteService,
            )

        Given("피처 시계열 데이터를 조회할 때") {
            When("온습도 센서의 시간별 데이터 조회 요청") {
                val deviceId = "TH001-34954-id"
                val interval = DataInterval.HOUR
                val from = "20240101000000"
                val to = "20240101235959"
                val feature =
                    dummyFeatureResponse(
                        deviceId = deviceId,
                        objectId = "34954",
                    )
                val climateData =
                    listOf(
                        dummyClimateSensorData(
                            time = LocalDateTime.of(2024, 1, 1, 10, 0).toInstant(ZoneOffset.UTC),
                            temperature = 25.0,
                            humidity = 60.0,
                            discomfortIndex = 30.0,
                        ),
                    )

                every {
                    featureService.findByDeviceIdResponse(deviceId)
                } returns feature

                every {
                    queryApi.query(any<String>(), "test-org", ClimateSensorData::class.java)
                } returns climateData

                Then("온습도 시계열 데이터 반환") {
                    val result = dataService.getFeatureTimeSeries(deviceId, interval, from, to)
                    result shouldNotBe null
                    result.meta.targetId shouldBe deviceId
                    result.timestamps.isNotEmpty() shouldBe true
                    result.metrics["Temperature"]?.values[0] shouldBe 25.0
                    result.metrics["Humidity"]?.values[0] shouldBe 60.0
                    result.metrics["DiscomfortIndex"]?.values[0] shouldBe 30.0
                }
            }

            When("변위계 센서의 일별 데이터 조회 요청") {
                val deviceId = "DG001-device-id"
                val interval = DataInterval.DAY
                val from = "20240101000000"
                val to = "20240131235959"
                val feature =
                    dummyFeatureResponse(
                        deviceId = deviceId,
                        objectId = "34957",
                    )
                val displacementData =
                    listOf(
                        dummyDisplacementGaugeSensorData(
                            time = LocalDateTime.of(2024, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
                            angleX = 12.5,
                            angleY = 15.2,
                        ),
                    )

                every {
                    featureService.findByDeviceIdResponse(deviceId)
                } returns feature

                every {
                    queryApi.query(any<String>(), "test-org", DisplacementGaugeSensorData::class.java)
                } returns displacementData

                Then("변위계 시계열 데이터 반환") {
                    val result = dataService.getFeatureTimeSeries(deviceId, interval, from, to)
                    result shouldNotBe null
                    result.meta.targetId shouldBe deviceId
                    result.timestamps.isNotEmpty() shouldBe true
                    result.metrics["Angle-X"]?.values[0] shouldBe 12.5
                    result.metrics["Angle-Y"]?.values[0] shouldBe 15.2
                }
            }

            When("지원하지 않는 센서 타입으로 조회 요청") {
                val deviceId = "XX001-device-id"
                val interval = DataInterval.HOUR
                val from = "20240101000000"
                val to = "20240101235959"
                val feature =
                    dummyFeatureResponse(
                        deviceId = deviceId,
                        objectId = "abced",
                    )

                every {
                    featureService.findByDeviceIdResponse(deviceId)
                } returns feature

                Then("IllegalArgumentException 예외 발생") {
                    shouldThrowExactly<IllegalArgumentException> {
                        dataService.getFeatureLatestData(deviceId)
                    }.message shouldBe "Unknown objectId: ${feature.objectId}"
                }
            }

            When("존재하지 않는 deviceId로 조회 요청") {
                val deviceId = "NON-EXISTENT"
                val interval = DataInterval.HOUR
                val from = "20240101000000"
                val to = "20240101235959"

                every {
                    featureService.findByDeviceIdResponse(deviceId)
                } throws CustomException(ErrorCode.NOT_FOUND_FEATURE)

                Then("NOT_FOUND_FEATURE 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        dataService.getFeatureTimeSeries(deviceId, interval, from, to)
                    }.message shouldBe ErrorCode.NOT_FOUND_FEATURE.getMessage()
                }
            }
        }

        Given("사이트 시계열 데이터를 조회할 때") {
            When("유효한 사이트 ID와 온습도 센서로 조회 요청") {
                val siteId = 1L
                val interval = DataInterval.DAY
                val from = "20240101000000"
                val to = "20240131235959"
                val sensorType = SensorType.TEMPERATURE_HUMIDITY
                val siteResponse = dummySiteResponse(id = siteId)
                val climateData =
                    listOf(
                        dummyClimateSensorData(
                            time = LocalDateTime.of(2024, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
                            temperature = 22.0,
                            humidity = 55.0,
                            discomfortIndex = 40.0,
                        ),
                    )

                every {
                    siteService.findByIdResponse(siteId)
                } returns siteResponse

                every {
                    queryApi.query(any<String>(), "test-org", ClimateSensorData::class.java)
                } returns climateData

                Then("사이트 시계열 데이터 반환") {
                    val result = dataService.getSiteTimeSeries(siteId, interval, from, to, sensorType)
                    result shouldNotBe null
                    result.meta.targetId shouldBe siteId.toString()
                    result.timestamps.isNotEmpty() shouldBe true
                    result.metrics["Temperature"]?.values[0] shouldBe 22.0
                    result.metrics["Humidity"]?.values[0] shouldBe 55.0
                    result.metrics["DiscomfortIndex"]?.values[0] shouldBe 40.0
                }
            }

            When("존재하지 않는 사이트 ID로 조회 요청") {
                val siteId = 999L
                val interval = DataInterval.HOUR
                val from = "20240101000000"
                val to = "20240101235959"
                val sensorType = SensorType.TEMPERATURE_HUMIDITY

                every {
                    siteService.findByIdResponse(siteId)
                } throws CustomException(ErrorCode.NOT_FOUND_SITE)

                Then("NOT_FOUND_SITE 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        dataService.getSiteTimeSeries(siteId, interval, from, to, sensorType)
                    }.message shouldBe ErrorCode.NOT_FOUND_SITE.getMessage()
                }
            }
        }

        Given("피처 최신 데이터를 조회할 때") {
            When("온습도 센서의 최신 데이터 조회 요청") {
                val deviceId = "TH001-device-id"
                val feature =
                    dummyFeatureResponse(
                        deviceId = deviceId,
                        objectId = "34954",
                    )
                val climateData =
                    listOf(
                        dummyClimateSensorData(
                            time = LocalDateTime.now().toInstant(ZoneOffset.UTC),
                            temperature = 23.5,
                            humidity = 58.0,
                            discomfortIndex = 30.0,
                        ),
                    )

                every {
                    featureService.findByDeviceIdResponse(deviceId)
                } returns feature

                every {
                    queryApi.query(any<String>(), "test-org", ClimateSensorData::class.java)
                } returns climateData

                Then("최신 온습도 데이터 반환") {
                    val result = dataService.getFeatureLatestData(deviceId)
                    result shouldNotBe null
                    result.meta.targetId shouldBe deviceId
                    result.metrics["Temperature"]?.value shouldBe 23.5
                    result.metrics["Humidity"]?.value shouldBe 58.0
                    result.metrics["DiscomfortIndex"]?.value shouldBe 30.0
                }
            }

            When("변위계 센서의 최신 데이터 조회 요청") {
                val deviceId = "DG001-device-id"
                val feature =
                    dummyFeatureResponse(
                        deviceId = deviceId,
                        objectId = "34957",
                    )
                val displacementData =
                    listOf(
                        dummyDisplacementGaugeSensorData(
                            time = LocalDateTime.now().toInstant(ZoneOffset.UTC),
                            angleX = 15.2,
                            angleY = 12.5,
                        ),
                    )

                every {
                    featureService.findByDeviceIdResponse(deviceId)
                } returns feature

                every {
                    queryApi.query(any<String>(), "test-org", DisplacementGaugeSensorData::class.java)
                } returns displacementData

                Then("최신 변위계 데이터 반환") {
                    val result = dataService.getFeatureLatestData(deviceId)
                    result shouldNotBe null
                    result.meta.targetId shouldBe deviceId
                    result.metrics["Angle-X"]?.value shouldBe 15.2
                    result.metrics["Angle-Y"]?.value shouldBe 12.5
                }
            }

            When("데이터가 없는 센서로 최신 데이터 조회 요청") {
                val deviceId = "TH001-device-id"
                val feature =
                    dummyFeatureResponse(
                        deviceId = deviceId,
                        objectId = "34954",
                    )

                every {
                    featureService.findByDeviceIdResponse(deviceId)
                } returns feature

                every {
                    queryApi.query(any<String>(), "test-org", ClimateSensorData::class.java)
                } returns emptyList()

                Then("NOT_FOUND_DATA 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        dataService.getFeatureLatestData(deviceId)
                    }.message shouldBe ErrorCode.NOT_FOUND_DATA.getMessage()
                }
            }

            When("지원하지 않는 센서 타입으로 최신 데이터 조회 요청") {
                val deviceId = "XX001-abcde-id"
                val feature =
                    dummyFeatureResponse(
                        deviceId = deviceId,
                        objectId = "abcde",
                    )

                every {
                    featureService.findByDeviceIdResponse(deviceId)
                } returns feature

                Then("IllegalArgumentException 예외 발생") {
                    shouldThrowExactly<IllegalArgumentException> {
                        dataService.getFeatureLatestData(deviceId)
                    }.message shouldBe "Unknown objectId: ${feature.objectId}"
                }
            }
        }
    })

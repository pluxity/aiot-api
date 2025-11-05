package com.pluxity.aiot.announcement

import com.influxdb.annotations.Column
import com.influxdb.client.QueryApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import com.pluxity.aiot.announcement.dto.LlmMessageResponse
import com.pluxity.aiot.announcement.dto.LlmRequest
import com.pluxity.aiot.announcement.dto.LlmResponse
import com.pluxity.aiot.global.properties.InfluxdbProperties
import com.pluxity.aiot.global.properties.LlmProperties
import com.pluxity.aiot.sensor.type.DeviceProfileEnum
import com.pluxity.aiot.sensor.type.SensorType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val log = KotlinLogging.logger {}

@Service
class LlmMessageService(
    private val queryApi: QueryApi,
    private val influxdbProperties: InfluxdbProperties,
    private val llmMessageRepository: LlmMessageRepository,
    llmProperties: LlmProperties,
    webClientBuilder: WebClient.Builder,
) {
    private val webClient: WebClient = webClientBuilder.baseUrl(llmProperties.baseUrl).build()

    @Transactional
    suspend fun generateAndSaveMessage() {
        // 1. 어제와 오늘의 평균 온도 조회
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val yesterdayAvgTemp = getAverageTemperature(yesterday)
        val todayAvgTemp = getAverageTemperature(today)

        log.info { "어제 평균 온도: $yesterdayAvgTemp°C, 오늘 평균 온도: $todayAvgTemp°C" }

        // 2. LLM API 호출을 위한 프롬프트 생성
        val prompt = "어제 평균온도는 ${yesterdayAvgTemp}도 이고 오늘 평균온도는 ${todayAvgTemp}도야. 이 상황에 공원 방문객을 위한 전광판 메시지를 한줄로 짧게 작성해줘."

        val llmRequest =
            LlmRequest(
                prompt = prompt,
                max_new_tokens = 100,
                temperature = 0.7,
            )

        // 3. 외부 LLM API 호출
        val llmResponse =
            webClient
                .post()
                .uri("/generate")
                .bodyValue(llmRequest)
                .retrieve()
                .awaitBody<LlmResponse>()

        val generatedMessage = llmResponse.generated_text
        log.info { "생성된 LLM 메시지: $generatedMessage" }

        // 4. DB에 저장
        val llmMessage =
            LlmMessage(
                yesterdayAvgTemp = yesterdayAvgTemp,
                todayAvgTemp = todayAvgTemp,
                prompt = prompt,
                message = generatedMessage,
            )

        llmMessageRepository.save(llmMessage)
        log.info { "LLM 메시지 저장 완료: ID=${llmMessage.id}" }
    }

    private fun getAverageTemperature(date: LocalDate): Double {
        // LocalDate를 Instant로 변환 (날짜의 시작 시간)
        val startOfDay = date.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant()

        // 온습도계 센서 타입 조회
        val temperatureHumiditySensor = SensorType.TEMPERATURE_HUMIDITY
        val temperatureField = DeviceProfileEnum.TEMPERATURE.fieldKey

        // InfluxDB 쿼리 생성
        val query =
            Flux
                .from(influxdbProperties.bucket)
                .range(startOfDay, endOfDay)
                .filter(
                    Restrictions.and(
                        Restrictions.measurement().equal(temperatureHumiditySensor.measureName),
                        Restrictions.field().equal(temperatureField),
                    ),
                ).mean("_value")
                .toString()

        log.debug { "Temperature query: $query" }

        // 쿼리 실행
        val results = queryApi.query(query, influxdbProperties.org, TemperatureData::class.java)

        // 평균 온도 추출
        val avgTemp =
            results
                .firstOrNull()
                ?.value
                ?: 0.0

        return String.format("%.1f", avgTemp).toDouble()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<LlmMessageResponse> {
        val messages = llmMessageRepository.findAll()
        return messages.map { LlmMessageResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): LlmMessageResponse {
        val message =
            llmMessageRepository.findByIdOrNull(id)
                ?: throw IllegalArgumentException("LLM 메시지를 찾을 수 없습니다. ID: $id")
        return LlmMessageResponse.from(message)
    }

    data class TemperatureData(
        @Column(name = "_value") val value: Double? = null,
        @Column(name = "_time") val time: Instant? = null,
    )
}

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
import java.time.LocalDateTime
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
        // 1. 현재 시간 기준으로 이전 시간대의 평균 온도 조회
        val now = LocalDateTime.now()
        val currentHour = now.hour // 현재 시간 (예: 7시 30분이면 7)
        val targetHour = if (currentHour > 0) currentHour - 1 else 23 // 이전 시간 (예: 7시 30분이면 6시)

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // 어제와 오늘의 같은 시간대 (targetHour:00 ~ targetHour+1:00) 평균 온도 조회
        val yesterdayAvgTemp = getHourlyAverageTemperature(yesterday, targetHour)
        val todayAvgTemp = getHourlyAverageTemperature(today, targetHour)

        log.info {
            "어제 ${targetHour}시~${targetHour + 1}시 평균 온도: $yesterdayAvgTemp°C, 오늘 ${targetHour}시~${targetHour + 1}시 평균 온도: $todayAvgTemp°C"
        }

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

    private fun getHourlyAverageTemperature(
        date: LocalDate,
        hour: Int,
    ): Double {
        // 해당 날짜의 특정 시간대 (hour:00 ~ hour+1:00)
        val startOfHour = date.atTime(hour, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant()
        val endOfHour = date.atTime(hour + 1, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant()

        // 온습도계 센서 타입 조회
        val temperatureHumiditySensor = SensorType.TEMPERATURE_HUMIDITY
        val temperatureField = DeviceProfileEnum.TEMPERATURE.fieldKey

        // InfluxDB 쿼리 생성
        val query =
            Flux
                .from(influxdbProperties.bucket)
                .range(startOfHour, endOfHour)
                .filter(
                    Restrictions.and(
                        Restrictions.measurement().equal(temperatureHumiditySensor.measureName),
                        Restrictions.field().equal(temperatureField),
                    ),
                ).mean("_value")
                .toString()

        log.debug { "Temperature query for $date $hour:00-${hour + 1}:00: $query" }

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

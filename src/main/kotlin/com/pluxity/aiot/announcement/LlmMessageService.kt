package com.pluxity.aiot.announcement

import com.influxdb.annotations.Column
import com.influxdb.client.QueryApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import com.pluxity.aiot.announcement.dto.LlmMessageResponse
import com.pluxity.aiot.announcement.dto.LlmRequest
import com.pluxity.aiot.announcement.dto.LlmResponse
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.InfluxdbProperties
import com.pluxity.aiot.global.properties.LlmProperties
import com.pluxity.aiot.sensor.type.DeviceProfileEnum
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.Site
import com.pluxity.aiot.site.SiteRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Service
class LlmMessageService(
    private val queryApi: QueryApi,
    private val influxdbProperties: InfluxdbProperties,
    private val llmMessageRepository: LlmMessageRepository,
    private val siteRepository: SiteRepository,
    llmProperties: LlmProperties,
    webClientBuilder: WebClient.Builder,
) {
    private val webClient: WebClient = webClientBuilder.baseUrl(llmProperties.baseUrl).build()

    @Transactional
    suspend fun generateAndSaveMessage() {
        // 1. 모든 사이트 조회
        val sites = siteRepository.findAll()

        if (sites.isEmpty()) {
            log.warn { "등록된 사이트가 없습니다." }
            return
        }

        // 2. 현재 시간 기준으로 이전 시간대 계산
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val targetHour = if (currentHour > 0) currentHour - 1 else 23

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // 3. 각 사이트별로 병렬로 메시지 생성
        coroutineScope {
            sites
                .map { site ->
                    async {
                        try {
                            generateMessageForSite(site, yesterday, today, targetHour)
                        } catch (e: Exception) {
                            log.error(e) { "사이트 ${site.name}(ID: ${site.id})의 LLM 메시지 생성 중 오류 발생" }
                        }
                    }
                }.awaitAll()
        }

        log.info { "모든 사이트의 LLM 메시지 생성 완료" }
    }

    private suspend fun generateMessageForSite(
        site: Site,
        yesterday: LocalDate,
        today: LocalDate,
        targetHour: Int,
    ) {
        val siteId = site.requiredId

        // 1. 해당 사이트의 어제와 오늘 평균 온도 조회
        val yesterdayAvgTemp = getHourlyAverageTemperature(yesterday, targetHour, siteId)
        val todayAvgTemp = getHourlyAverageTemperature(today, targetHour, siteId)

        log.info {
            "[${site.name}] 어제 ${targetHour}시~${targetHour + 1}시 평균: $yesterdayAvgTemp°C, " +
                "오늘 ${targetHour}시~${targetHour + 1}시 평균: $todayAvgTemp°C"
        }

        // 2. LLM API 호출을 위한 프롬프트 생성
        val llmRequest =
            LlmRequest(
                yesterday_temp = yesterdayAvgTemp,
                today_temp = todayAvgTemp,
            )

        // 3. 외부 LLM API 호출
        val llmResponse =
            webClient
                .post()
                .uri("/generate/temperature")
                .bodyValue(llmRequest)
                .retrieve()
                .awaitBody<LlmResponse>()

        val generatedMessage = llmResponse.generated_text
        log.info { "[${site.name}] 생성된 LLM 메시지: $generatedMessage" }

        // 4. DB에 저장
        val llmMessage =
            LlmMessage(
                site = site,
                yesterdayAvgTemp = yesterdayAvgTemp,
                todayAvgTemp = todayAvgTemp,
                prompt = llmRequest.toString(),
                message = generatedMessage,
            )

        llmMessageRepository.save(llmMessage)
        log.info { "[${site.name}] LLM 메시지 저장 완료: ID=${llmMessage.id}" }
    }

    private fun getHourlyAverageTemperature(
        date: LocalDate,
        hour: Int,
        siteId: Long,
    ): Double {
        // 해당 날짜의 특정 시간대 (hour:00 ~ hour+1:00)
        val startOfHour = date.atTime(hour, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant()
        val endOfHour = date.atTime(hour + 1, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant()

        // 온습도계 센서 타입 조회
        val temperatureHumiditySensor = SensorType.TEMPERATURE_HUMIDITY
        val temperatureField = DeviceProfileEnum.TEMPERATURE.fieldKey

        // InfluxDB 쿼리 생성 (facilityId로 필터링)
        val query =
            Flux
                .from(influxdbProperties.bucket)
                .range(startOfHour, endOfHour)
                .filter(
                    Restrictions.and(
                        Restrictions.measurement().equal(temperatureHumiditySensor.measureName),
                        Restrictions.field().equal(temperatureField),
                        Restrictions.tag("facilityId").equal(siteId.toString()),
                    ),
                ).mean("_value")
                .toString()

        log.debug { "Temperature query for Site $siteId, $date $hour:00-${hour + 1}:00: $query" }

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
    fun findAll(
        siteId: Long? = null,
        from: String? = null,
        to: String? = null,
    ): List<LlmMessageResponse> {
        // String 날짜를 LocalDateTime으로 파싱
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val startDate = from?.let { LocalDateTime.parse(it, dateFormatter) }
        val endDate = to?.let { LocalDateTime.parse(it, dateFormatter) }

        val messages =
            when {
                // siteId와 날짜 범위가 모두 있는 경우
                siteId != null && startDate != null && endDate != null -> {
                    val site =
                        siteRepository.findByIdOrNull(siteId)
                            ?: throw CustomException(ErrorCode.NOT_FOUND_SITE, siteId)
                    llmMessageRepository.findAllBySiteAndCreatedAtBetween(site, startDate, endDate)
                }
                // siteId만 있는 경우
                siteId != null -> {
                    val site =
                        siteRepository.findByIdOrNull(siteId)
                            ?: throw CustomException(ErrorCode.NOT_FOUND_SITE, siteId)
                    llmMessageRepository.findAllBySite(site)
                }
                // 날짜 범위만 있는 경우
                startDate != null && endDate != null -> {
                    llmMessageRepository.findAllByCreatedAtBetween(startDate, endDate)
                }
                // 필터가 없는 경우
                else -> llmMessageRepository.findAll()
            }

        return messages.map { LlmMessageResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): LlmMessageResponse {
        val message =
            llmMessageRepository.findByIdOrNull(id)
                ?: throw CustomException(ErrorCode.NOT_FOUND_LLM_MESSAGE, id)
        return LlmMessageResponse.from(message)
    }

    data class TemperatureData(
        @Column(name = "_value") val value: Double? = null,
        @Column(name = "_time") val time: Instant? = null,
    )
}

package com.pluxity.aiot.data

import com.pluxity.aiot.abbreviation.Abbreviation
import com.pluxity.aiot.abbreviation.AbbreviationRepository
import com.pluxity.aiot.alarm.dto.SubscriptionCinResponse
import com.pluxity.aiot.alarm.dto.SubscriptionRepListResponse
import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.data.dto.LocationData
import com.pluxity.aiot.data.dto.MobiusBatteryResponse
import com.pluxity.aiot.data.dto.MobiusLocationResponse
import com.pluxity.aiot.data.dto.MobiusUrilResponse
import com.pluxity.aiot.data.dto.SubscriptionM2mSub
import com.pluxity.aiot.data.dto.SubscriptionRequest
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.config.NgrokConfig
import com.pluxity.aiot.global.config.WebClientFactory
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.SubscriptionProperties
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.mobius.MobiusConfigService
import com.pluxity.aiot.system.mobius.MobiusUrlUpdatedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.time.Instant
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@Service
class AiotService(
    private val deviceTypeRepository: DeviceTypeRepository,
    private val featureRepository: FeatureRepository,
    private val abbreviationRepository: AbbreviationRepository,
    private val siteRepository: SiteRepository,
    mobiusConfigService: MobiusConfigService,
    webClientFactory: WebClientFactory,
    private val subscriptionProperties: SubscriptionProperties,
) {
    @Autowired(required = false)
    private val ngrokConfig: NgrokConfig? = null

    @Value("\${spring.profiles.active:local}")
    private val activeProfile: String = ""

    @Value("\${server.port}")
    private val serverPort: String = "8080"

    private var cachedMobiusUrl: String = mobiusConfigService.currentUrl
    private val client: WebClient =
        webClientFactory
            .createClient(cachedMobiusUrl)
            .mutate()
            .defaultHeaders { headers ->
                headers.setAll(createMobiusHeaders())
            }.build()

    @Transactional
    fun checkSynchronization() {
        runBlocking {
            val existFeatures = featureRepository.findAll()
            val existingIds = existFeatures.mapNotNull { it.deviceId }
            val features = fetchAllMobiusSensorPaths(existFeatures)
            val newIds = features.map { it.deviceId }
            val removedIds = existingIds - newIds.toSet()

            featureRepository.deleteAllByDeviceIdIn(removedIds)
            featureRepository.saveAll(features)
        }
    }

    @Transactional
    fun statusSynchronize() {
        runBlocking {
            val features = featureRepository.findAll()
            log.info { "총 ${features.size}개의 Feature 위치 동기화 시작" }

            supervisorScope {
                features
                    .mapNotNull { feature ->
                        async {
                            val deviceId = feature.deviceId
                            try {
                                fetchDeviceLocationData(deviceId)?.let { locationData ->
                                    val batteryLevel = fetchDeviceBatteryData(deviceId)
                                    log.info { "Status 업데이트 성공: $deviceId (${locationData.latitude}, ${locationData.longitude})" }
                                    val site =
                                        siteRepository.findFirstByPointInPolygon(
                                            locationData.longitude,
                                            locationData.latitude,
                                        )
                                    feature.updateStatusInfo(
                                        locationData.longitude,
                                        locationData.latitude,
                                        batteryLevel,
                                        site,
                                    )
                                } ?: log.warn { "위치 데이터 없음: $deviceId" }
                            } catch (e: Exception) {
                                log.error(e) { "위치 데이터 가져오기 실패: $deviceId" }
                            }
                        }
                    }.awaitAll()
            }

            log.info { "위치 동기화 완료" }
        }
    }

    suspend fun fetchDeviceBatteryData(deviceId: String): Int? =
        client
            .get()
            .uri("/$deviceId/3_1.2_0/data-report/la")
            .exchangeToMono { response ->
                if (response.statusCode().is2xxSuccessful) {
                    response.bodyToMono<MobiusBatteryResponse>()
                } else {
                    // 4xx, 5xx 상태코드는 null 반환 (예외 발생 안함)
                    Mono.empty()
                }
            }.awaitSingleOrNull()
            ?.cin
            ?.con
            ?.batteryLevel

    suspend fun fetchDeviceLocationData(deviceId: String): LocationData? {
        val res =
            client
                .get()
                .uri("/$deviceId")
                .retrieve()
                .awaitBody<MobiusLocationResponse>()
        var latitude: Double? = null
        var longitude: Double? = null
        for (str in res.cntResponse.lbl) {
            when {
                str.startsWith("latitude:", ignoreCase = true) -> {
                    latitude = str.substringAfter("latitude:").trim().toDoubleOrNull()
                }

                str.startsWith("longitude:", ignoreCase = true) -> {
                    longitude = str.substringAfter("longitude:").trim().toDoubleOrNull()
                }
            }
            if (latitude != null && longitude != null) break
        }
        return if (latitude != null && longitude != null) {
            LocationData(latitude, longitude)
        } else {
            null
        }
    }

    suspend fun fetchAllMobiusSensorPaths(existFeatures: List<Feature>): List<Feature> {
        val deviceTypes = deviceTypeRepository.findAll()
        val objectIds = deviceTypes.map { it.objectId }
        val res =
            client
                .get()
                .uri("?fu=1&ty=3&lvl=2")
                .retrieve()
                .awaitBody<MobiusUrilResponse>()

        val paths =
            res.uril
                .asSequence()
                .filter { path -> objectIds.any { id -> path.contains(id) } }
                .filter { it.count { char -> char == '/' } == 3 }
                .filterNot { it.contains("3_1.2_0") }
                .filter { !it.contains("P-TST") || it.contains(SensorType.DISPLACEMENT_GAUGE.objectId) }
                .toList()

        val existFeatureMap = existFeatures.associateBy { it.deviceId }

        val ret = mutableMapOf<String, Feature>()
        val abbreviations = abbreviationRepository.findByIsActiveTrue()

        val deviceTypeMap = deviceTypes.associateBy { it.objectId }
        paths.forEach { path ->
            val splitPaths = path.split("/")
            val (deviceId, sensorId) = splitPaths[2] to splitPaths[3]
            val objectId = sensorId.take(5)
            val deviceType = deviceTypeMap[objectId]
            val parsedName = parseDeviceId(deviceId, abbreviations)

            ret[deviceId] = existFeatureMap[deviceId]?.apply {
                updateInfo(deviceType, parsedName, sensorId)
            } ?: Feature(
                deviceType = deviceType,
                deviceId = deviceId,
                name = parsedName,
                objectId = sensorId,
            )
        }
        return ret.values.toList()
    }

    /**
     * deviceId를 파싱하여 deviceName을 생성합니다.
     * deviceId의 -, _, 공백을 -로 치환하고, -로 분리하여 각 부분이 Abbreviation 테이블에 있는지 확인합니다.
     * Abbreviation이 있으면 그 값을 사용, 없으면 해당 단어 제거합니다.
     * 모든 단어가 제거되면 원본 deviceId 그대로 사용합니다.
     * 숫자 부분(식별자)은 유지하여 결과 이름 뒤에 추가합니다.
     */
    fun parseDeviceId(
        deviceId: String,
        abbreviations: List<Abbreviation>,
    ): String {
        // -, _, 공백을 모두 -로 치환
        val normalizedId = deviceId.replace("[\\s_]", "-")

        // -로 분리
        val parts = normalizedId.split("-")

        val result = StringBuilder()
        var hasValidPart = false
        var numericSuffix = ""

        // 약어를 Map으로 변환 (대소문자 무시)
        val abbrMap =
            abbreviations
                .filter { it.isActive }
                .associateBy { it.abbreviationKey.lowercase() }

        // 마지막 부분이 숫자인지 확인하여 식별자로 저장
        if (parts.isNotEmpty()) {
            val lastPart = parts[parts.size - 1]
            if (lastPart.matches("\\d+".toRegex())) {
                numericSuffix = lastPart
            }
        }

        parts
            .asSequence()
            .filter { it.isNotEmpty() && !it.matches("\\d+".toRegex()) }
            .mapNotNull { abbrMap[it.lowercase()] }
            .firstOrNull()
            ?.let { abbr ->
                if (result.isNotEmpty()) result.append(" ")
                result.append(abbr.fullName)
                hasValidPart = true
            }

        // 모든 부분이 제거되었으면 원본 deviceId 사용
        if (!hasValidPart) {
            return deviceId
        }

        // 숫자 접미사가 있으면 결과에 추가
        if (!numericSuffix.isEmpty()) {
            result.append("-").append(numericSuffix)
        }
        return result.toString()
    }

    private fun createMobiusHeaders(): Map<String, String> =
        mapOf(
            "X-M2M-RI" to Instant.now().epochSecond.toString(),
            "X-M2M-Origin" to "S_AIoT_Application",
            "Accept" to "*/*",
        )

    fun updateFeatureSubscriptionTime(deviceId: String) {
        val feature =
            featureRepository.findByDeviceId(deviceId) ?: throw CustomException(ErrorCode.NOT_FOUND_FEATURE_BY_DEVICE_ID, deviceId)
        // 구독 시간 업데이트
        feature.updateSubscriptionTime(LocalDateTime.now())
        log.info { "Updated subscription time for Feature $deviceId" }
    }

    private fun fetchSubscription(
        uri: String,
        body: SubscriptionRequest,
        subscriptionName: String,
        deviceId: String,
    ) {
        client
            .post()
            .uri(uri)
            .header("Content-Type", "application/json;ty=23")
            .bodyValue(body)
            .exchangeToMono { response ->
                val isSuccess = response.statusCode().is2xxSuccessful
                response.bodyToMono<String>().doOnNext { body ->
                    log.info { "'$uri/$subscriptionName' Subscribe Result(${response.statusCode()}) : '$body'" }
                    if (isSuccess) {
                        updateFeatureSubscriptionTime(deviceId)
                    }
                }
            }.block()
    }

    /**
     * 지정된 Feature에 대한 구독을 설정합니다.
     */
    private fun setupSubscriptionForFeature(
        deviceId: String,
        objectId: String,
        pluxityUrl: String,
    ) {
        log.info { "Setting up subscription for Feature $deviceId using URL: $pluxityUrl" }
        val subscriptionName = "$activeProfile-$deviceId-$objectId"
        val subscriptionBody =
            SubscriptionRequest(
                SubscriptionM2mSub(
                    rn = subscriptionName,
                    nu = listOf("$pluxityUrl/subscription"),
                ),
            )

        val subscriptionUrl = "/$deviceId/$objectId/data-report"

        try {
            fetchSubscription(subscriptionUrl, subscriptionBody, subscriptionName, deviceId)
        } catch (e: WebClientResponseException) {
            if (e.statusCode.value() == 409 && e.responseBodyAsString.contains("resource is already exist")) {
                log.info { "Subscription '$subscriptionName' already exists. Attempting to remove and recreate." }

                try {
                    fetchRemoveSubscription(deviceId, objectId, subscriptionName)
                    // 새로운 구독 생성
                    fetchSubscription(subscriptionUrl, subscriptionBody, subscriptionName, deviceId)
                } catch (retryEx: Exception) {
                    log.error { "Error recreating subscription '$subscriptionName' for Feature $deviceId / $objectId: ${retryEx.message}" }
                }
            } else {
                log.error { "Error setting up subscription '$subscriptionName' for Feature $deviceId / $objectId: ${e.message}" }
            }
        } catch (e: Exception) {
            log.error { "Error setting up subscription '$subscriptionName' for Feature $deviceId / $objectId: ${e.message}" }
        }
    }

    fun subscription() {
        val activeFeatures = featureRepository.findByIsActiveTrueAndSiteIsNotNull()
        val subscriptionUrl = getSubscriptionUrl()
        log.info { "Setting up subscriptions for active Features using URL: $subscriptionUrl" }

        activeFeatures.forEach { feature ->
            setupSubscriptionForFeature(feature.deviceId, feature.objectId, subscriptionUrl)
        }
    }

    suspend fun findByDateRange(
        deviceId: String,
        objectId: String,
        startStr: String,
        endStr: String,
    ): List<SubscriptionCinResponse>? {
        val res =
            client
                .get()
                .uri("/$deviceId/$objectId/data-report?rcn=4&ty=4&lvl=1&cra=$startStr&crb=$endStr")
                .retrieve()
                .awaitBody<SubscriptionRepListResponse>()
        return res.cin
    }

    /**
     * 프로파일에 따른 구독 URL을 반환합니다.
     */
    private fun getSubscriptionUrl(): String {
        val ipv4 = getLocalIpv4()
        return when (activeProfile) {
            "local" -> {
                ngrokConfig?.getNgrokUrl().also {
                    log.info { "Using Ngrok URL for local profile: $it" }
                } ?: ""
            }
            else -> {
                subscriptionProperties.url
                    .takeIf { it.isNotBlank() }
                    ?.also { log.info { "Using configured subscription URL: $it" } }
                    ?: "http://$ipv4:$serverPort".also {
                        log.info { "Using localhost URL for non-local profile: $it" }
                    }
            }
        }
    }

    private fun getLocalIpv4(): String =
        try {
            NetworkInterface
                .getNetworkInterfaces()
                .asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .filterNot { it.isLoopbackAddress }
                .filterIsInstance<Inet4Address>()
                .map { it.hostAddress }
                .firstOrNull()
                ?: ""
        } catch (e: SocketException) {
            log.error(e) { "IPv4 주소 조회 실패" }
            ""
        }

    private fun fetchRemoveSubscription(
        deviceId: String,
        objectId: String,
        subscriptionName: String,
    ) {
        client
            .delete()
            .uri("/$deviceId/$objectId/data-report/$activeProfile-$deviceId-$objectId")
            .exchangeToMono { response ->
                val isSuccess = response.statusCode().is2xxSuccessful
                response.bodyToMono<String>().doOnNext { body ->
                    log.info { "'$subscriptionName' Subscribe Result(${response.statusCode()}) : '$body'" }
                    if (isSuccess) {
                        updateFeatureSubscriptionTime(deviceId)
                    }
                }
            }.block()
    }

    fun removeAllSubscriptions() {
        val targetFeatures = featureRepository.findAll()
        log.info { "Removing subscriptions for ${targetFeatures.size} Features" }
        for (feature in targetFeatures) {
            // 기존 구독 삭제
            fetchRemoveSubscription(feature.deviceId, feature.objectId, "$activeProfile-${feature.deviceId}-${feature.objectId}")
        }
    }

    @EventListener
    @Transactional
    fun handleMobiusUrlUpdated(event: MobiusUrlUpdatedEvent) {
        this.cachedMobiusUrl = event.newUrl
        checkSynchronization()
        statusSynchronize()
        subscription()
    }
}

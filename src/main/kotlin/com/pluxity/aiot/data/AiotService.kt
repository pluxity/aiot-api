package com.pluxity.aiot.data

import com.pluxity.aiot.data.dto.DeviceStatus
import com.pluxity.aiot.data.dto.LocationData
import com.pluxity.aiot.data.dto.MobiusBatteryResponse
import com.pluxity.aiot.data.dto.MobiusLocationResponse
import com.pluxity.aiot.data.dto.MobiusUrilResponse
import com.pluxity.aiot.data.dto.SubscriptionM2mSub
import com.pluxity.aiot.data.dto.SubscriptionRequest
import com.pluxity.aiot.data.subscription.dto.SubscriptionCinResponse
import com.pluxity.aiot.data.subscription.dto.SubscriptionRepListResponse
import com.pluxity.aiot.feature.FeatureQueryService
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.feature.FeatureStatusWriter
import com.pluxity.aiot.feature.parseDeviceName
import com.pluxity.aiot.global.config.NgrokConfig
import com.pluxity.aiot.global.config.RestClientFactory
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.ServerDomainProperties
import com.pluxity.aiot.mobius.MobiusConfigService
import com.pluxity.aiot.mobius.MobiusUrlUpdatedEvent
import com.pluxity.aiot.sensor.type.AbbreviationData
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

private val log = KotlinLogging.logger {}

@Service
class AiotService(
    private val featureRepository: FeatureRepository,
    private val featureQueryService: FeatureQueryService,
    private val featureStatusWriter: FeatureStatusWriter,
    mobiusConfigService: MobiusConfigService,
    private val restClientFactory: RestClientFactory,
    private val serverDomainProperties: ServerDomainProperties,
) {
    @Autowired(required = false)
    private val ngrokConfig: NgrokConfig? = null

    @Value($$"${spring.profiles.active:local}")
    private val activeProfile: String = ""

    @Value($$"${server.port}")
    private val serverPort: String = "8080"

    /** 주소 변경 이벤트 스레드가 갈아끼우고 요청 스레드가 읽는다. */
    @Volatile
    private var client: RestClient = createMobiusClient(mobiusConfigService.currentUrl)

    private fun createMobiusClient(baseUrl: String): RestClient =
        restClientFactory
            .createClient(baseUrl)
            .mutate()
            .defaultHeaders { headers ->
                headers.setAll(createMobiusHeaders())
            }.build()

    /**
     * Reactor Netty가 호스트당 커넥션 풀로 걸고 있던 상한을 이어받는다.
     * JDK HttpClient에는 대응 설정이 없어 그냥 옮기면 이 상한이 사라진다.
     */
    private val mobiusSemaphore = Semaphore(maxOf(Runtime.getRuntime().availableProcessors(), 8) * 2)

    /**
     * 실제로 client를 부르는 곳에서만 쓴다.
     * fan-out 쪽에서 잡은 채 그 안에서 또 잡으면 자기를 기다리는 데드락이 된다.
     */
    private fun <T> mobiusLimiter(block: () -> T): T {
        mobiusSemaphore.acquire()
        return try {
            block()
        } finally {
            mobiusSemaphore.release()
        }
    }

    /**
     * 트랜잭션을 붙이지 않는다. HTTP를 트랜잭션 안에서 돌면 응답을 기다리는 내내
     * JDBC 커넥션을 쥐고 있게 된다.
     */
    fun checkSynchronization() {
        featureStatusWriter.applyPaths(fetchMobiusUril())
    }

    fun statusSynchronize() {
        val deviceIds = featureQueryService.findAllDeviceIds()
        log.info { "총 ${deviceIds.size}개의 Feature 위치 동기화 시작" }

        featureStatusWriter.applyStatuses(fetchAllStatuses(deviceIds))

        log.info { "위치 동기화 완료" }
    }

    /** 좌표가 없는 기기는 갱신할 것이 없어 뺀다. */
    private fun fetchAllStatuses(deviceIds: List<String>): Map<String, DeviceStatus> =
        fanOut(deviceIds, "위치") { deviceId ->
            fetchDeviceLocationData(deviceId)?.let { locationData ->
                val batteryLevel = fetchDeviceBatteryData(deviceId)
                log.info { "Status 업데이트 성공: $deviceId (${locationData.latitude}, ${locationData.longitude})" }
                DeviceStatus(locationData.longitude, locationData.latitude, batteryLevel)
            } ?: run {
                log.warn { "위치 데이터 없음: $deviceId" }
                null
            }
        }.filterValues { it != null }
            .mapValues { (_, status) -> status!! }

    /**
     * 값이 없는 기기도 null로 실어 보낸다. 빼버리면 옛 수치가 그대로 남는다.
     * 요청 자체가 실패한 기기만 빠진다 — 못 받은 것을 null로 반영하면 멀쩡한 값을 지운다.
     */
    fun fetchAllBatteryLevels(deviceIds: List<String>): Map<String, Int?> = fanOut(deviceIds, "배터리") { fetchDeviceBatteryData(it) }

    /**
     * 트랜잭션 밖에서 돈다.
     *
     * 완료된 요청만 담고 값이 없으면 null로 남긴다. 실패한 요청은 아예 빼서
     * 호출자가 "값 없음"과 "못 받음"을 구분할 수 있게 한다.
     * 개별 실패가 전체를 멈추지 않도록 삼키고 로그만 남긴다.
     */
    private fun <T : Any> fanOut(
        deviceIds: List<String>,
        label: String,
        fetch: (String) -> T?,
    ): Map<String, T?> =
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            deviceIds
                .map { deviceId ->
                    executor.submit<Pair<String, T?>?> {
                        try {
                            deviceId to fetch(deviceId)
                        } catch (e: Exception) {
                            log.error(e) { "$label 데이터 가져오기 실패: $deviceId" }
                            null
                        }
                    }
                }.mapNotNull { it.get() }
                .toMap()
        }

    /** 비-2xx는 예외 없이 null이다. 배터리 동기화 순회가 첫 실패에서 끊기면 안 된다. */
    fun fetchDeviceBatteryData(deviceId: String): Int? =
        mobiusLimiter {
            client
                .get()
                .uri("/$deviceId/3_1.2_0/data-report/la")
                .exchange { _, response ->
                    if (!response.statusCode.is2xxSuccessful) {
                        null
                    } else {
                        response.bodyTo(MobiusBatteryResponse::class.java)
                    }
                }?.cin
                ?.con
                ?.batteryLevel
        }

    fun fetchDeviceLocationData(deviceId: String): LocationData? {
        val labels =
            mobiusLimiter {
                client
                    .get()
                    .uri("/$deviceId")
                    .retrieve()
                    .body<MobiusLocationResponse>()
                    ?: throw CustomException(ErrorCode.MOBIUS_EMPTY_RESPONSE)
            }.cntResponse
                .lbl

        val coordinates =
            labels
                .mapNotNull { label ->
                    label.split(":", limit = 2).takeIf { it.size == 2 }?.run {
                        val (k, v) = this
                        v.trim().toDoubleOrNull()?.let { k.lowercase() to it }
                    }
                }.toMap()

        val latitude = coordinates["latitude"]
        val longitude = coordinates["longitude"]

        return if (latitude != null && longitude != null) LocationData(latitude, longitude) else null
    }

    /**
     * 빈 응답을 emptyList()로 흘리면 안 된다.
     * 뒤 단계가 "Mobius에 아무것도 없다"로 읽어 로컬 Feature를 전량 삭제한다.
     * 삭제는 되돌릴 수 없지만 실패는 다음 주기에 재시도된다.
     */
    private fun fetchMobiusUril(): List<String> =
        mobiusLimiter {
            client
                .get()
                .uri("?fu=1&ty=3&lvl=2")
                .retrieve()
                .body<MobiusUrilResponse>()
                ?.uril
                ?: throw CustomException(ErrorCode.MOBIUS_EMPTY_RESPONSE)
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
        abbrMap: Map<String, AbbreviationData>,
    ): String = parseDeviceName(deviceId, abbrMap)

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
        featureRepository.save(feature)
        log.info { "Updated subscription time for Feature $deviceId" }
    }

    private fun fetchSubscription(
        uri: String,
        body: SubscriptionRequest,
        subscriptionName: String,
        deviceId: String,
    ) {
        val respBody =
            mobiusLimiter {
                client
                    .post()
                    .uri(uri)
                    .header("Content-Type", "application/json;ty=23")
                    .body(body)
                    .retrieve() // 비-2xx면 RestClientResponseException을 던진다. 409 재시도 경로가 이걸 받는다
                    .body(String::class.java)
            }
        log.info { "'$uri/$subscriptionName' Subscribe Result : '$respBody'" }
        updateFeatureSubscriptionTime(deviceId)
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
        } catch (e: RestClientResponseException) {
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

    fun findByDateRange(
        deviceId: String,
        objectId: String,
        startStr: String,
        endStr: String,
    ): List<SubscriptionCinResponse>? =
        mobiusLimiter {
            client
                .get()
                .uri("/$deviceId/$objectId/data-report?rcn=4&ty=4&lvl=1&cra=$startStr&crb=$endStr")
                .retrieve()
                .body<SubscriptionRepListResponse>()
                ?.cin
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
                serverDomainProperties.url
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
        // 실패해도 본문을 읽어 로깅하고 계속 간다. 순회가 첫 실패에서 끊기면 안 된다
        val succeeded =
            mobiusLimiter {
                client
                    .delete()
                    .uri("/$deviceId/$objectId/data-report/$activeProfile-$deviceId-$objectId")
                    .exchange { _, response ->
                        val body = response.bodyTo(String::class.java)
                        log.info { "'$subscriptionName' Subscribe Result(${response.statusCode}) : '$body'" }
                        response.statusCode.is2xxSuccessful
                    }
            }
        if (succeeded == true) {
            updateFeatureSubscriptionTime(deviceId)
        }
    }

    fun removeAllSubscriptions() {
        val targetFeatures = featureRepository.findAll()
        log.info { "Removing subscriptions for ${targetFeatures.size} Features" }
        for (feature in targetFeatures) {
            // 기존 구독 삭제
            fetchRemoveSubscription(feature.deviceId, feature.objectId, "$activeProfile-${feature.deviceId}-${feature.objectId}")
        }
    }

    /** 문자열만 갈아끼우면 client가 생성 시점 주소에 묶여 옛 서버로 계속 동기화한다. */
    @EventListener
    fun handleMobiusUrlUpdated(event: MobiusUrlUpdatedEvent) {
        this.client = createMobiusClient(event.newUrl)
        checkSynchronization()
        statusSynchronize()
        subscription()
    }
}

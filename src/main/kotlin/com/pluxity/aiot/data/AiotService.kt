package com.pluxity.aiot.data

import com.pluxity.aiot.abbreviation.AbbreviationRepository
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.config.WebClientFactory
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.mobius.MobiusConfigService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Instant

private val log = KotlinLogging.logger {}

@Service
class AiotService(
    private val deviceTypeRepository: DeviceTypeRepository,
    private val featureRepository: FeatureRepository,
    private val abbreviationRepository: AbbreviationRepository,
    mobiusConfigService: MobiusConfigService,
    webClientFactory: WebClientFactory,
) {
    private val cachedMobiusUrl: String = mobiusConfigService.currentUrl
    private val client: WebClient =
        webClientFactory
            .createClient(cachedMobiusUrl)
            .mutate()
            .defaultHeaders { headers ->
                headers.setAll(createMobiusHeaders())
            }.build()

    init {
        System.currentTimeMillis()
    }

    @Transactional
    fun checkSynchronization() {
        runBlocking {
            val existFeatures = featureRepository.findAll()
            val existingIds = existFeatures.mapNotNull { it.deviceId }
            val features = fetchAllMobiusSensorPaths(existFeatures)
            val newIds = features.mapNotNull { it.deviceId }
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
                        feature.deviceId?.let { deviceId ->
                            async {
                                try {
                                    val data = fetchDeviceLocationData(deviceId)
                                    data?.let {
                                        val batteryLevel = fetchDeviceBatteryData(deviceId)
                                        log.info { "Status 업데이트 성공: $deviceId (${it.latitude}, ${it.longitude})" }
                                        feature.updateStatusInfo(it.longitude, it.latitude, batteryLevel)
                                    } ?: log.warn { "위치 데이터 없음: $deviceId" }
                                } catch (e: Exception) {
                                    log.error(e) { "위치 데이터 가져오기 실패: $deviceId" }
                                }
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
            .uri("$cachedMobiusUrl/$deviceId/3_1.2_0/data-report/la")
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
                .uri("$cachedMobiusUrl/$deviceId")
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
                .uri("$cachedMobiusUrl?fu=1&ty=3&lvl=2")
                .retrieve()
                .awaitBody<MobiusUrilResponse>()

        val paths =
            res.uril
                .asSequence()
                .filter { path -> objectIds.any { id -> path.contains(id!!) } }
                .filter { it.count { char -> char == '/' } == 3 }
                .filterNot { it.contains("3_1.2_0") }
                .filter { !it.contains("P-TST") || it.contains("34957") }
                .toList()

        val existFeatureMap = existFeatures.associateBy { it.deviceId }

        val ret = mutableMapOf<String, Feature>()

        for (path in paths) {
            val splitPaths = path.split("/")
            val deviceId = splitPaths[2]
            val sensorId = splitPaths[3]
            val objectId = sensorId.take(5)
            val deviceType = deviceTypes.firstOrNull { it.objectId == objectId }
            val parsedName = parseDeviceId(deviceId)

            ret[deviceId] = existFeatureMap[deviceId]?.apply {
                updateInfo(deviceType, parsedName, objectId)
            } ?: Feature(deviceType = deviceType, deviceId = deviceId, name = parsedName, objectId = objectId)
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
    fun parseDeviceId(deviceId: String): String {
        val abbreviations = abbreviationRepository.findByIsActiveTrue()
        // -, _, 공백을 모두 -로 치환
        val normalizedId = deviceId.replace("[\\s_]".toRegex(), "-")

        // -로 분리
        val parts = normalizedId.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

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

        for (part in parts) {
            if (part.isEmpty()) continue

            // 숫자로만 구성된 부분은 처리하지 않고 건너뜀 (나중에 접미사로 추가)
            if (part.matches("\\d+".toRegex())) continue

            // 해당 부분이 약어 테이블에 있는지 확인
            val abbr = abbrMap[part.lowercase()]
            if (abbr != null) {
                // 약어를 찾았으면 전체 이름 사용
                if (!result.isEmpty()) result.append(" ")
                result.append(abbr.fullName)
                hasValidPart = true
            }
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
}

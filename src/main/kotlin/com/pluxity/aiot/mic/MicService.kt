package com.pluxity.aiot.mic

import com.pluxity.aiot.mic.dto.MicInfo
import com.pluxity.aiot.mic.dto.MicResponse
import com.pluxity.aiot.mic.dto.toResponse
import com.pluxity.aiot.site.SiteRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
@ConditionalOnProperty("mic.enabled", havingValue = "true")
@Transactional(readOnly = true)
class MicService(
    private val micRepository: MicRepository,
    private val siteRepository: SiteRepository,
) {
    fun findAll(): List<MicResponse> = micRepository.findAllWithSite().map { it.toResponse() }

    @Transactional
    fun sync(micInfos: List<MicInfo>) {
        val vendorMicIds = micInfos.map { it.id }.toSet()
        val existingMics = micRepository.findAllWithSite()
        val existingMap = existingMics.associateBy { it.vendorMicId }

        var created = 0
        var updated = 0
        var disconnected = 0

        for (micInfo in micInfos) {
            val existing = existingMap[micInfo.id]
            if (existing != null) {
                if (existing.updateFromVendor(micInfo, findSite(micInfo))) updated++
            } else {
                createMic(micInfo)
                created++
            }
        }

        for (mic in existingMics) {
            if (mic.vendorMicId !in vendorMicIds && mic.status != MicStatus.DISCONNECTED) {
                mic.disconnect()
                disconnected++
            }
        }

        log.info { "AI 마이크 동기화 완료: 신규=$created, 업데이트=$updated, 미연동처리=$disconnected" }
    }

    private fun createMic(micInfo: MicInfo) {
        val mic = Mic(vendorMicId = micInfo.id)
        mic.updateFromVendor(micInfo, findSite(micInfo))
        micRepository.save(mic)
    }

    private fun findSite(micInfo: MicInfo) =
        micInfo.location?.let { location ->
            val longitude = location.longitude
            val latitude = location.latitude
            if (longitude != null && latitude != null) {
                siteRepository.findFirstByPointInPolygon(longitude, latitude)
            } else {
                null
            }
        }
}

package com.pluxity.aiot.eds

import com.pluxity.aiot.cctv.Cctv
import com.pluxity.aiot.cctv.repository.CctvRepository
import com.pluxity.aiot.eds.dto.EdsCameraInfo
import com.pluxity.aiot.site.SiteRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsCameraSyncService(
    private val edsClient: EdsClient,
    private val cctvRepository: CctvRepository,
    private val siteRepository: SiteRepository,
) {
    @Transactional
    fun sync() {
        val edsCameras = edsClient.getCameraList()
        val edsCameraIds = edsCameras.map { it.cameraId }.toSet()
        val existingCctvs = cctvRepository.findAll()
        val existingMap = existingCctvs.associateBy { it.edsCameraId }

        var created = 0
        var updated = 0
        var deactivated = 0

        for (edsCamera in edsCameras) {
            val existing = existingMap[edsCamera.cameraId]
            if (existing != null) {
                if (updateCctv(existing, edsCamera)) updated++
            } else {
                createCctv(edsCamera)
                created++
            }
        }

        for (cctv in existingCctvs) {
            if (cctv.edsCameraId !in edsCameraIds && cctv.cameraStatus != EdsCameraStatus.DISCONNECTED) {
                cctv.cameraStatus = EdsCameraStatus.DISCONNECTED
                deactivated++
            }
        }

        log.info { "EDS 카메라 동기화 완료: 신규=$created, 업데이트=$updated, 미연동처리=$deactivated" }
    }

    private fun createCctv(edsCamera: EdsCameraInfo) {
        val site = findSite(edsCamera.longitude, edsCamera.latitude)
        val cctv = Cctv(
            edsCameraId = edsCamera.cameraId,
        )
        cctv.updateFromEds(edsCamera, site)
        cctvRepository.save(cctv)
    }

    private fun updateCctv(cctv: Cctv, edsCamera: EdsCameraInfo): Boolean {
        val site = findSite(edsCamera.longitude, edsCamera.latitude)
        return cctv.updateFromEds(edsCamera, site)
    }

    private fun findSite(longitude: Double?, latitude: Double?) =
        if (longitude != null && latitude != null) {
            siteRepository.findFirstByPointInPolygon(longitude, latitude)
        } else {
            null
        }
}

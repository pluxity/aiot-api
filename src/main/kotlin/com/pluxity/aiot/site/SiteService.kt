package com.pluxity.aiot.site

import com.pluxity.aiot.file.extensions.getFileMapById
import com.pluxity.aiot.file.service.FileService
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.constant.ErrorCode.NOT_FOUND_SITE
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.site.dto.SiteRequest
import com.pluxity.aiot.site.dto.SiteResponse
import com.pluxity.aiot.site.dto.toSiteResponse
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.WKTReader
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SiteService(
    private val siteRepository: SiteRepository,
    private val fileService: FileService,
) {
    companion object {
        private const val SITE_PATH: String = "sites/"
    }

    private val gf = GeometryFactory(PrecisionModel(), 4326)
    private val wktReader = WKTReader(gf)

    @Transactional
    fun save(request: SiteRequest): Long {
        val location = parsePolygon(request.location)
        val site =
            siteRepository.save(
                Site(
                    name = request.name,
                    description = request.description,
                    location = location,
                    thumbnailId = request.thumbnailId,
                ),
            )

        request.thumbnailId?.let { fileId ->
            fileService.finalizeUpload(fileId, "${SITE_PATH}${site.requiredId}/")
        }

        return site.requiredId
    }

    fun findAll(): List<SiteResponse> {
        val sites = siteRepository.findAllByOrderByCreatedAtDesc()
        val fileMap = fileService.getFileMapById(sites) { it.thumbnailId }
        return sites.map { it.toSiteResponse(fileMap) }
    }

    fun findByIdResponse(id: Long): SiteResponse {
        val site = findById(id)
        val fileMap = fileService.getFileMapById(listOf(site)) { it.thumbnailId }
        return site.toSiteResponse(fileMap)
    }

    @Transactional
    fun putUpdate(
        id: Long,
        request: SiteRequest,
    ) {
        val site = findById(id)
        val location = parsePolygon(request.location)
        site.updateName(request.name)
        site.updateDescription(request.description)
        site.updateLocation(location)

        if (request.thumbnailId != site.thumbnailId) {
            request.thumbnailId?.let { thumbnailId ->
                fileService.finalizeUpload(thumbnailId, "${SITE_PATH}${site.requiredId}/")
                site.updateThumbnailId(request.thumbnailId)
            } ?: site.updateThumbnailId(null)
        }
    }

    @Transactional
    fun delete(id: Long) {
        val site = findById(id)
        siteRepository.delete(site)
    }

    private fun parsePolygon(wkt: String): Polygon {
        val location = wktReader.read(wkt)
        if (location !is Polygon) {
            throw CustomException(ErrorCode.INVALID_LOCATION)
        }
        return location
    }

    private fun findById(id: Long): Site =
        siteRepository
            .findByIdOrNull(id)
            ?: throw CustomException(NOT_FOUND_SITE, id)

    fun findByIds(ids: List<Long>): List<Site> {
        val sites = siteRepository.findAllById(ids)
        val foundSiteIds = sites.map { it.id }
        val missingSiteIds = ids.filter { it !in foundSiteIds }

        if (missingSiteIds.isNotEmpty()) {
            throw CustomException(NOT_FOUND_SITE, missingSiteIds)
        }

        return sites
    }
}

package com.pluxity.aiot.facility

import com.pluxity.aiot.facility.dto.FacilityRequest
import com.pluxity.aiot.facility.dto.FacilityResponse
import com.pluxity.aiot.facility.dto.toFacilityResponse
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.constant.ErrorCode.NOT_FOUND_FACILITY
import com.pluxity.aiot.global.exception.CustomException
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.WKTReader
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FacilityService(
    private val facilityRepository: FacilityRepository,
) {
    private val gf = GeometryFactory(PrecisionModel(), 4326)
    private val wktReader = WKTReader(gf)

    @Transactional
    fun save(request: FacilityRequest): Long {
        val location = parsePolygon(request.location)
        val facility =
            Facility(
                name = request.name,
                description = request.description,
                location = location,
            )
        return facilityRepository.save(facility).id!!
    }

    @Transactional(readOnly = true)
    fun findAll(): List<FacilityResponse> = facilityRepository.findAllByOrderByCreatedAtDesc().map { it.toFacilityResponse() }

    @Transactional(readOnly = true)
    fun findByIdResponse(id: Long): FacilityResponse = findById(id).toFacilityResponse()

    @Transactional
    fun putUpdate(
        id: Long,
        request: FacilityRequest,
    ) {
        val facility = findById(id)
        val location = parsePolygon(request.location)
        facility.updateName(request.name)
        facility.updateDescription(request.description)
        facility.updateLocation(location)
    }

    @Transactional
    fun deleteFacility(id: Long) {
        val facility = findById(id)
        facilityRepository.delete(facility)
    }

    private fun parsePolygon(wkt: String): Polygon {
        val location = wktReader.read(wkt)
        if (location !is Polygon) {
            throw CustomException(ErrorCode.INVALID_LOCATION)
        }
        return location
    }

    private fun findById(id: Long): Facility =
        facilityRepository
            .findByIdOrNull(id)
            ?: throw CustomException(NOT_FOUND_FACILITY, id)
}

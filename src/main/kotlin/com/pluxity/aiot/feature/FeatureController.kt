package com.pluxity.aiot.feature

import com.pluxity.aiot.feature.dto.FeatureResponse
import com.pluxity.aiot.feature.dto.FeatureSearchCondition
import com.pluxity.aiot.global.response.DataResponseBody
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/features")
class FeatureController(
    private val featureService: FeatureService,
) {
    @GetMapping
    fun findAll(searchCondition: FeatureSearchCondition): ResponseEntity<DataResponseBody<List<FeatureResponse>>> =
        ResponseEntity.ok(DataResponseBody(featureService.findAll(searchCondition)))
}

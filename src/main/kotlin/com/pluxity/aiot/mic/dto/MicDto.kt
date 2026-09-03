package com.pluxity.aiot.mic.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MicPageResult<T>(
    val page: Int = 1,
    val size: Int = 0,
    @field:JsonProperty("total_cnt")
    val totalCount: Int = 0,
    @field:JsonProperty("max_page")
    val maxPage: Int = 0,
    val results: List<T> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MicInfo(
    val id: String,
    val name: String? = null,
    val host: String? = null,
    @field:JsonProperty("edge_id")
    val edgeId: String? = null,
    val status: String? = null,
    /** 키가 이벤트 카테고리로 가변이라 Map으로 받는다 */
    val thresholds: Map<String, MicThreshold>? = null,
    val location: MicLocation? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MicThreshold(
    @field:JsonProperty("sound_level_ge")
    val soundLevelGe: Double? = null,
    val confidence: Double? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MicLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
)

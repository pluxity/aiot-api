package com.pluxity.aiot.mic.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MicEventData(
    val id: String,
    val label: MicEventLabel? = null,
    val confidence: Double? = null,
    val mic: MicEventSource? = null,
    @field:JsonProperty("created_at")
    val createdAt: String? = null,
    val noises: List<Double>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MicEventLabel(
    val id: String? = null,
    val name: MicEventLabelName? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MicEventLabelName(
    val ko: String? = null,
    val en: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MicEventSource(
    val id: String,
    val name: String? = null,
    val location: MicLocation? = null,
)

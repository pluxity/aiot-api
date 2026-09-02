package com.pluxity.aiot.data.dto

import com.fasterxml.jackson.annotation.JsonInclude

data class DataResponse(
    val meta: MetaData,
    val timestamp: String,
    val metrics: Map<String, MetricData>,
)

data class MetricData(
    val unit: String,
    val value: Double,
    /** 비트 마스크처럼 값 자체로는 의미를 알 수 없는 항목의 해석 결과 (해당 없으면 미포함) */
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val causes: List<String>? = null,
)

data class MetaData(
    val targetId: String,
    val query: QueryInfo,
)

data class QueryInfo(
    val metrics: List<String>,
)

data class ListDataResponse(
    val meta: ListMetaData,
    val timestamps: List<String>,
    val metrics: Map<String, ListMetricData>,
)

data class ListMetaData(
    val targetId: String,
    val query: ListQueryInfo,
)

data class ListQueryInfo(
    val timeUnit: String,
    val from: String,
    val to: String,
    val metrics: List<String>,
)

data class ListMetricData(
    val unit: String,
    val values: List<Double?>,
)

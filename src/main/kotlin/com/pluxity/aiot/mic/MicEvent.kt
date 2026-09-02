package com.pluxity.aiot.mic

import com.pluxity.aiot.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(indexes = [Index(columnList = "micId"), Index(columnList = "occurredAt")])
class MicEvent(
    /** 벤더가 발급한 이벤트 식별자 */
    @Column(unique = true, nullable = false)
    var eventId: String,
    @Column(nullable = false)
    var micId: String,
    var micName: String? = null,
    var labelId: String? = null,
    var labelNameKo: String? = null,
    var labelNameEn: String? = null,
    var confidence: Double? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    /** 이벤트 구간의 소음 측정 원본 배열 */
    @JdbcTypeCode(SqlTypes.JSON)
    var noises: List<Double>? = null,
    var maxNoise: Double? = null,
    var avgNoise: Double? = null,
    @Column(nullable = false)
    var occurredAt: LocalDateTime,
) : BaseEntity()

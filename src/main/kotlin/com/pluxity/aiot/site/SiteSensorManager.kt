package com.pluxity.aiot.site

import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/** 현장의 센서 카테고리별 이벤트 담당자 */
@Entity
@Table(
    name = "site_sensor_manager",
    uniqueConstraints = [UniqueConstraint(columnNames = ["site_id", "sensor_type", "user_id"])],
)
class SiteSensorManager(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    var site: Site,
    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 50)
    var sensorType: SensorType,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,
) : BaseEntity()

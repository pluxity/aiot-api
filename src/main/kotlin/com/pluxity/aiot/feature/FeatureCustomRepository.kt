package com.pluxity.aiot.feature

import com.pluxity.aiot.dashboard.SensorStatisticsRaw
import com.pluxity.aiot.feature.dto.FeatureSearchCondition

interface FeatureCustomRepository {
    fun findAllBySearchCondition(searchCondition: FeatureSearchCondition?): List<Feature>

    fun findSensorStatisticsBySiteIds(siteIds: List<Long>): List<SensorStatisticsRaw>
}

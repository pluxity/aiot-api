package com.pluxity.aiot.dashboard

import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.Site
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val featureRepository: FeatureRepository,
) {
    fun getSensorSummary(): List<SensorSummary> =
        featureRepository
            .findAll {
                selectNew<SensorStatisticsRaw>(
                    path(Site::id),
                    path(Site::name),
                    count(path(Feature::id)),
                    count(
                        caseWhen(
                            path(Feature::eventStatus)
                                .eq("DISCONNECTED"),
                        ).then(1),
                    ),
                    count(
                        caseWhen(
                            path(Feature::eventStatus)
                                .notEqual("DISCONNECTED"),
                        ).then(1),
                    ),
                    count(
                        caseWhen(
                            path(Feature::objectId)
                                .like("${SensorType.TEMPERATURE_HUMIDITY.objectId}%"),
                        ).then(1),
                    ),
                    count(
                        caseWhen(
                            path(Feature::objectId)
                                .like("${SensorType.FIRE.objectId}%"),
                        ).then(1),
                    ),
                    count(
                        caseWhen(
                            path(Feature::objectId)
                                .like("${SensorType.DISPLACEMENT_GAUGE.objectId}%"),
                        ).then(1),
                    ),
                ).from(
                    entity(Feature::class),
                    join(Feature::site),
                ).where(
                    path(Site::id).isNotNull(),
                ).groupBy(path(Site::id))
            }.filterNotNull()
            .map { it.toSummaryStatistics() }

    private data class SensorStatisticsRaw(
        val siteId: Long,
        val siteName: String,
        val totalCount: Long,
        val disconnectedCount: Long,
        val connectedCount: Long,
        val temperatureHumidityCount: Long,
        val fireCount: Long,
        val displacementCount: Long,
    )

    private fun SensorStatisticsRaw.toSummaryStatistics() =
        SensorSummary(
            siteId = this.siteId,
            siteName = this.siteName,
            totalSensors = this.totalCount,
            connectionStatus =
                ConnectionStatus(
                    connected = this.connectedCount,
                    disconnected = this.disconnectedCount,
                ),
            sensorTypeStatus =
                SensorTypeStatus(
                    temperatureHumidity = this.temperatureHumidityCount,
                    fire = this.fireCount,
                    displacement = this.displacementCount,
                ),
        )
}

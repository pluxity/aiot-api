package com.pluxity.aiot.feature

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.dashboard.SensorStatisticsRaw
import com.pluxity.aiot.feature.dto.FeatureSearchCondition
import com.pluxity.aiot.global.utils.findAllNotNull
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.Site
import org.springframework.stereotype.Repository

@Repository
class FeatureCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : FeatureCustomRepository {
    override fun findAllBySearchCondition(searchCondition: FeatureSearchCondition?): List<Feature> =
        kotlinJdslJpqlExecutor
            .findAllNotNull {
                select(entity(Feature::class))
                    .from(
                        entity(Feature::class),
                        leftFetchJoin(Feature::site),
                    ).where(
                        and(
                            searchCondition?.siteId?.let { path(Site::id).equal(it) },
                            searchCondition?.deviceId?.takeIf { it.isNotBlank() }?.let { path(Feature::deviceId).equal(it) },
                            searchCondition?.name?.takeIf { it.isNotBlank() }?.let { path(Feature::name).equal(it) },
                            searchCondition?.objectId?.let { path(Feature::objectId).like("$it%") },
                            searchCondition?.isActive?.let { path(Feature::isActive).equal(it) },
                        ),
                    ).orderBy(
                        path(Feature::site).asc(),
                        path(Feature::id).asc(),
                    )
            }

    override fun findSensorStatisticsBySiteIds(siteIds: List<Long>): List<SensorStatisticsRaw> =
        kotlinJdslJpqlExecutor
            .findAllNotNull {
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
                    and(
                        path(Site::id).isNotNull(),
                        path(Site::id).`in`(siteIds),
                    ),
                ).groupBy(path(Site::id))
            }
}

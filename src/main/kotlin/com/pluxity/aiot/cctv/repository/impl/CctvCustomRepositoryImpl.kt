package com.pluxity.aiot.cctv.repository.impl

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.cctv.Cctv
import com.pluxity.aiot.cctv.repository.CctvCustomRepository
import com.pluxity.aiot.global.annotation.CheckPermission
import com.pluxity.aiot.global.utils.findAllNotNull
import com.pluxity.aiot.permission.ResourceType
import com.pluxity.aiot.site.Site
import com.pluxity.aiot.user.entity.PermissionCheckType
import com.pluxity.aiot.user.entity.PermissionType
import org.springframework.stereotype.Repository

@Repository
class CctvCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : CctvCustomRepository {
    @CheckPermission(type = PermissionType.ID, phase = PermissionCheckType.FULL_ACCESS, resourceType = ResourceType.SITE)
    override fun findAllBySiteId(siteId: Long?): List<Cctv> =
        kotlinJdslJpqlExecutor
            .findAllNotNull {
                select(entity(Cctv::class))
                    .from(
                        entity(Cctv::class),
                        leftFetchJoin(Cctv::site),
                    ).where(
                        and(
                            siteId?.let { path(Site::id).eq(it) },
                        ),
                    )
            }
}

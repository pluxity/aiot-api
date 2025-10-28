package com.pluxity.aiot.announcement

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository

interface AnnouncementRepository :
    JpaRepository<Announcement, Long>,
    KotlinJdslJpqlExecutor

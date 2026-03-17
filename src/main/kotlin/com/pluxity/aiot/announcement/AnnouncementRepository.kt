package com.pluxity.aiot.announcement

import org.springframework.data.jpa.repository.JpaRepository

interface AnnouncementRepository :
    JpaRepository<Announcement, Long>,
    AnnouncementCustomRepository

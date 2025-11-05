package com.pluxity.aiot.announcement

import org.springframework.data.jpa.repository.JpaRepository

interface LlmMessageRepository : JpaRepository<LlmMessage, Long>

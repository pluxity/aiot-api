package com.pluxity.aiot.action

import org.springframework.data.jpa.repository.JpaRepository

interface ActionHistoryFileRepository : JpaRepository<ActionHistoryFile, Long>

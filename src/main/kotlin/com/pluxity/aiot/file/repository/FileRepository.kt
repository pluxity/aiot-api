package com.pluxity.aiot.file.repository

import com.pluxity.aiot.file.entity.FileEntity
import org.springframework.data.jpa.repository.JpaRepository

interface FileRepository : JpaRepository<FileEntity, Long> {
    fun findByIdIn(ids: List<Long>): List<FileEntity>
}

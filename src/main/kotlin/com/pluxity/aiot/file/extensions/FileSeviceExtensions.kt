package com.pluxity.aiot.file.extensions

import com.pluxity.aiot.file.dto.FileResponse
import com.pluxity.aiot.file.service.FileService

fun <T> FileService.getFileMapById(
    items: List<T>,
    idExtractor: (T) -> Long?,
): Map<Long, FileResponse> {
    val fileIds = items.mapNotNull(idExtractor)
    return if (fileIds.isEmpty()) {
        emptyMap()
    } else {
        getFiles(fileIds).associateBy { it.requiredId }
    }
}

fun <T> FileService.getFileMapByIds(
    items: List<T>,
    idExtractor: (T) -> List<Long?>,
): Map<Long, FileResponse> {
    val fileIds = items.flatMap(idExtractor).filterNotNull()
    return if (fileIds.isEmpty()) {
        emptyMap()
    } else {
        getFiles(fileIds).associateBy { it.requiredId }
    }
}

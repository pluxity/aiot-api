package com.pluxity.aiot.file.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.aiot.file.entity.FileEntity
import com.pluxity.aiot.global.response.BaseResponse
import com.pluxity.aiot.global.response.toBaseResponse

data class FileResponse(
    var id: Long? = null,
    var url: String? = null,
    var originalFileName: String? = null,
    var contentType: String? = null,
    var fileStatus: String? = null,
    @field:JsonUnwrapped var baseResponse: BaseResponse? = null,
) {
    val requiredId: Long
        get() = checkNotNull(id) { "FileResponse.id is null" }
}

fun FileEntity.toFileResponse(url: String?) =
    FileResponse(
        id = this.id,
        url = url ?: this.filePath,
        originalFileName = this.originalFileName,
        contentType = this.contentType,
        fileStatus = this.fileStatus.toString(),
        baseResponse = this.toBaseResponse(),
    )

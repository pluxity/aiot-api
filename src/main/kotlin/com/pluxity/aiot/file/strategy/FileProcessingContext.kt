package com.pluxity.aiot.file.strategy

import java.nio.file.Path

data class FileProcessingContext(
    val contentType: String,
    val tempPath: Path,
    val originalFileName: String,
)

package com.pluxity.aiot.file.strategy

data class FilePersistenceContext(
    val filePath: String,
    val newPath: String,
    val contentType: String,
    val originalFileName: String,
)

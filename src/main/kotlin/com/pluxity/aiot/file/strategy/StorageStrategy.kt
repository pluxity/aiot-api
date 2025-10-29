package com.pluxity.aiot.file.strategy

interface StorageStrategy {
    fun save(context: FileProcessingContext): String

    fun persist(context: FilePersistenceContext): String
}

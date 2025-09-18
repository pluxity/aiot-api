package com.pluxity.aiot.file.strategy.storage

interface StorageStrategy {
    fun save(context: FileProcessingContext): String

    fun persist(context: FilePersistenceContext): String
}

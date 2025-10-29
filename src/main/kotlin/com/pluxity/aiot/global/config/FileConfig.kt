package com.pluxity.aiot.global.config

import com.pluxity.aiot.file.repository.FileRepository
import com.pluxity.aiot.file.service.FileService
import com.pluxity.aiot.file.strategy.LocalStorageStrategy
import com.pluxity.aiot.file.strategy.S3StorageStrategy
import com.pluxity.aiot.file.strategy.StorageStrategy
import com.pluxity.aiot.global.properties.FileProperties
import com.pluxity.aiot.global.properties.S3Properties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class FileConfig {
    @Bean
    fun fileService(
        storageStrategy: StorageStrategy,
        fileRepository: FileRepository,
        s3Properties: S3Properties,
        s3Presigner: S3Presigner,
        fileProperties: FileProperties,
    ): FileService = FileService(s3Presigner, s3Properties, storageStrategy, fileRepository, fileProperties)

    @Bean
    @ConditionalOnProperty(name = ["file.storage-strategy"], havingValue = "local")
    fun localStorageStrategy(fileProperties: FileProperties): StorageStrategy = LocalStorageStrategy(fileProperties)

    @Bean
    @ConditionalOnProperty(name = ["file.storage-strategy"], havingValue = "s3")
    fun s3StorageStrategy(
        s3Properties: S3Properties,
        s3Client: S3Client,
    ): StorageStrategy = S3StorageStrategy(s3Properties, s3Client)
}

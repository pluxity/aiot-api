package com.pluxity.aiot.global.config

import com.influxdb.LogLevel
import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.InfluxDBClientOptions
import com.influxdb.client.QueryApi
import com.influxdb.client.WriteApi
import com.influxdb.client.WriteApiBlocking
import com.influxdb.client.WriteOptions
import com.pluxity.aiot.global.properties.InfluxdbProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InfluxdbConfig(
    val influxdbProperties: InfluxdbProperties,
) {
    /**
     * 비동기 동작 (응답 대기 x)
     * 데이터를 모아서 한번에 write
     */
    @Bean
    fun writeApi(influxDBClient: InfluxDBClient): WriteApi {
        val writeOptions =
            WriteOptions
                .builder()
                .batchSize(1000) // 한 번에 처리할 데이터 개수
                .flushInterval(1000) // 설정 시간(초)마다 저장
                .bufferLimit(10000) // 버퍼 사이즈 조정
                .build()
        return influxDBClient.makeWriteApi(writeOptions)
    }

    /**
     * 동기식 (응답 대기)
     */
    @Bean
    fun writeApiBlocking(influxDBClient: InfluxDBClient): WriteApiBlocking = influxDBClient.writeApiBlocking

    /**
     * 쿼리 API (조회용)
     */
    @Bean
    fun queryApi(influxDBClient: InfluxDBClient): QueryApi = influxDBClient.queryApi

    @Bean
    fun influxDBClient(): InfluxDBClient = InfluxDBClientFactory.create(influxDBClientOptions())

    @Bean
    fun influxDBClientOptions(): InfluxDBClientOptions =
        InfluxDBClientOptions
            .builder()
            .url(influxdbProperties.url)
            .authenticateToken(influxdbProperties.token.toCharArray())
            .org(influxdbProperties.org)
            .bucket(influxdbProperties.bucket)
            .logLevel(LogLevel.BASIC) // NONE, BASIC, HEADERS, BODY
            .build()
}

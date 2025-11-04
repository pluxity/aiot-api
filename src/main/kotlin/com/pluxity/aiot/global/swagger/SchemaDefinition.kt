package com.pluxity.aiot.global.swagger

import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema

interface SchemaDefinition {
    fun getSchemaName(): String

    fun createSchema(): Schema<Map<String, Any>>

    @Suppress("UNCHECKED_CAST")
    fun getSchema(): Schema<Map<String, Any>> =
        MapSchema()
            .addProperty("status", IntegerSchema().format("int32"))
            .addProperty("message", StringSchema())
            .addProperty("timestamp", StringSchema())
            .addProperty("data", createSchema()) as Schema<Map<String, Any>>
}

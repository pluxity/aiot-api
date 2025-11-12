package com.pluxity.aiot.global.swagger.builder

import com.pluxity.aiot.event.entity.EventStatus
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.springframework.stereotype.Component

@Component
class EventSummaryBuilder {
    fun createEventResponseSchema(): Schema<*> =
        Schema<Map<String, Object>>()
            .addProperty("eventId", IntegerSchema().format("int64"))
            .addProperty("deviceId", StringSchema())
            .addProperty("objectId", StringSchema())
            .addProperty("occurredAt", StringSchema())
            .addProperty("minValue", NumberSchema().format("double"))
            .addProperty("maxValue", NumberSchema().format("double"))
            .addProperty("status", StringSchema())
            .addProperty("eventName", StringSchema())
            .addProperty("fieldKey", StringSchema())
            .addProperty("guideMessage", StringSchema())

    fun createEventResponseListSchema(): ArraySchema =
        ArraySchema()
            .items(createEventResponseSchema())
            .example(createEventResponseListExample()) as ArraySchema

    @Suppress("UNCHECKED_CAST")
    fun createEventSummarySchema(): Schema<Map<String, Any>> =
        MapSchema()
            .additionalProperties(createEventResponseListSchema())
            .example(createEventSummaryExample()) as Schema<Map<String, Any>>

    private fun createEventResponseListExample(): List<Map<String, Any>> =
        listOf(
            mapOf(
                "eventId" to 1L,
                "deviceId" to "SNIOT-P-TST-050",
                "objectId" to "34957",
                "occurredAt" to "2025-11-04T10:00:00Z",
                "minValue" to 0.1,
                "maxValue" to 9.9,
                "status" to EventStatus.ACTIVE.name,
                "eventName" to "string",
                "fieldKey" to "Angle-X",
                "guideMessage" to "Check",
            ),
        )

    private fun createEventSummaryExample(): Map<String, Any> {
        val exampleList = createEventResponseListExample()
        return mapOf(
            EventStatus.ACTIVE.name to exampleList,
            EventStatus.IN_PROGRESS.name to exampleList,
            EventStatus.RESOLVED.name to exampleList,
        )
    }
}

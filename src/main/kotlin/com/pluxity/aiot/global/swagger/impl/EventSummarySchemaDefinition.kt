package com.pluxity.aiot.global.swagger.impl

import com.pluxity.aiot.global.swagger.SchemaDefinition
import com.pluxity.aiot.global.swagger.builder.EventSummaryBuilder
import io.swagger.v3.oas.models.media.Schema
import org.springframework.stereotype.Component

@Component
class EventSummarySchemaDefinition(
    private val schemaBuilder: EventSummaryBuilder,
) : SchemaDefinition {
    override fun getSchemaName(): String = "EventSummary"

    override fun createSchema(): Schema<Map<String, Any>> = schemaBuilder.createEventSummarySchema()
}

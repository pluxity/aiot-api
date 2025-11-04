package com.pluxity.aiot.global.swagger

import io.swagger.v3.oas.models.Components
import org.springframework.stereotype.Component

@Component
class SchemaRegistry(
    private val schemaDefinitions: List<SchemaDefinition>,
) {
    fun registerAllSchemas(components: Components) {
        schemaDefinitions.forEach { definition ->
            components.addSchemas(definition.getSchemaName(), definition.getSchema())
        }
    }
}

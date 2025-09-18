package com.pluxity.aiot.global.util

import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.PhysicalNamingStrategy
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment
import java.util.Locale

class PhysicalNamingCustomStrategy : PhysicalNamingStrategy {
    override fun toPhysicalCatalogName(
        name: Identifier?,
        jdbcEnvironment: JdbcEnvironment?,
    ): Identifier? = null

    override fun toPhysicalSchemaName(
        name: Identifier?,
        jdbcEnvironment: JdbcEnvironment?,
    ): Identifier? = null

    override fun toPhysicalTableName(
        name: Identifier,
        jdbcEnvironment: JdbcEnvironment?,
    ): Identifier? = convertToSnakeCase(name, "tb_")

    override fun toPhysicalSequenceName(
        name: Identifier,
        jdbcEnvironment: JdbcEnvironment?,
    ): Identifier? = convertToSnakeCase(name, "seq_")

    override fun toPhysicalColumnName(
        name: Identifier,
        jdbcEnvironment: JdbcEnvironment?,
    ): Identifier? = convertToSnakeCase(name, "")

    private fun convertToSnakeCase(
        identifier: Identifier,
        prefix: String?,
    ): Identifier? {
        val regex = "([a-z])([A-Z])"
        val replacement = "$1_$2"
        val newName =
            prefix +
                identifier.text
                    .replace(regex.toRegex(), replacement)
                    .lowercase(Locale.getDefault())
        return Identifier.toIdentifier(newName)
    }
}

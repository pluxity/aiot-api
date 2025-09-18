package com.pluxity.aiot.abbreviation

import org.springframework.data.jpa.repository.JpaRepository

interface AbbreviationRepository : JpaRepository<Abbreviation, Long> {
    fun existsByAbbreviationKey(abbreviationKey: String): Boolean

    fun existsByAbbreviationKeyAndIdNot(
        abbreviationKey: String,
        id: Long,
    ): Boolean

    fun findByIsActiveTrue(): List<Abbreviation>
}

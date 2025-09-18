package com.pluxity.aiot.system.entity.deviceprofile

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

@Entity
data class DeviceProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "field_key", nullable = false, unique = true)
    var fieldKey: String,
    @Column(name = "description")
    var description: String,
    @Column(name = "unit", nullable = false)
    var fieldUnit: String? = null,
    @Column(name = "field_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var fieldType: FieldType,
) {
    @OneToMany(mappedBy = "deviceProfile", cascade = [CascadeType.ALL], orphanRemoval = true)
    var deviceProfileTypes: MutableSet<DeviceProfileType> = mutableSetOf()

    fun update(
        fieldKey: String,
        description: String,
        fieldUnit: String?,
        fieldType: FieldType,
    ) {
        this.fieldKey = fieldKey
        this.description = description
        this.fieldUnit = fieldUnit
        this.fieldType = fieldType
    }

    enum class FieldType {
        String,
        Integer,
        Float,
        Boolean,
    }
}

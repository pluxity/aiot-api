package com.pluxity.aiot.data.dto

import java.time.Instant

fun dummyClimateSensorData(
    time: Instant = Instant.now(),
    temperature: Double = 25.0,
    humidity: Double = 60.0,
    discomfortIndex: Double = 10.0,
): ClimateSensorData =
    ClimateSensorData(
        time = time,
        temperature = temperature,
        humidity = humidity,
        discomfortIndex = discomfortIndex,
    )

fun dummyWasteFillLevelSensorData(
    time: Instant = Instant.now(),
    containerModuleId: Double = 1.0,
    actualFilling: Double = 20.0,
    highThreshold: Double = 60.0,
): WasteFillLevelSensorData =
    WasteFillLevelSensorData(
        time = time,
        containerModuleId = containerModuleId,
        actualFilling = actualFilling,
        highThreshold = highThreshold,
    )

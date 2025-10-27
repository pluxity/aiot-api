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

fun dummyDisplacementGaugeSensorData(
    time: Instant = Instant.now(),
    angleX: Double = 10.0,
    angleY: Double = 20.0,
): DisplacementGaugeSensorData =
    DisplacementGaugeSensorData(
        time = time,
        angleX = angleX,
        angleY = angleY,
    )

plugins {
    val kotlinVersion = "2.2.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version kotlinVersion
    id("com.diffplug.spotless") version "7.2.1"
}

group = "com.pluxity.aiot"
version = "0.0.1-SNAPSHOT"
description = "aiot"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
    implementation("org.springdoc:springdoc-openapi-starter-common:2.8.13")
    implementation("io.github.oshai:kotlin-logging:7.0.12")
    implementation("org.zalando:logbook-spring-boot-starter:3.12.2")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("software.amazon.awssdk:s3:2.30.24")
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:3.5.5")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:3.5.5")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:3.5.5")
    implementation ("org.hibernate:hibernate-spatial:6.6.26.Final")
    implementation("org.locationtech.jts:jts-core:1.20.0")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.11.0")
    implementation("com.influxdb:influxdb-client-kotlin:7.3.0")
    implementation("com.influxdb:flux-dsl:7.3.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Kotest Spring Extension
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.mockk:mockk:1.14.5")
    // H2 Database for testing
    testImplementation("com.h2database:h2")
    // websocket
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation ("io.github.springwolf:springwolf-core:1.18.0")
    implementation ("io.github.springwolf:springwolf-stomp:1.18.0")
    runtimeOnly("io.github.springwolf:springwolf-ui:1.18.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

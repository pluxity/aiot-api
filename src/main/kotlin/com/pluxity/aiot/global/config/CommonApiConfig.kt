package com.pluxity.aiot.global.config

import com.pluxity.aiot.global.properties.ServerDomainProperties
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CommonApiConfig(
    private val properties: ServerDomainProperties,
) {
    @Bean
    @ConditionalOnMissingBean(OpenAPI::class)
    fun commonOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("AIot API")
                    .description("AIot API Documentation")
                    .version("1.0.0")
                    .contact(Contact().name("Pluxity").email("support@pluxity.com"))
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("http://www.apache.org/licenses/LICENSE-2.0.html"),
                    ),
            ).servers(
                listOf(
                    Server().url(properties.url),
                ),
            )

    @Bean
    fun commonApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("1. 전체")
            .pathsToMatch("/**")
            .build()

    @Bean
    fun authApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("2. 인증")
            .pathsToMatch("/auth/**")
            .pathsToExclude("/users/**", "/admin/**", "/other/**") // 제외 경로 추가
            .build()

    @Bean
    fun fileApiByPath(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("3. 파일관리 API")
            .pathsToMatch("/files/**")
            .build()

    @Bean
    fun userApiByPath(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("4. 사용자 API")
            .pathsToMatch("/users/**", "/admin/users/**", "/roles/**", "/permissions/**")
            .build()

    @Bean
    fun siteApiByPath(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("5. 현장관리 API")
            .pathsToMatch("/sites/**")
            .build()

    @Bean
    fun featureApiByPath(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("6. Feature 관리 API")
            .pathsToMatch("/features/**")
            .build()

    @Bean
    fun deviceApiByPath(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("7. 디바이스 연동 설정 API")
            .pathsToMatch("/mobius/**", "/device-profiles/**")
            .build()

    @Bean
    fun cctvApiByPath(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("8. Cctv 관리 API")
            .pathsToMatch("/cctvs/**")
            .build()
}

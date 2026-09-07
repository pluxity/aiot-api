package com.pluxity.aiot.global.config

import com.pluxity.aiot.authentication.security.CustomUserDetails
import com.pluxity.aiot.authentication.security.JwtAuthenticationFilter
import com.pluxity.aiot.authentication.security.JwtProvider
import com.pluxity.aiot.authentication.security.RestAuthenticationEntryPoint
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.CorsProperties
import com.pluxity.aiot.user.repository.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class CommonSecurityConfig(
    private val repository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val restAuthenticationEntryPoint: RestAuthenticationEntryPoint,
    private val corsProperties: CorsProperties,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/actuator/**",
                        "/health",
                        "/subscription",
                        "/info",
                        "/prometheus",
                        "/error",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/swagger-config/**",
                        "/docs/**",
                    ).permitAll() // .requestMatchers("/admin/**").hasRole("ADMIN") // TODO: 구현 완료 시 적용
                    // GET permitAll보다 뒤에 두면 익명 요청이 컨트롤러에 닿아 401 대신 404가 나간다
                    .requestMatchers("/users/me/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET)
                    .permitAll()
                    .requestMatchers("/auth/**")
                    .permitAll() // GET 외의 /auth/** 경로도 허용
                    .anyRequest()
                    .authenticated()
            } // 나머지 모든 (GET이 아닌) 요청은 인증 필요
            .exceptionHandling { it.authenticationEntryPoint(restAuthenticationEntryPoint) }
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .sessionManagement { sessionManagement: SessionManagementConfigurer<HttpSecurity> ->
                sessionManagement.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS,
                )
            }

        return http.build()
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager

    @Bean
    fun userDetailsService(): UserDetailsService =
        UserDetailsService { username: String ->
            repository
                .findByUsername(username)
                ?.let { CustomUserDetails(it) }
                ?: throw CustomException(ErrorCode.NOT_FOUND_USER, username)
        }

    @Bean
    fun jwtAuthenticationFilter(): JwtAuthenticationFilter = JwtAuthenticationFilter(jwtProvider, userDetailsService())

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = corsProperties.allowedOriginPatterns
        configuration.allowedMethods = corsProperties.allowedMethods
        configuration.allowedHeaders = mutableListOf("*") // 와일드카드 또는 필요한 헤더 명시
        configuration.allowCredentials = true
        configuration.maxAge = corsProperties.maxAge // pre-flight 요청 캐시 시간

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}

package com.pluxity.aiot.authentication.security

enum class WhiteListPath(
    val path: String,
) {
    AUTH_IN("auth/sign-in"),
    AUTH_UP("auth/sign-up"),
    REFRESH_TOKEN("auth/refresh-token"),
    ACTUATOR("actuator"),
    APIDOC("api-docs"),
    HEALTH("health"),
    INFO("info"),
    PROMETHEUS("prometheus"),
    SWAGGER("swagger-ui"),
    ;

    /** startsWith만으로는 /health가 /health-actions를, /info가 /information을 삼킨다. */
    fun matches(requestPath: String): Boolean {
        val prefix = "/$path"
        if (!requestPath.startsWith(prefix)) return false

        val next = requestPath.getOrNull(prefix.length) ?: return true
        return next == '/' || next == '.'
    }

    companion object {
        fun isWhiteListed(requestPath: String): Boolean = entries.any { it.matches(requestPath) }
    }
}

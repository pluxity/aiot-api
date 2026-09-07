package com.pluxity.aiot.authentication.security

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.response.ErrorResponseBody
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import tools.jackson.databind.json.JsonMapper

/** 필터 단계는 @RestControllerAdvice보다 앞이라 예외 처리기가 잡지 못한다. */
object AuthenticationErrorWriter {
    private val jsonMapper = JsonMapper.builder().build()

    fun write(
        response: HttpServletResponse,
        errorCode: ErrorCode,
        message: String? = null,
    ) {
        val status = errorCode.getHttpStatus()
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()

        val body =
            ErrorResponseBody(
                status = status,
                message = message ?: errorCode.getMessage(),
                code = status.value().toString(),
                error = errorCode.name,
            )
        response.writer.write(jsonMapper.writeValueAsString(body))
    }
}

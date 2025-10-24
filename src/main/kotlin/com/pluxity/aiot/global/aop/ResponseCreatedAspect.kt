package com.pluxity.aiot.global.aop

import com.pluxity.aiot.global.annotation.ResponseCreated
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.net.URI

@Aspect
@Component
class ResponseCreatedAspect {
    @Around("@annotation(responseCreated)")
    fun handleResponseCreated(
        joinPoint: ProceedingJoinPoint,
        responseCreated: ResponseCreated,
    ): ResponseEntity<Void> {
        val result = joinPoint.proceed() as ResponseEntity<*>
        val id = result.getBody()
        val eventId = getArgument(joinPoint, "eventId") // 추가 추출

        return ResponseEntity
            .created(
                URI.create(
                    responseCreated.path
                        .replace("{id}", id?.toString() ?: "")
                        .replace("{eventId}", eventId?.toString() ?: ""),
                ),
            ).build()
    }

    private fun getArgument(
        joinPoint: ProceedingJoinPoint,
        name: String,
    ): Any? {
        val signature = joinPoint.signature as MethodSignature
        val paramNames = signature.parameterNames
        val args = joinPoint.args

        val index = paramNames.indexOf(name)
        return if (index >= 0) args[index] else null
    }
}

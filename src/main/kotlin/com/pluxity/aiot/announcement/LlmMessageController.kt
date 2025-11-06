package com.pluxity.aiot.announcement

import com.pluxity.aiot.announcement.dto.LlmMessageResponse
import com.pluxity.aiot.global.annotation.ResponseCreated
import com.pluxity.aiot.global.response.DataResponseBody
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/llm")
class LlmMessageController(
    private val llmMessageService: LlmMessageService,
) {
    @PostMapping("/generate")
    @ResponseCreated
    fun generateMessage(): ResponseEntity<Void> {
        log.info { "LLM 메시지 생성 요청" }
        runBlocking {
            llmMessageService.generateAndSaveMessage()
        }
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun getAllMessages(
        @Parameter(description = "공원 아이디", example = "1")
        @RequestParam(required = false)
        siteId: Long?,
        @Parameter(description = "조회 시작일(yyyyMMddHHmmss)", example = "20250101000000")
        @RequestParam(required = false)
        from: String?,
        @Parameter(description = "조회 종료일(yyyyMMddHHmmss)", example = "20250131235959")
        @RequestParam(required = false)
        to: String?,
    ): ResponseEntity<DataResponseBody<List<LlmMessageResponse>>> {
        val messages = llmMessageService.findAll(siteId, from, to)
        return ResponseEntity.ok(DataResponseBody(messages))
    }

    @GetMapping("/{id}")
    fun getMessageById(
        @PathVariable id: Long,
    ): ResponseEntity<DataResponseBody<LlmMessageResponse>> {
        val message = llmMessageService.findById(id)
        return ResponseEntity.ok(DataResponseBody(message))
    }
}

package com.pluxity.aiot.mic

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * 업체 장비 목록 조회(HTTP)와 반영(트랜잭션)을 분리해 조합한다.
 * 목록 조회가 MicService의 트랜잭션 밖에서 끝나도록 하는 것이 이 클래스의 존재 이유다.
 */
@Component
@ConditionalOnProperty("mic.enabled", havingValue = "true")
class MicFacade(
    private val micClient: MicClient,
    private val micService: MicService,
) {
    fun sync() {
        micService.sync(micClient.getMicList())
    }
}

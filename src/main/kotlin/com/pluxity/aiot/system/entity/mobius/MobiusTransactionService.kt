package com.pluxity.aiot.system.entity.mobius

import com.pluxity.aiot.system.entity.mobius.dto.MobiusResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MobiusTransactionService(
    private val configRepository: MobiusConfigRepository,
) {
    @Transactional
    fun loadLatestUrl(): MobiusResponse = MobiusResponse(configRepository.findTopByOrderByCreatedAtDesc()?.url ?: createDefaultConfig())

    @Transactional
    fun createConfig(url: String) {
        configRepository.save(MobiusConfig(url = url))
    }

    @Transactional
    fun createDefaultConfig(): String {
        val defaultConfig = MobiusConfig(url = DEFAULT_MOBIUS_URL)
        configRepository.save(defaultConfig)
        return DEFAULT_MOBIUS_URL
    }

    companion object {
        private const val DEFAULT_MOBIUS_URL = "http://203.253.128.181:11000/Mobius/sawwave"
    }
}

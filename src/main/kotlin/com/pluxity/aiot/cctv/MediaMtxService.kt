package com.pluxity.aiot.cctv

import com.pluxity.aiot.cctv.dto.MediaMtxErrorResponse
import com.pluxity.aiot.cctv.dto.MediaMtxPathListResponse
import com.pluxity.aiot.cctv.dto.MediaMtxPathResponse
import com.pluxity.aiot.cctv.dto.MediaMtxRequest
import com.pluxity.aiot.global.config.WebClientFactory
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.MediaMtxProperties
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
class MediaMtxService(
    webClientFactory: WebClientFactory,
    mediaMtxProperties: MediaMtxProperties,
) {
    private val client: WebClient =
        webClientFactory
            .createClient(mediaMtxProperties.apiUrl)
            .mutate()
            .build()

    fun getAllPath(): List<MediaMtxPathResponse> {
        val response =
            client
                .get()
                .uri("/v3/config/paths/list")
                .retrieve()
                .bodyToMono<MediaMtxPathListResponse>()
                .block()
        return response?.let { response.items } ?: emptyList()
    }

    fun addPath(
        path: String,
        source: String,
    ) {
        val request = MediaMtxRequest(source)
        client
            .post()
            .uri("/v3/config/paths/add/$path")
            .bodyValue(request)
            .retrieve()
            .onStatus({ !it.is2xxSuccessful }) { resp ->
                resp
                    .bodyToMono<MediaMtxErrorResponse>()
                    .defaultIfEmpty(MediaMtxErrorResponse("empty body"))
                    .flatMap { body ->
                        Mono.error(CustomException(errorCode = ErrorCode.MEDIAMTX_ADD_ERROR, body.error))
                    }
            }.toBodilessEntity()
            .block()
    }

    fun deletePath(path: String) {
        client
            .delete()
            .uri("/v3/config/paths/delete/$path")
            .retrieve()
            .onStatus({ !it.is2xxSuccessful }) { resp ->
                if (resp.statusCode().value() == 404) {
                    Mono.empty()
                } else {
                    resp
                        .bodyToMono<MediaMtxErrorResponse>()
                        .defaultIfEmpty(MediaMtxErrorResponse("empty body"))
                        .flatMap { body ->
                            Mono.error(CustomException(errorCode = ErrorCode.MEDIAMTX_DELETE_ERROR, body.error))
                        }
                }
            }.toBodilessEntity()
            .block()
    }
}

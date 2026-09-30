package com.pluxity.aiot.ldms.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "LDMS 전광판 응답")
data class LdmsDisplayResponse(
    @field:Schema(description = "LDMS 전광판 일련번호", example = "1")
    val seq: Long,
    @field:Schema(description = "전광판 장치 ID", example = "C16LD25004DC")
    val deviceId: String?,
    @field:Schema(description = "전광판 명칭", example = "BNS 전광판")
    val name: String?,
    @field:Schema(description = "위도", example = "37.4467071")
    val latitude: Double?,
    @field:Schema(description = "경도", example = "127.1584128")
    val longitude: Double?,
    @field:Schema(description = "장비 IP", example = "192.168.1.21")
    val ip: String?,
    @field:Schema(description = "장비 포트", example = "7200")
    val port: Int?,
    @field:Schema(description = "전광판 타입", example = "Noaled")
    val type: String?,
    @field:Schema(description = "화면 가로 크기(px)", example = "384")
    val width: Int?,
    @field:Schema(description = "화면 세로 크기(px)", example = "64")
    val height: Int?,
    @field:Schema(description = "표시 순서", example = "1")
    val displayOrder: Int?,
    @field:Schema(description = "통신 상태", example = "Ok")
    val commStatus: String?,
    @field:Schema(description = "최종 명령 처리 결과", example = "Success")
    val commandResult: String?,
    @field:Schema(description = "운영 시작 시각(HHmm)", example = "0700")
    val onTimeStart: String?,
    @field:Schema(description = "운영 종료 시각(HHmm)", example = "1840")
    val onTimeEnd: String?,
    @field:Schema(description = "송출 여부(Y/N)", example = "Y")
    val sendYn: String?,
    @field:Schema(description = "현재 표출 메시지")
    val displayMessage: String?,
    @field:Schema(description = "최근 이벤트", example = "EMER_END")
    val event: String?,
    @field:Schema(description = "연동 카메라 일련번호", example = "22")
    val cameraSeq: Long?,
    @field:Schema(description = "LDMS 그룹 ID", example = "R105")
    val groupId: String?,
)

fun LdmsEbrdInfo.toResponse() =
    LdmsDisplayResponse(
        seq = ebrdSeq,
        deviceId = ebrdId,
        name = ebrdNm,
        latitude = ebrdLat,
        longitude = ebrdLng,
        ip = ebrdIp,
        port = ebrdPort,
        type = ebrdType,
        width = ebrdSizeW,
        height = ebrdSizeH,
        displayOrder = dispSeq,
        commStatus = commStat,
        commandResult = cmdRslt,
        onTimeStart = onTimeStart,
        onTimeEnd = onTimeEnd,
        sendYn = sendYn,
        displayMessage = ebrdDispMsg,
        event = ebrdEvent,
        cameraSeq = camSeq,
        groupId = grpId,
    )

@Schema(description = "LDMS 송출그룹 응답")
data class LdmsEmcallGroupResponse(
    @field:Schema(description = "LDMS 송출그룹 일련번호", example = "1")
    val seq: Long,
    @field:Schema(description = "송출그룹 장치 ID", example = "901")
    val deviceId: String?,
    @field:Schema(description = "송출그룹 명칭", example = "BNS 비상방송")
    val name: String?,
    @field:Schema(description = "장비 IP", example = "192.168.1.31")
    val ip: String?,
    @field:Schema(description = "장비 포트", example = "8888")
    val port: Int?,
    @field:Schema(description = "위도", example = "37.4428")
    val latitude: Double?,
    @field:Schema(description = "경도", example = "127.174")
    val longitude: Double?,
    @field:Schema(description = "송출그룹 타입", example = "Itg")
    val type: String?,
    @field:Schema(description = "통신 상태", example = "Ok")
    val commStatus: String?,
    @field:Schema(description = "이벤트 연동 모드", example = "On")
    val eventMode: String?,
    @field:Schema(description = "기본 TTS 메시지")
    val ttsMessage: String?,
    @field:Schema(description = "최근 송출 상태. LDMS가 상태를 보낸 적 없으면 null")
    val status: LdmsEmcallGroupStatusResponse?,
    @field:Schema(description = "표시 순서")
    val displayOrder: Int?,
    @field:Schema(description = "연동 카메라 일련번호", example = "23")
    val cameraSeq: Long?,
    @field:Schema(description = "LDMS 그룹 ID", example = "R105")
    val groupId: String?,
)

@Schema(description = "LDMS 송출그룹 상태")
data class LdmsEmcallGroupStatusResponse(
    @field:Schema(description = "전광판 On/Off", example = "Off")
    val message: String?,
    @field:Schema(description = "경광등 On/Off", example = "Off")
    val light: String?,
    @field:Schema(description = "저장된 음성 송출 On/Off", example = "Off")
    val speaker: String?,
    @field:Schema(description = "TTS 음원 송출 On/Off", example = "Off")
    val speakerTts: String?,
    @field:Schema(description = "TTS 메시지", example = "IDLE")
    val ttsMessage: String?,
)

fun LdmsEmcallGroupInfo.toResponse(status: LdmsEmcallGroupStatus?) =
    LdmsEmcallGroupResponse(
        seq = emcallGrpSeq,
        deviceId = emcallGrpId,
        name = emcallGrpNm,
        ip = emcallGrpIp,
        port = emcallGrpPort,
        latitude = emcallGrpLat,
        longitude = emcallGrpLng,
        type = emcallType,
        commStatus = commStat,
        eventMode = evtMode,
        ttsMessage = emcallTtsMsg,
        status =
            status?.let {
                LdmsEmcallGroupStatusResponse(
                    message = it.msg,
                    light = it.light,
                    speaker = it.speaker,
                    speakerTts = it.speakerTts,
                    ttsMessage = it.ttsMsg,
                )
            },
        displayOrder = dispSeq,
        cameraSeq = camSeq,
        groupId = grpId,
    )

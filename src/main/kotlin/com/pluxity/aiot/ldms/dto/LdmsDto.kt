package com.pluxity.aiot.ldms.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class LdmsLoginRequest(
    @field:JsonProperty("user_id")
    val userId: String,
    @field:JsonProperty("user_pass")
    val userPass: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LdmsLoginResult(
    @field:JsonProperty("user_id")
    val userId: String? = null,
    @field:JsonProperty("grp_id")
    val grpId: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LdmsEbrdInfo(
    @field:JsonProperty("ebrd_seq")
    val ebrdSeq: Long,
    @field:JsonProperty("ebrd_id")
    val ebrdId: String? = null,
    @field:JsonProperty("ebrd_nm")
    val ebrdNm: String? = null,
    @field:JsonProperty("ebrd_lat")
    val ebrdLat: Double? = null,
    @field:JsonProperty("ebrd_lng")
    val ebrdLng: Double? = null,
    @field:JsonProperty("ebrd_ip")
    val ebrdIp: String? = null,
    @field:JsonProperty("ebrd_port")
    val ebrdPort: Int? = null,
    @field:JsonProperty("ebrd_type")
    val ebrdType: String? = null,
    @field:JsonProperty("ebrd_size_w")
    val ebrdSizeW: Int? = null,
    @field:JsonProperty("ebrd_size_h")
    val ebrdSizeH: Int? = null,
    @field:JsonProperty("disp_seq")
    val dispSeq: Int? = null,
    @field:JsonProperty("comm_stat")
    val commStat: String? = null,
    @field:JsonProperty("cmd_rslt")
    val cmdRslt: String? = null,
    @field:JsonProperty("on_time_start")
    val onTimeStart: String? = null,
    @field:JsonProperty("on_time_end")
    val onTimeEnd: String? = null,
    @field:JsonProperty("send_yn")
    val sendYn: String? = null,
    @field:JsonProperty("ebrd_disp_msg")
    val ebrdDispMsg: String? = null,
    @field:JsonProperty("ebrd_event")
    val ebrdEvent: String? = null,
    @field:JsonProperty("cam_seq")
    val camSeq: Long? = null,
    @field:JsonProperty("grp_id")
    val grpId: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LdmsEmcallGroupInfo(
    @field:JsonProperty("emcall_grp_seq")
    val emcallGrpSeq: Long,
    @field:JsonProperty("emcall_grp_id")
    val emcallGrpId: String? = null,
    @field:JsonProperty("emcall_grp_nm")
    val emcallGrpNm: String? = null,
    @field:JsonProperty("emcall_grp_ip")
    val emcallGrpIp: String? = null,
    @field:JsonProperty("emcall_grp_port")
    val emcallGrpPort: Int? = null,
    @field:JsonProperty("emcall_grp_lat")
    val emcallGrpLat: Double? = null,
    @field:JsonProperty("emcall_grp_lng")
    val emcallGrpLng: Double? = null,
    @field:JsonProperty("emcall_type")
    val emcallType: String? = null,
    @field:JsonProperty("comm_stat")
    val commStat: String? = null,
    @field:JsonProperty("evt_mode")
    val evtMode: String? = null,
    @field:JsonProperty("emcall_tts_msg")
    val emcallTtsMsg: String? = null,
    @field:JsonProperty("emcall_grp_stat_json")
    val emcallGrpStatJson: String? = null,
    @field:JsonProperty("disp_seq")
    val dispSeq: Int? = null,
    @field:JsonProperty("cam_seq")
    val camSeq: Long? = null,
    @field:JsonProperty("grp_id")
    val grpId: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LdmsEmcallGroupStatus(
    @field:JsonProperty("device_id")
    val deviceId: String? = null,
    val msg: String? = null,
    val light: String? = null,
    val speaker: String? = null,
    @field:JsonProperty("speaker_tts")
    val speakerTts: String? = null,
    @field:JsonProperty("tts_msg")
    val ttsMsg: String? = null,
)

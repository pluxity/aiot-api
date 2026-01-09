package com.pluxity.aiot.global.constant

import org.springframework.http.HttpStatus

enum class ErrorCode(
    private val httpStatus: HttpStatus,
    private val message: String,
) : Code {
    INVALID_ID_OR_PASSWORD(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호가 틀렸습니다."),

    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "ACCESS 토큰이 유효하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "REFRESH 토큰이 유효하지 않습니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),

    INVALID_FILE_STATUS(HttpStatus.BAD_REQUEST, "적절하지 않은 파일 상태입니다."),

    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "ACCESS 토큰이 만료되었습니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "REFRESH 토큰이 만료되었습니다."),

    FAILED_TO_ZIP_FILE(HttpStatus.BAD_REQUEST, "파일 압축에 실패했습니다."),
    FAILED_TO_UPLOAD_FILE(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),

    DUPLICATE_RESOURCE_ID(HttpStatus.BAD_REQUEST, "중복된 리소스 ID가 포함되어 있습니다."),

    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "%s 을(를) 가진 회원을 찾을 수 없습니다."),
    NOT_FOUND_DATA(HttpStatus.BAD_REQUEST, "데이터가 존재하지 않습니다."),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_RESOURCE_TYPE(HttpStatus.BAD_REQUEST, "%s는 유효하지 않은 RESOURCE TYPE 입니다."),

    INVALID_LOCATION(HttpStatus.BAD_REQUEST, "Polygon WKT만 허용됩니다."),
    INVALID_CURSOR_PARAMETERS(HttpStatus.BAD_REQUEST, "lastId와 lastStatus가 모두 제공되거나 모두 생략되어야 합니다. lastId:%s, lastStatus:%s"),

    DUPLICATE_USERNAME(HttpStatus.BAD_REQUEST, "%s는 이미 존재 하는 아이디 입니다."),
    DUPLICATE_PERMISSION_GROUP_NAME(HttpStatus.BAD_REQUEST, "이름이 %s인 권한 그룹이 이미 존재합니다."),
    DUPLICATE_EVENT_CONDITION(HttpStatus.BAD_REQUEST, "%s"),
    INVALID_EVENT_CONDITION(HttpStatus.BAD_REQUEST, "%s"),
    INVALID_DATE_TIME_FORMAT(HttpStatus.BAD_REQUEST, "날짜 형식이 올바르지 않습니다: 형식:%s, 입력:%s"),

    NOT_FOUND_EVENT_HISTORY(HttpStatus.NOT_FOUND, "ID가 %s인 Event History를 찾을 수 없습니다."),
    NOT_FOUND_ACTION_HISTORY(HttpStatus.NOT_FOUND, "ID가 %s인 Action History를 찾을 수 없습니다."),
    NOT_FOUND_EVENT_CONDITION(HttpStatus.NOT_FOUND, "ID가 %s인 이벤트 조건을 찾을 수 없습니다."),
    NOT_FOUND_FILE(HttpStatus.NOT_FOUND, "ID가 %s인 파일을 찾을 수 없습니다."),
    NOT_FOUND_FEATURE(HttpStatus.NOT_FOUND, "ID가 %s인 Feature를 찾을 수 없습니다."),
    NOT_FOUND_FEATURE_BY_DEVICE_ID(HttpStatus.NOT_FOUND, "Device ID가 %s인 Feature를 찾을 수 없습니다."),
    NOT_FOUND_DEVICE_BY_FEATURE(HttpStatus.NOT_FOUND, "Feature ID가 %s인 디바이스를 찾을 수 없습니다."),
    NOT_FOUND_SITE(HttpStatus.NOT_FOUND, "ID가 %s인 현장을 찾을 수 없습니다."),
    NOT_FOUND_ROLE(HttpStatus.NOT_FOUND, "ID가 %s인 Role을 찾을 수 없습니다."),
    NOT_FOUND_PERMISSION_GROUP(HttpStatus.NOT_FOUND, "ID가 %s인 PermissionGroup을 찾을 수 없습니다."),
    NOT_FOUND_CCTV(HttpStatus.NOT_FOUND, "ID가 %s인 CCTV를 찾을 수 없습니다."),
    NOT_FOUND_LLM_MESSAGE(HttpStatus.NOT_FOUND, "ID가 %s인 LLM 메시지를 찾을 수 없습니다."),

    MEDIAMTX_ADD_ERROR(HttpStatus.BAD_REQUEST, "MediaMtx등록 실패: %s"),
    MEDIAMTX_DELETE_ERROR(HttpStatus.BAD_REQUEST, "MediaMtx삭제 실패: %s"),

    NOT_FOUND_INVALID_NUMERIC_VALUE(HttpStatus.BAD_REQUEST, "유효한 숫자가 아닙니다."),
    NOT_SUPPORTED_OPERATOR(HttpStatus.BAD_REQUEST, "지원하지 않는 연산자입니다."),
    ;

    override fun getHttpStatus(): HttpStatus = httpStatus

    override fun getMessage(): String = message

    override fun getStatusName(): String = httpStatus.name
}

package com.pluxity.aiot.domain.account.group

enum class AdminMenu(
    val id: String,
    val path: String,
    val displayName: String,
) {
    DRAWING("drawing", "/admin/drawing", "도면 설정"),
    POI_LIST("device", "/admin/poi-list", "디바이스 설정"),
    ACCOUNT("account", "/admin/account", "사용자 설정"),
    ACCOUNT_GROUP("account-group", "/admin/account-group", "사용자 그룹 설정"),
    SYSTEM_SETTING("system-setting", "/admin/system-setting", "시스템 설정"),
    NOTIFICATION("notification", "/admin/notification", "공지사항 설정"),
    DISPLAY_SETTING("display-setting", "/admin/display-setting", "디스플레이 설정"),
    SOUND_SETTING("sound-setting", "/admin/sound-setting", "경고음 설정"),
    DEVICE_TYPE("device-type", "/admin/device-type", "디바이스 타입 설정"),
    EVENT_SETTING("event-setting", "/admin/event-setting", "이벤트 설정"),
    ;

    data class MenuInfo(
        val id: String,
        val name: String,
        val path: String,
    )

    companion object {
        fun getAllPaths(): List<String> = entries.map { it.path }

        fun getMenuInfoList(): List<MenuInfo> = entries.map { MenuInfo(it.id, it.displayName, it.path) }
    }
}

package com.pluxity.aiot.domain.account

enum class AccountRole {
    ROLE_ADMIN,
    ROLE_USER,
    MASTER,
}

enum class AccountSearch {
    USER_ID,
    USER_NAME,
    USER_ROLE,
    DEFAULT,
}

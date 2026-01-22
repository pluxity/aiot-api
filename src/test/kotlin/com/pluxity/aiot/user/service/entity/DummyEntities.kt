package com.pluxity.aiot.user.service.entity

import com.pluxity.aiot.authentication.entity.RefreshToken
import com.pluxity.aiot.base.entity.withId
import com.pluxity.aiot.user.entity.Role
import com.pluxity.aiot.user.entity.User

fun dummyUser(
    id: Long? = 1L,
    username: String = "username",
    password: String = "password",
    name: String = "name",
    code: String? = "code",
    phoneNumber: String? = null,
    department: String? = null,
): User = User(username, password, name, code, phoneNumber, department).withId(id)

fun dummyRefreshToken(
    username: String = "username",
    token: String = "token",
    timeToLive: Int = 30,
): RefreshToken = RefreshToken(username, token, timeToLive)

fun dummyRole(
    id: Long? = 1L,
    name: String = "role",
    description: String? = "description",
) = Role(name, description).withId(id)

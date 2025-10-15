package com.pluxity.aiot.cctv.repository

import com.pluxity.aiot.cctv.Cctv

interface CctvCustomRepository {
    fun findAllBySiteId(siteId: Long?): List<Cctv>
}

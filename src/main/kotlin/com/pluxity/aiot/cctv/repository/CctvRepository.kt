package com.pluxity.aiot.cctv.repository

import com.pluxity.aiot.cctv.Cctv
import org.springframework.data.jpa.repository.JpaRepository

interface CctvRepository :
    JpaRepository<Cctv, Long>,
    CctvCustomRepository

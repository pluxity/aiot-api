package com.pluxity.aiot.domain.account.group

import com.pluxity.aiot.domain.account.AccountRepository
import com.pluxity.aiot.domain.account.group.dto.AccountGroupRequest
import com.pluxity.aiot.domain.account.group.dto.AccountGroupResponse
import com.pluxity.aiot.domain.account.group.dto.toAccountGroupResponse
import com.pluxity.aiot.domain.boundary.Boundary
import com.pluxity.aiot.domain.boundary.BoundaryRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils

@Service
@Transactional
class AccountGroupService(
    private val repository: AccountGroupRepository,
    private val accountRepository: AccountRepository,
    private val boundaryRepository: BoundaryRepository,
) {
    fun checkDuplicateName(name: String) {
        if (StringUtils.hasText(name) && repository.existsByName(name)) {
            throw CustomException(ErrorCode.DUPLICATE_GROUP_NAME, name)
        }
    }

    fun save(requestDto: AccountGroupRequest): Long {
        checkDuplicateName(requestDto.name)

        var accountGroup =
            AccountGroup(
                name = requestDto.name,
                role = requestDto.role,
            )

        accountGroup = repository.save(accountGroup)

        // 권한 설정
        updateAccessRights(accountGroup, requestDto)

        return accountGroup.id!!
    }

    @Transactional(readOnly = true)
    fun findAll(search: String?): List<AccountGroupResponse> =
        repository
            .findAll {
                this
                    .select(entity(AccountGroup::class))
                    .from(entity(AccountGroup::class))
                    .where(
                        and(
                            search?.let { path(AccountGroup::name).eq(it) },
                        ),
                    )
            }.filterNotNull()
            .map { it.toAccountGroupResponse() }

    @Transactional(readOnly = true)
    fun find(id: Long): AccountGroupResponse {
        val accountGroup = findById(id)
        return accountGroup.toAccountGroupResponse()
    }

    fun update(
        id: Long,
        requestDto: AccountGroupRequest,
    ) {
        val accountGroup = findById(id)
        checkDuplicateName(requestDto.name)
        accountGroup.update(requestDto)

        // 권한 설정 업데이트
        updateAccessRights(accountGroup, requestDto)
    }

    fun delete(id: Long) {
        val accountGroup = findById(id)

        // 해당 그룹에 속한 계정 확인
        if (accountRepository.existsByAccountGroup(accountGroup)) {
            throw CustomException(ErrorCode.INVALID_DELETE_EXIST_IN_ACCOUNT)
        }
        repository.delete(accountGroup)
    }

    fun deleteAll(ids: List<Long>) {
        ids.forEach { id -> delete(id) }
    }

    private fun updateAccessRights(
        accountGroup: AccountGroup,
        requestDto: AccountGroupRequest,
    ) {
        // 접근 권한 업데이트
        val drawings: List<Boundary> = boundaryRepository.findAllById(requestDto.boundaryIds)
        accountGroup.updateAccessibleBoundaries(drawings)

        // 메뉴 권한 업데이트
        accountGroup.updateAccessibleMenus(requestDto.menuPaths)
    }

    @Transactional(readOnly = true)
    fun getAllAccessibleMenuPaths(id: Long): List<String> = findById(id).accessibleMenus.toList()

    @Transactional(readOnly = true)
    fun getAllAccessibleBoundaries(id: Long): List<Long> {
        val accountGroup = findById(id)
        return accountGroup.boundaries.map { it.id!! }
    }

    private fun AccountGroupService.findById(id: Long): AccountGroup =
        repository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_ACCOUNT_GROUP, id)
}

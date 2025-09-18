package com.pluxity.aiot.domain.account

import com.pluxity.aiot.domain.account.group.AccountGroupRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AccountService(
    private val passwordEncoder: PasswordEncoder,
    private val accountRepository: AccountRepository,
    private val accountGroupRepository: AccountGroupRepository,
) {
//
//    fun save(dto: AccountRequestDto): AccountDto {
//        require(!accountRepository!!.existsByUserid(dto.userid())) { "이미 존재하는 아이디입니다." }
//
//        val accountGroup: AccountGroup = accountGroupRepository.findById(dto.groupId())
//            .orElseThrow({ IllegalArgumentException("사용자 그룹을 찾을 수 없습니다.") })
//
//        var account: Account? = Account.builder()
//            .accountGroup(accountGroup)
//            .userid(dto.userid())
//            .username(dto.username())
//            .password(passwordEncoder.encode(dto.password()))
//            .role(accountGroup.getRole())
//            .expirationTime(dto.expirationTime())
//            .build()
//
//
//        account = accountRepository.save(account!!)
//        accountGroup.addAccount(account)
//
//        return Account.toDto(account)
//    }
//
//    @Transactional(readOnly = true)
//    fun find(id: Long?): AccountDto {
//        val account: Account? = accountRepository!!.findById(id!!)
//            .orElseThrow({ IllegalArgumentException("사용자를 찾을 수 없습니다.") })
//        return Account.toDto(account)
//    }
//
//    @Transactional(readOnly = true)
//    fun findAll(search: AccountSearchDto?): List<AccountDto> =
//        accountRepository.findAll(getSpecification(search, null))
//            .map { Account.toDto(it) }
//
//    @Transactional(readOnly = true)
//    fun findAllByGroupId(groupId: Long?, search: AccountSearchDto?): MutableList<AccountDto?> {
//        return accountRepository.findAll(getSpecification(search, groupId)).stream()
//            .map(Account::toDto)
//            .collect(Collectors.toList())
//    }
//
//    private fun getSpecification(search: AccountSearchDto?, groupId: Long?): Specification<Account> {
//        return Specification { root, _, criteriaBuilder ->
//            val predicates = mutableListOf<Predicate>()
//
//            groupId?.let {
//                predicates.add(criteriaBuilder.equal(root.get<Any>("accountGroup").get<Any>("id"), it))
//            }
//
//            if (search?.value()?.isNotBlank() == true) {
//                when (search.type()) {
//                    "USER_ID" -> predicates.add(criteriaBuilder.like(root.get("userid"), "%${search.value()}%"))
//                    "USER_NAME" -> predicates.add(criteriaBuilder.like(root.get("username"), "%${search.value()}%"))
//                }
//            }
//
//            criteriaBuilder.and(*predicates.toTypedArray())
//        }
//    }
//
//    fun update(id: Long?, dto: AccountRequestDto): AccountDto {
//        val account: Account = accountRepository!!.findById(id!!)
//            .orElseThrow({ IllegalArgumentException("사용자를 찾을 수 없습니다.") })
//
//        // 비밀번호 변경
//        if (StringUtils.hasText(dto.password())) {
//            account.setPassword(passwordEncoder.encode(dto.password()))
//        }
//
//        // 사용자 그룹 변경
//        if (dto.groupId() != null && !dto.groupId().equals(account.getAccountGroup().getId())) {
//            val accountGroup: AccountGroup = accountGroupRepository.findById(dto.groupId())
//                .orElseThrow({ IllegalArgumentException("사용자 그룹을 찾을 수 없습니다.") })
//
//            account.setRole(accountGroup.getRole())
//
//            if (account.getAccountGroup() != null) {
//                account.getAccountGroup().removeAccount(account)
//            }
//
//            account.setAccountGroup(accountGroup)
//            accountGroup.addAccount(account)
//        }
//
//        account.update(dto)
//
//        return Account.toDto(account)
//    }
//
//    fun delete(id: Long?) {
//        val account: Account = accountRepository!!.findById(id!!)
//            .orElseThrow({ IllegalArgumentException("사용자를 찾을 수 없습니다.") })
//
//
//        // 북마크 삭제
//        bookmarkRepository.deleteAllByAccount(account)
//
//
//        // 사용자 그룹에서 제거
//        if (account.getAccountGroup() != null) {
//            account.getAccountGroup().removeAccount(account)
//        }
//
//        accountRepository.delete(account)
//    }
//
//    fun deleteAll(ids: MutableList<Long?>) {
//        ids.forEach(Consumer { id: Long? -> this.delete(id) })
//    }
//
//    @Transactional(readOnly = true)
//    fun findByUsername(name: String?): AccountDto {
//        val account: Account? = accountRepository!!.findByUserid(name!!)
//            .orElseThrow({ IllegalArgumentException("사용자를 찾을 수 없습니다.") })
//        return Account.toDto(account)
//    }
//
//    @Transactional
//    fun updateLastLoginTime(userid: String?) {
//        val account: Account = accountRepository!!.findByUserid(userid!!)
//            .orElseThrow({ IllegalArgumentException("사용자를 찾을 수 없습니다.") })
//        account.changeLastLoginTime()
//    }
//
//    @Transactional
//    fun updateExpirationTime(userid: String?, sessionTimeoutMinutes: Long?) {
//        val account: Account = accountRepository!!.findByUserid(userid!!)
//            .orElseThrow({ IllegalArgumentException("사용자를 찾을 수 없습니다.") })
//
//
//        // expirationTime이 null이거나 0이면 기본값 사용
//        val timeout = if (sessionTimeoutMinutes == null || sessionTimeoutMinutes <= 0) 30 else sessionTimeoutMinutes
//
//        account.changeExpirationTime(timeout)
//    }
//
//    @Transactional(readOnly = true)
//    fun findByUserid(userid: String?): Optional<Account?> {
//        return accountRepository!!.findByUserid(userid!!)
//    }
//
//    val currentAccount: Optional<Account?>?
//        // 현재 로그인한 사용자의 계정 정보 가져오기
//        get() {
//            val authentication: Authentication? =
//                SecurityContextHolder.getContext().getAuthentication()
//            if (authentication == null || !authentication.isAuthenticated()) {
//                return Optional.empty<Account?>()
//            }
//
//            val userId = authentication.getName()
//            return accountRepository!!.findByUserid(userId!!)
//        }
//
//    val currentUserAccessibleDrawingIds: MutableList<Long?>
//        // 현재 로그인한 사용자의 접근 가능한 도면 ID 목록 가져오기
//        get() = this.currentAccount
//            .map<Any>(Function { account: Account? ->
//                account.getAccountGroup().getAccessibleDrawings().stream()
//                    .map(Drawing::getId)
//                    .collect(Collectors.toList())
//            })
//            .orElse(mutableListOf<Any?>())
//
//    val currentUserAccessibleMenuPaths: MutableList<String?>
//        // 현재 로그인한 사용자의 접근 가능한 메뉴 경로 목록 가져오기
//        get() = this.currentAccount
//            .map<Any>(Function { account: Account? ->
//                account.getAccountGroup().getAccessibleMenus().stream()
//                    .collect(Collectors.toList())
//            })
//            .orElse(mutableListOf<Any?>())
//
//    // 현재 사용자가 특정 도면에 접근 가능한지 확인
//    fun canAccessDrawing(drawingId: Long?): Boolean {
//        return this.currentUserAccessibleDrawingIds.contains(drawingId)
//    }
//
//    // 현재 사용자가 특정 메뉴에 접근 가능한지 확인
//    fun canAccessMenu(menuPath: String?): Boolean {
//        return this.currentUserAccessibleMenuPaths.contains(menuPath)
//    }
}

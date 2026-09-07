package com.pluxity.aiot.sms

import com.pluxity.aiot.sms.dto.UmsSendResultRow
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

private fun history(
    targetNumber: String,
    clidx: Long? = null,
) = SmsHistory(
    targetNumber = targetNumber,
    title = "제목",
    message = "내용",
    stat = UmsSendStat.SUCCESS,
    clidx = clidx,
)

/**
 * 이미 발송된 문자의 이력이 바깥 트랜잭션 롤백에 휩쓸리지 않는지, 실제 트랜잭션으로 확인한다.
 * mock으로는 REQUIRES_NEW를 지워도 드러나지 않는다.
 */
@SpringBootTest
@ActiveProfiles("test")
class SmsHistoryServiceKoTest(
    private val smsHistoryService: SmsHistoryService,
    private val smsHistoryRepository: SmsHistoryRepository,
    private val smsFacade: SmsFacade,
    transactionManager: PlatformTransactionManager,
) : BehaviorSpec({
        extension(SpringExtension(SpringTestLifecycleMode.Root))

        val transaction = TransactionTemplate(transactionManager)

        // beforeEach는 컨테이너 바디가 만든 데이터까지 지우므로, 정리는 각 When 바디 첫 줄에서 한다
        afterEach { smsHistoryRepository.deleteAll() }

        Given("바깥 트랜잭션 안에서 이력을 저장") {
            When("저장 뒤 바깥 트랜잭션이 롤백됨") {
                smsHistoryRepository.deleteAll()
                shouldThrowExactly<IllegalStateException> {
                    transaction.execute {
                        smsHistoryService.saveAll(listOf(history("01011112222"), history("01033334444")))
                        throw IllegalStateException("바깥 작업 실패")
                    }
                }

                Then("이미 나간 문자의 이력은 남는다") {
                    val saved = smsHistoryRepository.findAll()
                    saved shouldHaveSize 2
                    saved.map { it.targetNumber }.toSet() shouldBe setOf("01011112222", "01033334444")
                }
            }
        }

        Given("결과 조회 대상") {
            When("확정되지 않은 건과 확정된 건이 섞여 있음") {
                smsHistoryRepository.deleteAll()
                val pending = smsHistoryRepository.save(history("01011112222", clidx = 100L))
                val noClidx = smsHistoryRepository.save(history("01022223333"))
                val done = smsHistoryRepository.save(history("01033334444", clidx = 200L))
                smsHistoryService.applyResults(mapOf(done.requiredId to UmsSendResultRow(903, 333, "0", "S", null)))

                val targets = smsHistoryService.findPending(10, LocalDateTime.now().minusHours(24))

                Then("확정되지 않고 clidx가 있는 건만 나온다") {
                    targets.map { it.id } shouldBe listOf(pending.requiredId)
                    targets.single().clidx shouldBe 100L
                    smsHistoryRepository
                        .findById(noClidx.requiredId)
                        .get()
                        .resultCode
                        .shouldBeNull()
                }
            }

            When("정의되지 않은 RESULT를 받은 건") {
                smsHistoryRepository.deleteAll()
                val target = smsHistoryRepository.save(history("01044445555", clidx = 300L))
                smsHistoryService.applyResults(mapOf(target.requiredId to UmsSendResultRow(907, null, null, null, null)))

                val targets = smsHistoryService.findPending(10, LocalDateTime.now().minusHours(24))

                Then("원본 코드가 남고 다시 조회되지 않는다") {
                    val reloaded = smsHistoryRepository.findById(target.requiredId).get()
                    reloaded.resultCode shouldBe 907
                    reloaded.result.shouldBeNull()
                    reloaded.resultCheckedAt.shouldNotBeNull()
                    targets.shouldHaveSize(0)
                }
            }

            When("행은 왔지만 RESULT가 비어 있는 건") {
                smsHistoryRepository.deleteAll()
                val target = smsHistoryRepository.save(history("01055556666", clidx = 400L))
                val confirmed =
                    smsHistoryService.applyResults(mapOf(target.requiredId to UmsSendResultRow(null, 333, null, "S", null)))

                val targets = smsHistoryService.findPending(10, LocalDateTime.now().minusHours(24))

                Then("확정되지 않아 다음 주기에 다시 조회된다") {
                    confirmed shouldBe 0
                    targets.map { it.id } shouldBe listOf(target.requiredId)
                    smsHistoryRepository
                        .findById(target.requiredId)
                        .get()
                        .resultCheckedAt
                        .shouldNotBeNull()
                }
            }
        }

        Given("트랜잭션 안에서 발송을 시도") {
            When("바깥 트랜잭션이 열려 있는 채로 send를 호출") {
                smsHistoryRepository.deleteAll()
                Then("커넥션을 붙잡지 않도록 즉시 거부한다") {
                    val exception =
                        shouldThrowExactly<IllegalStateException> {
                            transaction.execute { smsFacade.send("제목", "내용", listOf("01011112222")) }
                        }
                    exception.message.shouldNotBeNull() shouldContain "트랜잭션 밖에서"
                    smsHistoryRepository.count() shouldBe 0L
                }
            }

            When("동기 AFTER_COMMIT 콜백에서 호출") {
                smsHistoryRepository.deleteAll()
                // @TransactionalEventListener(AFTER_COMMIT)이 내부적으로 쓰는 경로와 같다
                var thrown: Throwable? = null
                transaction.execute {
                    TransactionSynchronizationManager.registerSynchronization(
                        object : TransactionSynchronization {
                            override fun afterCommit() {
                                thrown = runCatching { smsFacade.send("제목", "내용", listOf("01011112222")) }.exceptionOrNull()
                            }
                        },
                    )
                    null
                }

                Then("커밋 후에도 커넥션이 남아 있으므로 거부한다") {
                    thrown.shouldBeInstanceOf<IllegalStateException>()
                    smsHistoryRepository.count() shouldBe 0L
                }
            }

            When("트랜잭션 밖에서 호출") {
                smsHistoryRepository.deleteAll()
                val results = smsFacade.send("제목", "내용", listOf("01011112222"))

                Then("미연동 환경이라 발송은 생략되지만 이력은 남는다") {
                    results.single().stat shouldBe UmsSendStat.NOT_SENT
                    smsHistoryRepository.count() shouldBe 1L
                }
            }
        }
    })

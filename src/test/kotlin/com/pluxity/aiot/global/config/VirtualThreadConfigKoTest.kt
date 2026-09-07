package com.pluxity.aiot.global.config

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.TaskScheduler
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.concurrent.CompletableFuture

/** 빈 타입만 봐서는 스레드 종류를 알 수 없어 실제로 작업을 돌려 확인한다. */
@SpringBootTest
@ActiveProfiles("test")
class VirtualThreadConfigKoTest(
    @param:Qualifier("taskExecutor") private val taskExecutor: AsyncTaskExecutor,
    @param:Qualifier("taskScheduler") private val taskScheduler: TaskScheduler,
    @param:Qualifier("heartBeatScheduler") private val heartBeatScheduler: TaskScheduler,
) : BehaviorSpec({
        extension(SpringExtension(SpringTestLifecycleMode.Root))

        fun threadOf(run: (Runnable) -> Unit): Thread {
            val thread = CompletableFuture<Thread>()
            run(Runnable { thread.complete(Thread.currentThread()) })
            return thread.get()
        }

        Given("@Async가 쓰는 taskExecutor") {
            val thread = threadOf { task -> taskExecutor.execute(task) }

            Then("가상 스레드에서 돈다") {
                thread.isVirtual shouldBe true
            }

            Then("이름으로 출처를 알 수 있다") {
                thread.name shouldStartWith "app-async-"
            }
        }

        Given("@Scheduled가 쓰는 taskScheduler") {
            val thread = threadOf { task -> taskScheduler.schedule(task, Instant.now()) }

            Then("가상 스레드에서 돈다") {
                thread.isVirtual shouldBe true
            }

            Then("이름으로 출처를 알 수 있다") {
                thread.name shouldStartWith "app-sched-"
            }
        }

        Given("STOMP 하트비트 전용 스케줄러") {
            val thread = threadOf { task -> heartBeatScheduler.schedule(task, Instant.now()) }

            Then("5초 간격 작업이 밀리지 않도록 전용 플랫폼 스레드를 유지한다") {
                thread.isVirtual shouldBe false
                thread.name shouldStartWith "stomp-heartbeat-"
            }
        }
    })

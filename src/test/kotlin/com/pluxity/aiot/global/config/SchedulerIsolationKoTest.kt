package com.pluxity.aiot.global.config

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.concurrent.CompletableFuture

/**
 * 하트비트와 @Scheduled 배치가 스레드 풀을 나눠 쓰지 않는지 확인한다.
 *
 * taskScheduler에 @Primary가 붙어 있으면 @Primary가 파라미터 이름 매칭을 이겨
 * WebSocketConfig의 heartBeatScheduler 자리에 배치용 빈이 주입된다.
 * 코드를 읽어서는 드러나지 않고 컨텍스트를 띄워야만 드러나는 오배선이라 단언으로 남긴다.
 */
@SpringBootTest
@ActiveProfiles("test")
class SchedulerIsolationKoTest(
    private val webSocketConfig: WebSocketConfig,
    private val scheduledAnnotationBeanPostProcessor: ScheduledAnnotationBeanPostProcessor,
    @param:Qualifier("heartBeatScheduler") private val heartBeatScheduler: TaskScheduler,
    @param:Qualifier("taskScheduler") private val taskScheduler: TaskScheduler,
) : BehaviorSpec({
        extension(SpringExtension(SpringTestLifecycleMode.Root))

        Given("TaskScheduler 빈이 하트비트용과 배치용으로 나뉘어 있을 때") {
            When("WebSocketConfig에 주입된 스케줄러를 보면") {
                val injected = ReflectionTestUtils.getField(webSocketConfig, "heartBeatScheduler")

                Then("배치용이 아니라 하트비트용 빈이어야 한다") {
                    injected shouldBeSameInstanceAs heartBeatScheduler
                    injected shouldNotBe taskScheduler
                }

                Then("두 빈은 서로 다른 스레드 풀이어야 한다") {
                    (heartBeatScheduler as ThreadPoolTaskScheduler).threadNamePrefix shouldBe "stomp-heartbeat-"
                    (taskScheduler as ThreadPoolTaskScheduler).threadNamePrefix shouldBe "scheduled-"
                }
            }
        }

        Given("taskScheduler에서 @Primary를 뺀 상태") {
            When("@Scheduled 인프라가 고른 스케줄러로 작업을 실행하면") {
                val registrar =
                    ReflectionTestUtils.getField(scheduledAnnotationBeanPostProcessor, "registrar") as ScheduledTaskRegistrar
                val resolved = registrar.scheduler.shouldNotBeNull()
                val threadName = CompletableFuture<String>()
                resolved.schedule({ threadName.complete(Thread.currentThread().name) }, Instant.now())

                Then("이름으로 배치용 빈을 찾아 하트비트 풀을 쓰지 않는다") {
                    threadName.get() shouldStartWith "scheduled-"
                }
            }
        }
    })

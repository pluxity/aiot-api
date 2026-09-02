package com.pluxity.aiot.global.lifecycle

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.SmartLifecycle
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

/**
 * 외부 연동의 기동을 담당하되, 초기화가 실패하면 성공할 때까지 백오프를 두고 재시도한다.
 *
 * Spring은 [SmartLifecycle.start]를 다시 호출하지 않으므로, 업체 서버가 늦게 뜨거나
 * 일시적으로 응답하지 않으면 재시작 전까지 연동이 멈춘 채로 남는다. 이를 막기 위한 공통 기반이다.
 *
 * @param name 로그에 표시할 연동 이름
 * @param initialRetryDelaySeconds 첫 재시도까지 대기 시간
 * @param maxRetryDelaySeconds 재시도 간격 상한
 */
abstract class RetryingLifecycle(
    private val name: String,
    private val initialRetryDelaySeconds: Long = DEFAULT_INITIAL_RETRY_DELAY_SECONDS,
    private val maxRetryDelaySeconds: Long = DEFAULT_MAX_RETRY_DELAY_SECONDS,
) : SmartLifecycle {
    private val scheduler =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "$name-lifecycle-retry").apply { isDaemon = true }
        }

    private val lock = Any()

    /**
     * 라이프사이클 활성 여부. 초기화 성공 여부와 별개다.
     *
     * DefaultLifecycleProcessor는 isRunning()이 true인 빈에만 stop()을 호출하므로,
     * 재시도 대기 중에도 true여야 종료 시 예약된 재시도가 취소된다.
     */
    @Volatile
    private var active = false

    @Volatile
    private var stopped = false

    @Volatile
    private var initialized = false

    @Volatile
    private var retryTask: ScheduledFuture<*>? = null

    /** 초기화가 성공해 연동이 실제로 동작 중인지 */
    val isInitialized: Boolean
        get() = initialized

    /**
     * 연동 초기화. 재시도로 여러 번 호출될 수 있으므로 멱등해야 한다.
     * 실패는 예외로 알린다.
     */
    protected abstract fun initialize()

    /** 연동 종료. 초기화가 끝나지 않은 상태에서도 호출될 수 있다 */
    protected abstract fun shutdown()

    final override fun start() {
        synchronized(lock) {
            stopped = false
            active = true
        }
        log.info { "$name 연동 시작..." }
        attempt(initialRetryDelaySeconds)
    }

    final override fun stop() {
        log.info { "$name 연동 종료..." }
        synchronized(lock) {
            stopped = true
            active = false
            initialized = false
            retryTask?.cancel(false)
            retryTask = null
            shutdown()
        }
    }

    final override fun isRunning(): Boolean = active

    override fun getPhase(): Int = Int.MAX_VALUE - 1

    private fun attempt(retryDelaySeconds: Long) {
        if (stopped) return

        try {
            initialize()
        } catch (e: Exception) {
            synchronized(lock) {
                initialized = false
                if (stopped) return
                log.error(e) { "$name 연동 초기화 실패, ${retryDelaySeconds}초 후 재시도: ${e.message}" }
                retryTask =
                    scheduler.schedule(
                        { attempt((retryDelaySeconds * 2).coerceAtMost(maxRetryDelaySeconds)) },
                        retryDelaySeconds,
                        TimeUnit.SECONDS,
                    )
            }
            return
        }

        synchronized(lock) {
            if (stopped) {
                // 초기화 도중 stop()이 호출됐다면, 방금 되살린 자원을 다시 정리한다
                log.info { "$name 연동 초기화 중 종료 요청, 정리합니다" }
                shutdown()
                return
            }
            initialized = true
            log.info { "$name 연동 초기화 완료" }
        }
    }

    companion object {
        private const val DEFAULT_INITIAL_RETRY_DELAY_SECONDS = 10L
        private const val DEFAULT_MAX_RETRY_DELAY_SECONDS = 300L
    }
}

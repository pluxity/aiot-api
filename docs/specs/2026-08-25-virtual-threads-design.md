# 가상 스레드 도입 및 의존성 버전 정렬 설계

- 작성일: 2026-08-25
- 대상 모듈: `aiot-api` (단일 모듈)
- 현재 환경: Java 25, Kotlin 2.3.10, Spring Boot 4.0.3, Spring Framework 7.0.5
- 목표 환경: Java 25, Kotlin 2.4.10, Spring Boot 4.1.0, Spring Framework 7.0.8
- 함께 읽을 것: `docs/specs/2026-08-25-auth-alignment-design.md` (JWT/인증 정렬 — jjwt 제거 포함)
- 참조: `safers-api/docs/specs/2026-08-03-virtual-threads-design.md` — 판정 기준과 결론을 그대로 따르되,
  aiot 의 실제 jar / 빈 구성으로 전부 재확인했다. **결과가 safers 와 다른 지점이 두 곳 있다(§2.2, §4.2).**

### 외부 리뷰 반영 (Codex, 2026-08-25)

| 지적 | 판정 | 반영 |
|---|---|---|
| 썸네일 크기 검사가 `bodyTo(ByteArray)` **이후**라 상한을 복원하지 못한다 (chunked/과소 신고 시 전량 적재) | **타당** — "복원한다" 는 서술이 틀렸다 | §6.6 을 `response.body` + `readNBytes(상한+1)` 스트림 판정으로 교체, §6.5 서술 정정, §8.3 확인 항목 추가 |
| `retrieve()` 는 4xx/5xx 에서 던지므로 `response.code` 검사에 도달하지 못한다 — EDS 도메인 에러 매핑이 사라지고 500 이 나간다 | **타당** — "썸네일만 형태가 다르다" 고 단정한 것이 틀렸다. 7개 전부 에러 계약이 다르다 | §6.5 에 `throwOnHttpError` 옵션 추가, §6.6 에 계약 설명 + `EdsClient` 생성 변경, §8.3 확인 항목 추가 |
| `DeviceStatus` 가 레포에도 문서에도 정의돼 있지 않고, 생성 인자(`LocationData`)와 접근 프로퍼티(`longitude`/`latitude`)가 어긋난다 | **타당** — 그대로는 컴파일되지 않는다 | §6.8 에 `private data class DeviceStatus` 정의 추가, 생성부를 평탄한 인자로 교체 |
| `MobiusProperties` 가 존재하지 않는데 클래스·배선·yml 키 정의 없이 사용한다 | **타당** — 레포에 `MobiusConfig`/`MobiusConfigService` 만 있고 `MobiusProperties` 는 없다 | **설정 클래스를 만들지 않는 방향으로 변경**(사용자 결정). `max(코어,8)×2` 계산식으로 대체해 §6.3 의 "측정 없이 상수를 승격시키지 않는다" 원칙과 맞춤. §9-6·§11-3 갱신 |
| §10 커밋 6 이 `WebClientFactory`/`webClientBuilder` 를 제거하는데 소비자 3곳이 7~9 커밋에 남아 있어 **중간 커밋이 빌드되지 않는다** — "각 커밋에서 빌드 통과" 전제와 모순 | **타당** — 이분탐색을 위해 커밋을 나눠놓고 정작 이분탐색을 불가능하게 만들었다 | 커밋 6 을 "추가" 로 바꾸고 제거를 커밋 10 으로 분리, §6.5 에 존치 규칙 명시 |
| `EdsClient` 는 6개가 아니라 **7개** 다 — `keepAlive` 가 마이그레이션 목록과 회귀 확인에서 빠졌다. 그 catch 가 `login()` 을 불러 `apiKey` 를 갱신하는 경로다 | **타당** — 문서 5곳에서 6개로 세고 있었다 | 전 지점을 7개로 정정, §6.6 에 메서드 표와 `keepAlive` 주의 추가, §8.3 에 재로그인 경로 확인 항목 추가 |
| `throwOnHttpError` 를 클라이언트 단위로만 봤다. `AiotService` 는 `fetchDeviceBatteryData`(non-2xx → null), `fetchRemoveSubscription`(로깅 후 진행) 처럼 메서드마다 계약이 달라 `retrieve()` 로 바꾸면 예상된 4xx 가 루프 전체를 끊는다 | **타당** — §6.6 에서 EDS 에 한 검사를 `AiotService` 에는 하지 않았다 | §6.5 에 메서드별 계약 표 추가, 두 메서드는 `.exchange { }` 개별 처리로 명시 |
| `WebClientConfig` 제거로 전역 `Accept: application/json` 이 사라지는데 대체가 없다 | **타당. 그리고 이건 코드 축약 패스가 만든 회귀다** — 원래 §6.5 코드에 있던 `defaultHeaders` 줄을 축약하며 결정 목록에 옮기지 않았다 | §6.5 구성 결정에 복원 |
| `NgrokConfig` 의 연산자 타임아웃(2초·5초)이 사라지고 팩토리 기본값 30초가 적용된다 — `@PostConstruct` 라 기동이 블로킹된다 | **타당** | §6.9 에 전용 클라이언트 타임아웃 지정 추가 |
| §6.8 R4 표가 `setupSubscriptionForFeature` 를 leaf 로 분류했으나 실제로는 `fetchSubscription` 에 위임하고 409 시 `fetchRemoveSubscription` 재시도를 탄다 — 중첩 획득 데드락 | **타당** — 중첩 금지 규칙을 써놓고 바로 아래 표에서 어겼다 | leaf 목록을 `fetchSubscription`/`fetchRemoveSubscription` 으로 정정, 판정 기준을 "이름이 아니라 `client` 직접 호출 여부" 로 명시, §9-10 갱신 |
| `createClient` 마다 executor·HttpClient 가 생기는데 `RestClient` 만 반환해 스프링이 닫을 수 없다 | **타당** — Java 25 에서 `HttpClient` 는 `AutoCloseable` 이다. `@SpringBootTest` 3개가 컨텍스트를 띄우므로 테스트에서 누적된다 | §6.5 를 `DisposableBean` + executor 공유 구조로 변경하고 라이프사이클 항목 추가 |
| `fetchMobiusUril` 의 `?: emptyList()` 가 빈 응답을 정상 빈 목록으로 바꿔 **Feature 전량 삭제**로 이어진다 | **타당** — 5차 반영에서 내가 새로 만든 결함이다. 현재 `awaitBody` 는 예외를 던져 안전했다 | `?: throw CustomException(MOBIUS_EMPTY_RESPONSE)` 로 교체, `RestClient.body()` 의 null 계약을 §6.8·§9-11 에 경고로 명시, §8.3 확인 항목 추가 |
| `@Transactional` 이 `findAll()` 전에 시작해 HTTP fan-out 내내 살아 있다 — "트랜잭션 밖" 이라는 주석이 사실이 아니다. `handleMobiusUrlUpdated` 도 동일 | **타당** — private 메서드로 뺐다고 경계가 생기지 않는다 | §6.8 전면 재작성: 읽기/HTTP/쓰기 3단계 분리, `FeatureQueryService`·`FeatureStatusWriter` 신설(self-invocation 회피), `handleMobiusUrlUpdated`·`checkSynchronization` 의 `@Transactional` 제거 |
| 세마포어가 `fetchAllStatuses` 에만 걸려 다른 Mobius 경로를 덮지 못한다. Reactor Netty 는 호스트당 **집계** 상한이라 경로별 상한의 합이 기존 상한을 넘는다 | **타당** — §6.4 로 08:00 두 cron 이 겹치게 만들어 놓고 상한은 경로별로 뒀다 | 세마포어를 `AiotService` 공유 `mobiusSemaphore` 로 승격하고 **leaf 호출에만** 적용(중첩 획득 데드락 회피). §9-10 한계 추가 |
| §6.8 의 "Mobius 동시 호출이 1이었다" 가 틀렸다. 코루틴은 단일 스레드에서도 동시적이라 이미 동시 호출 중이며, `Semaphore(10)` 은 상한 신설이 아니라 **기존 동시성을 조이는 것** | **타당** — §5.1 에 맞게 써놓고 §6.8 에서 뒤집었다 | §6.8 전면 재작성(Reactor Netty `max(코어,8)×2` 실측 근거 추가, 기본값 10 → 20), §5.1 보강, §6.4·§8.3·§9 갱신 |

## 0. 배경 — 왜 지금인가

이 프로젝트는 **서블릿 MVC + JPA**(`spring-boot-starter-web`, `open-in-view: false`)라는 블로킹 스택인데,
그 위에 WebFlux(`WebClient`/`Mono`)와 코루틴(`suspend`/`runBlocking`) 두 패러다임이 얹혀 있다.

문제는 그 둘이 **아무 이득도 만들지 못하고 있다**는 점이다.

| 지점 | 현재 | 실제 효과 |
|---|---|---|
| `LlmMessageController:29` | `runBlocking { ... }` | 톰캣 스레드를 그대로 점유. 논블로킹 이득 0 |
| `EdsClient` 7개 메서드 | `exchangeToMono { }.block()` | Reactor Netty 스택을 지고 동기 호출 |
| `NgrokConfig:53,90` | `.block()` | 같음 |
| `AiotService:271,394` | `.block()` | 같음 |
| `AiotService.statusSynchronize` | `runBlocking { supervisorScope { async { } } }` | **디스패처 미지정 → 단일 스레드**. §5.1 |
| `FeatureScheduler:104` | 같은 패턴 | 같음 |

동시에 `safers-api` 와 라이브러리 버전이 벌어졌다. 두 프로젝트가 같은 Boot 4 / Java 25 기반이고
`kotlin-jdsl`·`springwolf`·`kotest` 처럼 마이그레이션 비용이 큰 라이브러리를 공유하므로,
한쪽에서 이미 검증한 조합으로 맞추는 편이 싸다.

## 1. 개요 / 범위

`spring.threads.virtual.enabled` 를 켜고, **리액티브 타입을 걷어내되 리액티브가 실제로 필요한 한 곳은 남긴다.**
동시에 의존성 버전을 `safers-api` 기준으로 정렬한다.

### 사전 검증 결과 (2026-08-25, aiot 실제 jar / 빈 구성 기준)

| # | 확인 대상 | 결과 |
|---|---|---|
| ① | Tomcat 요청 스레드가 가상 스레드로 바뀌는가 | **바뀐다.** `spring-boot-tomcat-4.0.3` 의 `TomcatVirtualThreadsWebServerFactoryCustomizer` 가 `protocolHandler.setExecutor(new VirtualThreadExecutor("tomcat-handler-"))` 수행 (§2.1) |
| ② | `@Async` 자동설정이 백오프하는가 | **백오프한다.** 단 **aiot 에는 `@Async` 사용처가 0개**라 무해하다 — safers 와 갈리는 지점 (§2.2) |
| ③ | `@Scheduled` 가 어디서 도는가 | **익명 단일 스레드에서 직렬로 돈다.** `@Scheduled` 3개 중 2개가 같은 시각(08:00) cron 이다 (§2.3) |
| ④ | JDK 25 에서 `synchronized` pinning 이 남아 있는가 | **없다.** JEP 491(JDK 24)로 해소 (§2.4) |
| ⑤ | `taskExecutor` 빈(5/10/500)을 쓰는 곳이 있는가 | **없다.** `@Async` 0개, STOMP 채널도 쓰지 않는다 (§4.2) |
| ⑥ | HikariCP 기본값이 가상 스레드에서 위험한가 | **아니다.** `connection-timeout` 기본이 무한이 아니라 **30초**(`HikariCP-7.0.2` `CONNECTION_TIMEOUT = 30`). 설정을 바꾸지 않는다 (§6.3) |
| ⑦ | `@EnableWebSocketMessageBroker` 가 `TaskScheduler` 후보를 몇 개 추가하는가 | **3개.** `messageBrokerTaskScheduler` + `clientInbound/OutboundChannelExecutor`. 운영 로그로 확정 (§2.3) |
| ⑧ | Boot 의 `ClientHttpRequestFactoryBuilder` 를 쓸 수 있는가 | **못 쓴다.** `org.springframework.boot.http.client.*` 는 `spring-boot-restclient` 모듈에 있고 aiot 런타임 클래스패스에 없다. `spring-web` 의 `JdkClientHttpRequestFactory` 를 직접 쓴다 (§6.5) |

### 이번 범위 (In scope)

**A. 가상 스레드**
- `spring.threads.virtual.enabled: true` (`application-common.yml`)
- `AsyncConfig`(`global/config/WebSocketConfig.kt` 두 번째 클래스) — `taskExecutor` 가상 스레드 교체,
  `taskScheduler` 명시 추가, `heartBeatScheduler` 주입 지점 고정

**B. 리액티브/코루틴 제거 — HTTP 클라이언트**
- `WebClientFactory` → `RestClientFactory` (JDK `HttpClient` 기반)
- `EdsClient`, `NgrokConfig`, `AiotService`, `LlmMessageService` 의 `.block()` / `awaitBody` 제거
- `suspend` / `runBlocking` / `async` / `Dispatchers.IO` 전량 제거

**C. 의존성 버전 정렬** (§3)
- Boot 4.0.3 → 4.1.0, Kotlin 2.3.10 → 2.4.10, Gradle 9.3.1 → 9.7.0
- `kotlin-jdsl` 3.8.0 → 3.9.0, `springwolf` 2.0.0 → 2.4.0, `kotest` 5.9.1 → 6.2.4
- `influxdb-client-kotlin` → `influxdb-client-java`
- `kotlinx-coroutines-*`, `spring-boot-starter-webflux` **제거하지 않는다** — §1.1 참조

**D. 기존 결함 수정 (가상 스레드와 무관하나 같이 잡는다)**
- `EdsWebSocketClient` 의 stale api-key 재연결 (§6.10)
- `stopped` / `disposable` 가시성 (§6.10)

### 이번 제외 (Out of scope)

| 항목 | 이유 |
|---|---|
| **`EdsWebSocketClient` 의 WebFlux 제거** | **사용자 결정.** `ReactorNettyWebSocketClient` → `StandardWebSocketClient` 재작성은 `repeatWhen`/`retryWhen` 백오프를 직접 구현해야 하고 재연결 경로 검증이 어렵다. 이 파일 하나 때문에 `spring-boot-starter-webflux` 는 남는다 (§7) |
| `EdsClient` 를 `@HttpExchange` 인터페이스로 전환 | 리팩터링 범위가 커진다. RestClient 전환을 먼저 하고 재판단 (§11) |
| 버전 카탈로그(`libs.versions.toml`) 도입 | 단일 모듈이라 이득이 적다. 멀티모듈화 시점에 (§3.6, §11) |
| Flyway 도입 | aiot 는 `ddl-auto` 기반. 별건 |
| `SensorDataMigrationService` 의 `newScheduledThreadPool(4)` | (B) 유형 — 디바이스별 타이머 관리용이지 스레드 공급용이 아니다 (§4.2) |
| **HikariCP 설정 변경** | 측정 없이 값을 바꾸지 않는다. 기본값이 안전한 이유는 §6.3 |
| MDC traceId 전파 | safers 의 `2026-08-04-mdc-trace-id-design.md` 에 해당. 별건 (§11) |
| **JWT/인증 정렬 (jjwt 제거 포함)** | 별도 설계문서 `2026-08-25-auth-alignment-design.md`. 의존성 커밋만 §10 과 순서를 맞춘다 |

### 1.1 `webflux` / `coroutines` 의존성을 남기는 이유

`EdsWebSocketClient` 가 `org.springframework.web.reactive.socket.*` 를 쓰므로 `spring-boot-starter-webflux`
는 남는다. `kotlinx-coroutines-core` / `-reactor` 는 §5 를 전부 적용하면 **사용처가 0이 되므로 제거 가능**하다.
다만 `webflux` 가 남는 이상 큰 실익이 없고, 제거했다가 되돌리는 비용이 있으므로
**`build.gradle.kts` 에 주석으로 "사용처 없음, EdsWebSocketClient 전환 시 함께 제거" 를 남기고 유지한다.**

> 판단 근거: 의존성 제거는 되돌리기 쉬우나, "왜 남겼는지" 를 잃으면 다음 사람이 다시 조사한다.
> 주석 한 줄이 그 조사를 막는다.

## 2. 사전 검증 상세

### 2.1 Tomcat 요청 스레드 — 적용됨

```java
// spring-boot-tomcat-4.0.3, TomcatVirtualThreadsWebServerFactoryCustomizer (바이트코드 확인)
new VirtualThreadExecutor("tomcat-handler-")
ProtocolHandler.setExecutor(java.util.concurrent.Executor)
```

재현:

```bash
unzip -o -q ~/.gradle/caches/modules-2/files-2.1/org.springframework.boot/spring-boot-tomcat/4.0.3/*/spring-boot-tomcat-4.0.3.jar -d /tmp/tc403
javap -v -cp /tmp/tc403 org.springframework.boot.tomcat.autoconfigure.TomcatVirtualThreadsWebServerFactoryCustomizer | grep VirtualThreadExecutor
```

요청 스레드가 가상 스레드가 되면 JPA/JDBC, S3, Redis, InfluxDB, 그리고 §6 에서 RestClient 로 바뀔
HTTP 호출이 전부 캐리어를 놓는다. **이 설정의 실질 이득은 대부분 여기서 나온다.**

### 2.2 `@Async` — 자동설정은 백오프하지만, aiot 에서는 무해하다

`TaskExecutorConfigurations$OnExecutorCondition` 은 `AnyNestedCondition` 이다 (바이트코드 확인).

```
OnExecutorCondition
├─ ModelCondition        : @ConditionalOnProperty("spring.task.execution.mode", havingValue="force")
└─ ExecutorBeanCondition : @ConditionalOnMissingBean(java.util.concurrent.Executor.class)
```

재현:

```bash
javap -v -cp /tmp/ac403 'org.springframework.boot.autoconfigure.task.TaskExecutorConfigurations$OnExecutorCondition$ExecutorBeanCondition' | grep -A3 ConditionalOnMissingBean
```

aiot 의 `Executor` 타입 빈은 **6개**다. `ThreadPoolTaskScheduler` 도 `Executor` 라는 점에 주의한다.

```
public class ThreadPoolTaskScheduler extends ExecutorConfigurationSupport
    implements AsyncTaskExecutor, SchedulingTaskExecutor, TaskScheduler
                  └─ TaskExecutor └─ java.util.concurrent.Executor
```

| 빈 | 타입 | Executor 인가 |
|---|---|---|
| `taskExecutor` (`AsyncConfig`, `@Primary`) | `ThreadPoolTaskExecutor` | O |
| `heartBeatScheduler` (`AsyncConfig`) | `ThreadPoolTaskScheduler` | **O** |
| `messageBrokerTaskScheduler` (프레임워크) | `ThreadPoolTaskScheduler` | **O** |
| `clientInboundChannelExecutor` (프레임워크) | `ThreadPoolTaskExecutor` | **O** |
| `clientOutboundChannelExecutor` (프레임워크) | `ThreadPoolTaskExecutor` | **O** |
| `brokerChannelExecutor` (프레임워크) | `ThreadPoolTaskExecutor` | **O** |

→ `ExecutorBeanCondition` 불일치, `spring.task.execution.mode` 도 없어 `ModelCondition` 도 불일치
→ **자동설정이 만들어지지 않아 `applicationTaskExecutor` 가 없다.**

**여기서 safers 와 갈린다.** safers 는 무자격 `@Async` 3곳이 `new SimpleAsyncTaskExecutor()`(플랫폼 스레드)
폴백으로 떨어지는 게 문제였다. **aiot 에는 `@Async` 사용처가 0개다.**

```bash
grep -rn "@Async" --include="*.kt" src/main/kotlin
# → StompMessageSender 의 @AsyncPublisher 3건만 나온다.
#   springwolf 문서화 어노테이션이지 org.springframework.scheduling.annotation.Async 가 아니다.
```

즉 **`@EnableAsync` 와 `taskExecutor`(5/10/500) 는 아무도 쓰지 않는 죽은 코드다.** `applicationTaskExecutor`
부재가 문제되는 다른 경로(MVC async: `Callable`/`DeferredResult`/`SseEmitter`/`StreamingResponseBody`)도
검색 결과 0건이라 영향이 없다.

```bash
grep -rn "SseEmitter\|DeferredResult\|StreamingResponseBody\|Callable<" --include="*.kt" src/main/kotlin
# → 0건
```

#### 그래도 `taskExecutor` 를 지우지 않고 가상 스레드로 교체한다

지워도 §2.2 표대로 `Executor` 빈이 5개 남아 자동설정은 여전히 백오프한다. 지우는 것과 남기는 것의 차이는
"나중에 누가 `@Async` 를 붙였을 때 어디로 가는가" 뿐이다.

| | `taskExecutor` 를 지웠을 때 | 가상 스레드로 교체했을 때 |
|---|---|---|
| 새 `@Async` 의 행선지 | `AsyncExecutionInterceptor` 의 `new SimpleAsyncTaskExecutor()` — **플랫폼 스레드, 무제한, 스프링 관리 밖** | 이 빈 — 가상 스레드, graceful shutdown 대상 |

**후자가 낫다.** 비용은 빈 하나이고, `@EnableAsync` 도 함께 유지한다.

> `spring.task.execution.mode: force` 로 자동설정을 살리는 대안은 채택하지 않는다.
> 명시 빈 쪽이 §2.3 의 `taskScheduler` 처리와 대칭이고, 빈 개수 변화에 영향받지 않는다.

### 2.3 `@Scheduled` — 익명 단일 스레드가 돌고 있다 (이번 작업의 실질 수확)

`TaskSchedulingConfigurations$TaskSchedulerConfiguration` 은
`@ConditionalOnMissingBean({TaskScheduler.class, ScheduledExecutorService.class})` 이고
`@Bean(name = "taskScheduler")` 를 만든다 (바이트코드 확인).

**운영 로그로 확정했다 (2026-08-25).** 후보는 2개가 아니라 **4개**다.

```
INFO  o.s.s.config.TaskSchedulerRouter - More than one TaskScheduler bean exists within the context,
and none is named 'taskScheduler'. ...
[heartBeatScheduler, clientInboundChannelExecutor, clientOutboundChannelExecutor, messageBrokerTaskScheduler]
```

| 빈 | 출처 | 제거 가능? |
|---|---|---|
| `heartBeatScheduler` (`AsyncConfig:55`) | 직접 선언, `ThreadPoolTaskScheduler()` poolSize **1** | 불가 — 아래 |
| `messageBrokerTaskScheduler` | **프레임워크** — `AbstractMessageBrokerConfiguration` | 불가 |
| `clientInboundChannelExecutor` | **프레임워크** — 동일 | 불가 |
| `clientOutboundChannelExecutor` | **프레임워크** — 동일 | 불가 |

재현:

```bash
javap -v -cp <spring-messaging-7.0.5 전개경로> \
  org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration | grep messageBrokerTaskScheduler
#   name=["messageBrokerTaskScheduler","messageBrokerSockJsTaskScheduler"]
```

**4개 중 3개가 프레임워크 등록이라 제거할 수 없다.** `heartBeatScheduler` 도 지울 수 없다 —
`SimpleBrokerRegistration.getMessageHandler()` 는 `taskScheduler` 가 명시되지 않으면 하트비트를
`0,0`(비활성)으로 두고 `messageBrokerTaskScheduler` 를 자동으로 집어오지 않는다. 지우면 STOMP 하트비트가
조용히 꺼진다.

→ **이름이 `taskScheduler` 인 빈을 두는 것이 유일한 해법이다**(§6.4). 빈을 정리해서 해소하는 경로는 없다.

> **정적 분석과 어긋나는 지점 — 미해결로 남긴다.** `clientInbound/OutboundChannelExecutor` 는
> `@Bean` 선언 반환 타입이 `java.util.concurrent.Executor` 이고 기본 구현이
> `TaskExecutorRegistration.getTaskExecutor()` → `ThreadPoolTaskExecutor` 인데,
> `ThreadPoolTaskExecutor` 는 `TaskScheduler` 를 구현하지 않는다(`AsyncTaskExecutor`,
> `SchedulingTaskExecutor` 만). 바이트코드만으로는 이 둘이 `TaskScheduler` 후보로 잡히는 이유를
> 규명하지 못했다. **운영 로그를 사실로 채택한다** — 어느 쪽이든 결론(§6.4)은 같고,
> 후보가 4개면 "빈을 줄여서 해결" 가능성이 더 낮아질 뿐이다.

이 상태에서 `TaskSchedulerRouter.determineDefaultScheduler()` 폴백을 탄다.

```java
try { return resolveSchedulerBean(beanFactory, TaskScheduler.class, false); }   // 2개 → NoUnique
catch (NoUniqueBeanDefinitionException ex) {
    try { return resolveSchedulerBean(beanFactory, TaskScheduler.class, true); } // "taskScheduler" 없음
    catch (NoSuchBeanDefinitionException ex2) {
        logger.info("More than one TaskScheduler bean exists within the context, and "
                  + "none is named 'taskScheduler'. Mark one of them as primary ...");
    }
}
ScheduledExecutorService localExecutor = Executors.newSingleThreadScheduledExecutor();  // ← 여기
return new ConcurrentTaskScheduler(localExecutor);
```

**결과: `@Scheduled` 3개가 스레드 1개에서 직렬로 돈다.**

| 위치 | 주기 | 하는 일 |
|---|---|---|
| `FeatureScheduler:40` `checkDisconnect` | cron 매시간 05분 | 전체 Feature 순회 + Influx 질의 + STOMP 발송 |
| `FeatureScheduler:100` `scheduledBatteryDataUpdate` | **cron 매일 08:00** | 전체 Feature 배터리 조회 (Mobius HTTP fan-out) |
| `LlmMessageScheduler:17` `generateDailyMessage` | **cron 매일 08:00** | 전체 Site LLM 메시지 생성 (LLM HTTP fan-out) |

**뒤의 둘이 같은 시각(08:00)이다.** 지금은 한 스레드에서 순차 실행되므로 배터리 갱신이 끝나야 LLM 생성이
시작된다. 둘 다 외부 HTTP fan-out 이라 소요가 길다.

#### 적용 전 확인

두 신호를 구분해서 본다.

| 신호 | 의미 | 시점 | 확인 |
|---|---|---|---|
| INFO 로그 | **원인** — 빈이 2개고 이름이 `taskScheduler` 가 아님 | 기동 시 1회 | `grep "More than one TaskScheduler bean" <로그>` |
| `pool-N-thread-` 스레드명 | **결과** — 익명 스케줄러가 실제로 돌리는 중 | 작업 실행마다 | `grep "pool-[0-9]*-thread-" <로그>` |

- INFO 로그 쪽이 확실하다. 메시지 끝에 `ex.getBeanNamesFound()` 로 실제 빈 이름이 찍힌다.
  **2026-08-25 운영 로그에서 위 4개로 확인 완료 — 이 절의 전제는 확정됐다.**
  로거는 `org.springframework.scheduling.config.TaskSchedulerRouter`, `application.yml` 의 `org: INFO` 로 보인다.
  `defaultScheduler` 가 `SingletonSupplier.of(this::determineDefaultScheduler)` 라 한 번만 평가된다.
- 스레드명은 보조 확인용이다. `logback-spring.xml` 패턴에 `[%thread]` 가 있으면 매시간 05분
  `checkDisconnect` 로그에서 제일 자주 나타난다.

**확인 완료(2026-08-25).** 로그 원문을 PR 에 첨부한다.

### 2.4 JDK 25 pinning

safers 의 실측(§3)을 그대로 인용한다. 캐리어 1개로 고정하고 가상 스레드 4개가 각 500ms 대기 —
직렬화되면 ~2000ms, 병렬이면 ~500ms.

```
Thread.sleep (일반)              508ms  언마운트 O
synchronized 내부 sleep          506ms  언마운트 O
Object.wait(500)               507ms  언마운트 O
ReentrantLock + Condition      503ms  언마운트 O
```

JDK 21~23 의 `synchronized` pinning(JEP 491 이전)은 해소됐다. 남는 요인은 JNI 네이티브 프레임인데
aiot 가 쓰는 pgjdbc·AWS SDK v2·InfluxDB 클라이언트·jjwt 는 모두 순수 Java 라 해당 없다.

**단 aiot 에는 safers 에 없는 것이 하나 있다 — Reactor Netty.** `EdsWebSocketClient` 가 남는 이상
Netty 이벤트 루프 스레드(플랫폼)는 계속 존재한다. 가상 스레드와 무관하게 동작하며 간섭하지 않는다.

## 3. 의존성 버전 정렬

### 3.1 현행 대조표

| 항목 | aiot (현재) | safers-api | 판정 |
|---|---|---|---|
| Gradle | 9.3.1 | **9.7.0** | 올린다 |
| Kotlin | 2.3.10 | **2.4.10** | 올린다 |
| Spring Boot | 4.0.3 | **4.1.0** | 올린다 — §3.3 파급 있음 |
| spring-dependency-management | 1.1.7 | 1.1.7 | 동일 |
| spotless | 8.1.0 | 8.1.0 | 동일 |
| kotlin-jdsl | 3.8.0 | **3.9.0** | 올린다 |
| springdoc | 3.0.1 | 3.0.1 | 동일 |
| springwolf | 2.0.0 | **2.4.0** | 올린다 |
| kotlin-logging | 8.0.01 | 8.0.01 | 동일 |
| logbook | 4.0.2 | 4.0.2 | 동일 |
| p6spy | 2.0.0 | 2.0.0 | 동일 |
| jts | 1.20.0 | 1.20.0 | 동일 |
| aws-s3 | 2.42.3 | 2.42.3 | 동일 |
| influxdb | 7.3.0 (`-kotlin`) | 7.3.0 (`-java`) | **아티팩트 교체** — §3.5 |
| kotest | 5.9.1 | **6.2.4** | 올린다 — §3.4 파급 있음 |
| kotest-extensions-spring | `io.kotest.extensions:1.1.3` | **`io.kotest:6.2.4`** | **groupId 변경** — §3.4 |
| mockk | 1.14.5 | 1.14.5 | 동일 |
| jjwt | 0.12.6 (api/impl/jackson) | **없음 — nimbus-jose-jwt 10.3** | **제거**. 별도 설계문서 `2026-08-25-auth-alignment-design.md` 참조 |
| H2 (test) | 있음 | (Testcontainers) | aiot 고유. 유지 — §3.7 |

### 3.2 원칙

- **safers 에 있는 버전으로만 맞춘다.** safers 보다 최신이 나와 있어도 이번엔 올리지 않는다 —
  검증된 조합을 가져오는 것이 목적이지 최신화가 목적이 아니다.
- **aiot 고유 의존성은 손대지 않는다** (jjwt, redis, hibernate-spatial, H2).
- 버전 정렬과 가상 스레드는 **커밋을 분리한다** (§10). 회귀 시 이분탐색이 가능해야 한다.

### 3.3 Boot 4.0.3 → 4.1.0 파급 — 테스트 슬라이스 분리

safers 의 카탈로그 주석이 남긴 경고를 그대로 옮긴다.

```
# Boot 4.1 은 테스트 슬라이스를 기술별 스타터로 쪼갰다 — @DataJpaTest 는 starter-test 에 없다
spring-boot-starter-data-jpa-test = { module = "org.springframework.boot:spring-boot-starter-data-jpa-test" }
spring-boot-starter-webmvc-test   = { module = "org.springframework.boot:spring-boot-starter-webmvc-test" }
```

aiot 현재 사용 현황을 확인했다. **슬라이스 테스트와 `@SpringBootTest` 는 구분해서 봐야 한다.**

```bash
grep -rn "@DataJpaTest\|@WebMvcTest\|@JdbcTest" --include="*.kt" src/test
# → 0건

grep -rn "@SpringBootTest" --include="*.kt" src/test
# → 3건 (FireAlarmProcessorTest, TemperatureHumidityProcessorTest, DisplacementGaugeProcessorTest)
```

`@SpringBootTest` 는 **슬라이스가 아니라 전체 컨텍스트 로딩**이고 `spring-boot-starter-test` 에 그대로 있다.
쪼개진 것은 `@DataJpaTest`(→ `spring-boot-starter-data-jpa-test`)와
`@WebMvcTest`(→ `spring-boot-starter-webmvc-test`)이며, 이 둘은 aiot 에 0건이다.

**따라서 이번 업그레이드에서 추가 스타터가 필요 없다.** 단 위 3개는 실제 DB(H2)를 띄우므로
업그레이드 회귀 확인 대상이다(§3.7).
다만 `plx-backend:test-controller` 스킬이 `@WebMvcTest` 를 생성하므로, 앞으로 컨트롤러 테스트를
추가할 때 의존성 누락으로 헤맬 수 있다. **`build.gradle.kts` 에 주석으로 남긴다**(§6.1).

### 3.4 Kotest 5.9.1 → 6.2.4

두 가지가 바뀐다.

**(1) `kotest-extensions-spring` 좌표 변경**

```
- testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")
+ testImplementation("io.kotest:kotest-extensions-spring:6.2.4")
```

safers 카탈로그 주석: "Kotest 6 에서 Spring 확장이 코어 저장소로 흡수되며 groupId 가 바뀌었다.
구 좌표(`io.kotest.extensions`)는 1.3.0 에서 멈춰 있고 Kotest 5.8.1 이 대상이다."

**구 좌표를 그대로 두고 kotest 만 6 으로 올리면 런타임에 `NoSuchMethodError` 가 난다.** 반드시 함께 바꾼다.

**(2) 영향 범위 — 실측**

```bash
find src/test -name "*.kt" | wc -l        # 40
grep -rln "io.kotest.extensions.spring" --include="*.kt" src/test | wc -l   # 3
grep -rln "org.junit.jupiter" --include="*.kt" src/test | wc -l             # 0
```

| 대상 | 건수 | 조치 |
|---|---|---|
| `SpringExtension` 사용 스펙 | 3 (`FireAlarmProcessorTest`, `TemperatureHumidityProcessorTest`, `DisplacementGaugeProcessorTest`) | import 를 `io.kotest.extensions.spring.SpringExtension` 로 유지 — **패키지는 그대로고 groupId 만 바뀐다.** 컴파일 후 실제 실행으로 확인 |
| `BehaviorSpec` + `shouldBe` / `shouldThrow` 사용 | 나머지 | Kotest 6 에서 시그니처 유지. 컴파일로 확인 |
| `ProjectConfig` (`AbstractProjectConfig`, `IsolationMode.InstancePerLeaf`) | 1 | **Kotest 6 에서 `AbstractProjectConfig` 확장 방식이 유지되는지 실행으로 확인한다.** 이 파일이 이번 업그레이드에서 가장 깨질 확률이 높다 |

**착수 조건: `./gradlew test` 가 업그레이드 전에 전부 통과하는 상태여야 한다.** 통과 상태를 기록해두고,
업그레이드 후 같은 결과가 나오는지 비교한다. 전이 깨져 있으면 후를 판정할 수 없다.

### 3.5 `influxdb-client-kotlin` → `influxdb-client-java`

```
- implementation("com.influxdb:influxdb-client-kotlin:7.3.0")
+ implementation("com.influxdb:influxdb-client-java:7.3.0")
  implementation("com.influxdb:flux-dsl:7.3.0")   // 유지
```

**코드 변경 없음.** 확인 결과 aiot 는 코루틴 API(`QueryKotlinApi`, `Flow<*>`)를 전혀 쓰지 않는다.

```bash
grep -rn "QueryKotlinApi\|influxdb.*Flow" --include="*.kt" src/main/kotlin   # 0건
```

`InfluxdbConfig` 가 쓰는 `InfluxDBClient` / `QueryApi` / `WriteApi` / `WriteApiBlocking` 은 전부
`influxdb-client-java` 의 타입이고, `-kotlin` 아티팩트는 이것을 **의존성으로 끌고 오던 것뿐**이다.
`queryApi.query(...)` 는 원래 블로킹이므로 가상 스레드와 궁합도 좋다.

> `flux-dsl` 은 별도 아티팩트라 그대로 둔다. `com.influxdb.query.dsl.Flux` 는 Reactor 의 `Flux` 가 아니라
> Flux 쿼리 언어 빌더다 — 이름이 겹칠 뿐 무관하다. **§6 작업 중 import 정리할 때 혼동 주의.**

### 3.6 버전 카탈로그 — 이번엔 도입하지 않는다

safers 는 멀티모듈(`apps/safers`, `apps/safers-collect`)이라 `libs.versions.toml` 이 필수다.
aiot 는 단일 모듈이고 `build.gradle.kts` 하나에 전부 들어 있어 카탈로그의 주 이득(모듈 간 버전 공유)이 없다.

**단, safers 카탈로그의 주석들(Boot 4.1 슬라이스 분리, Kotest 6 groupId)은 지식이므로 옮긴다** — §6.1.

### 3.7 H2 는 유지한다 — **실제 사용 중이다**

safers 는 Testcontainers + PostgreSQL 로 갔지만(`2026-08-07-postgres-testcontainers-design.md`)
aiot 는 H2 를 쓰고 있고, **제거 대상이 아니다.**

```bash
grep -rn "@SpringBootTest" --include="*.kt" src/test          # 3건
cat src/test/resources/application.yml | grep -A3 datasource
#   url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;...
#   driver-class-name: org.h2.Driver
```

| 테스트 | 성격 |
|---|---|
| `FireAlarmProcessorTest` | `@SpringBootTest @ActiveProfiles("test") @Transactional`. `SiteRepository`·`FeatureRepository`·`EventHistoryRepository` 실제 주입 |
| `TemperatureHumidityProcessorTest` | 동일 |
| `DisplacementGaugeProcessorTest` | 동일 |

`ddl-auto: create-drop` 으로 H2 에 스키마를 만들어 돌린다. **`com.h2database:h2` 를 지우면 이 3개가
컨텍스트 로딩 단계에서 깨진다.**

**남는 문제 — 이번 범위에서 판단하지 않는다.** aiot 는 `hibernate-spatial` + JTS 를 쓰고
`siteRepository.findFirstByPointInPolygon` 같은 **PostGIS 의존 쿼리**가 있는데, H2 `MODE=PostgreSQL` 은
PostGIS 함수를 제공하지 않는다. 즉 **그 경로는 지금 테스트되지 않고 있다.** §11 후속 과제로 넘긴다.

> **인증 설계문서와 겹치는 지점:** `src/test/resources/application.yml` 에도 `jwt.*` 설정이
> 운영과 같은 값으로 들어 있다. `2026-08-25-auth-alignment-design.md` §4.5 에서 `Duration` 으로
> 바꿀 때 이 파일도 함께 고쳐야 한다.

## 4. Executor / Scheduler 판정

### 4.1 판정 기준

스레드 풀은 두 가지를 한 도구로 묶은 것이다.

- **(A) 스레드 공급** — 블로킹 작업에 쓸 스레드를 어디서 얻나
- **(B) 동시성 제어** — 동시에 몇 개까지 허용하나

가상 스레드는 **(A) 만** 해결한다. (B) 는 그대로 남으므로, 제한이 필요하면 스레드 풀이 아니라
하류(커넥션 풀, `setConcurrencyLimit`, rate limiter)에 건다. 판정 질문은
"제한이 필요한가"가 아니라 **"이 풀이 (A)인가 (B)인가"** 다.

(B) 인데 가상 스레드가 필요하면 분리할 수 있다 (Spring Framework 7.0.5 확인):

```kotlin
SimpleAsyncTaskExecutor("prefix-").apply {
    setVirtualThreads(true)   // 스레드는 가상
    setConcurrencyLimit(16)   // 상한은 유지
}
```

단 `SimpleAsyncTaskExecutor` 에는 **큐가 없다.** `setConcurrencyLimit` 을 건 경우 상한 초과 시
넘치는 작업을 쌓는 대신 **제출 스레드를 블로킹시킨다.** 상한을 걸지 않으면(기본
`UNBOUNDED_CONCURRENCY`) 제출자는 절대 막히지 않는다.

### 4.2 전수 판정

| Executor / Scheduler | 현재 | 유형 | 판정 |
|---|---|---|---|
| `taskExecutor` (`AsyncConfig`, `@Primary`) | `ThreadPoolTaskExecutor` 5/10/500 | (A) — **현재 사용처 0** | **가상 스레드 전환, 빈 이름·`@Primary` 유지** (§2.2) |
| `heartBeatScheduler` (`AsyncConfig`) | `ThreadPoolTaskScheduler()` poolSize 1 | (B) — STOMP 하트비트 전용 | **유지** — §4.3 |
| `messageBrokerTaskScheduler` | 프레임워크, `availableProcessors` | — | **손댈 수 없음** |
| (신규) `taskScheduler` | 없음 → 익명 폴백 | (A) | **`SimpleAsyncTaskScheduler` 신규 추가** (§6.4) |
| `EdsKeepAliveScheduler:19` | `Executors.newSingleThreadScheduledExecutor()` | (B) — keepAlive 직렬 보장 | **유지** — §4.3 |
| `SensorDataMigrationService:164` | `Executors.newScheduledThreadPool(4)` | (B) — 디바이스별 타이머 | **유지** — §4.3 |
| `LlmMessageService:47` `Semaphore(concurrencyLimit)` | `kotlinx.coroutines.sync.Semaphore(5)` | (B) — LLM 동시 호출 상한 | **`java.util.concurrent.Semaphore` 로 교체, 상한 유지** (§6.7) |

### 4.3 유지 대상의 근거

**`heartBeatScheduler`** — poolSize 1 이지만 (A) 가 아니다. STOMP 하트비트는 5초 간격
(`setHeartbeatValue(longArrayOf(5000, 5000))`)의 짧고 정확해야 하는 작업이고, 다른 작업과 섞이면
밀린다. §6.4 로 `@Scheduled` 가 새 `taskScheduler` 로 옮겨가면 이 빈은 **하트비트 전용이 되어
오히려 목적이 선명해진다.** 그대로 둔다.

**`EdsKeepAliveScheduler` 의 단일 스레드 스케줄러** — 빈이 아니라 private 필드다.
`@ConditionalOnMissingBean(ScheduledExecutorService.class)` 판정에 영향을 주지 않는다(§2.3 무관).
단일 스레드인 것이 의도다 — keepAlive 가 겹쳐 실행되면 `login()` 재진입으로 `apiKey` 가 경합한다.
**가상 스레드로 바꾸면 이 직렬성이 깨진다. 손대지 않는다.**

**`SensorDataMigrationService` 의 `newScheduledThreadPool(4)`** — `deviceTimers: ConcurrentHashMap<String, ScheduledFuture<*>>`
로 디바이스별 타이머를 취소·재등록하는 구조다. `ScheduledFuture` 취소 의미가 필요하므로
`SimpleAsyncTaskScheduler` 로 대체하면 의미가 달라진다. **손대지 않는다.**

## 5. 코루틴 제거 판정 — 호출부 전수

### 5.1 현재 `async` 병렬화는 실제로 병렬이 아니다

```kotlin
// AiotService.statusSynchronize:89, FeatureScheduler:104 — 같은 형태
runBlocking {                    // ← 디스패처 미지정: 호출 스레드의 이벤트 루프
    supervisorScope {
        features.map { async {   // ← 부모 디스패처 상속 = 같은 단일 스레드
            ...
        } }.awaitAll()
    }
}
```

`runBlocking` 은 호출 스레드에서 자체 이벤트 루프를 돌리고, 디스패처를 명시하지 않은 `async` 는
그 디스패처를 상속한다. **따라서 모든 `async` 블록이 한 스레드에서 돈다.**

동시성은 오직 `WebClient` 서스펜션 지점에서만 생긴다. 그 사이의 JPA 호출
(`siteRepository.findFirstByPointInPolygon`)과 엔티티 변경은 전부 직렬이다.

**단 "동시성이 없다" 는 뜻이 아니다.** 서스펜션 지점이 곧 HTTP 호출이므로
**아웃바운드 요청은 이미 동시에 나가고 있다.** 직렬인 것은 CPU 작업과 블로킹 JDBC 뿐이다.
전환 시 동시성을 "새로 만드는" 것이 아니라는 점이 상한 설정에 영향을 준다 — §6.8.

**부수 효과 하나는 다행이다** — `@Transactional` 이 붙은 `statusSynchronize` / `scheduledBatteryDataUpdate`
에서 엔티티 변경이 호출 스레드(= 트랜잭션 스레드)에서 일어나므로 dirty checking 이 동작한다.
`Dispatchers.IO` 를 붙였다면 조용히 깨졌을 코드다.

**가상 스레드 전환 시 이 함정을 되풀이하면 안 된다** — §6.8 에서 트랜잭션 경계를 명시적으로 다룬다.

### 5.2 전수 판정

| 파일 | 현재 | 전환 후 | 난이도 |
|---|---|---|---|
| `EdsClient` (7개 메서드) | `exchangeToMono{}.block()` | `RestClient.retrieve().body()` | 낮음 — §6.6 |
| `NgrokConfig:53,90` | `.block()` | 동일 | 낮음 |
| `LlmMessageController:29` | `runBlocking{}` | 직접 호출 | 낮음 |
| `LlmMessageScheduler:22` | `runBlocking{}` | 직접 호출 | 낮음 |
| `LlmMessageService` | `suspend` + `coroutineScope`+`async`+`Semaphore`+`withContext(IO)` | 가상 스레드 executor + `j.u.c.Semaphore` | **중** — §6.7 |
| `AiotService.checkSynchronization:75` | `runBlocking{}` (병렬 없음) | HTTP 1회를 트랜잭션 밖으로 분리 | **중** — §6.8 |
| `AiotService.statusSynchronize:89` | `runBlocking{supervisorScope{async}}` | **읽기/HTTP/쓰기 3단계 분리 + 빈 2개 신설** | **최고** — §6.8 |
| `AiotService` suspend 5개 | `awaitBody` / `.block()` 혼재 | RestClient 동기 | 낮음 |
| `FeatureScheduler:104` | `runBlocking{supervisorScope{async}}` | §6.8 과 동일한 3단계 구조 | **높음** |
| `SensorDataMigrationService:148` | `runBlocking{}` | 직접 호출 | 낮음 |
| `EdsWebSocketClient` | Reactor 전량 | **유지** | — §7 |
| `AiotServiceKoTest` | `runBlocking` 테스트 | 일반 테스트 | 낮음 |

## 6. 변경 내역

### 6.1 `build.gradle.kts`

```kotlin
plugins {
    val kotlinVersion = "2.4.10"          // 2.3.10 → 2.4.10
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "4.1.0"   // 4.0.3 → 4.1.0
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version kotlinVersion
    id("com.diffplug.spotless") version "8.1.0"
}
```

의존성 변경분만:

```kotlin
    // kotlin-jdsl 3.8.0 → 3.9.0
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:3.9.0")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:3.9.0")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-boot4-support:3.9.0")

    // influxdb: -kotlin → -java (코드 변경 없음, 코루틴 API 미사용 — 설계문서 §3.5)
    implementation("com.influxdb:influxdb-client-java:7.3.0")
    implementation("com.influxdb:flux-dsl:7.3.0")

    // springwolf 2.0.0 → 2.4.0
    implementation("io.github.springwolf:springwolf-core:2.4.0")
    implementation("io.github.springwolf:springwolf-stomp:2.4.0")
    runtimeOnly("io.github.springwolf:springwolf-ui:2.4.0")

    // 사용처 없음(설계문서 §1.1). EdsWebSocketClient 를 StandardWebSocketClient 로
    // 전환할 때 webflux 와 함께 제거한다.
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Kotest 5.9.1 → 6.2.4
    // Kotest 6 에서 Spring 확장이 코어 저장소로 흡수되며 groupId 가 바뀌었다.
    // 구 좌표(io.kotest.extensions)는 1.3.0 에서 멈춰 있고 Kotest 5.8.1 이 대상이다.
    testImplementation("io.kotest:kotest-extensions-spring:6.2.4")
    testImplementation("io.kotest:kotest-runner-junit5:6.2.4")
    testImplementation("io.kotest:kotest-assertions-core:6.2.4")

    // Boot 4.1 은 테스트 슬라이스를 기술별 스타터로 쪼갰다 — @DataJpaTest 는 starter-test 에 없다.
    // 현재 슬라이스 테스트 사용처가 없어 추가하지 않는다. 컨트롤러/JPA 슬라이스 테스트를
    // 도입할 때 아래를 함께 추가할 것.
    //   testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    //   testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
```

Gradle wrapper:

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.7.0-bin.zip
```

### 6.2 `application-common.yml` — 가상 스레드 활성화

```yaml
spring:
  # 대부분의 작업이 블로킹 I/O(Mobius/LLM/EDS HTTP, JDBC, Influx, S3, Redis)다.
  # @Async/@Scheduled 는 이 플래그만으로 전환되지 않아 AsyncConfig 에서 명시한다
  # (docs/specs/2026-08-25-virtual-threads-design.md §2.2 §2.3).
  threads:
    virtual:
      enabled: true
```

### 6.3 HikariCP — **설정을 바꾸지 않는다**

당초 `maximum-pool-size` 와 `connection-timeout` 을 명시하려 했으나, 근거를 검증한 결과
**둘 다 넣을 이유가 없다.** 기록을 남긴다.

#### 검증: `connection-timeout` 기본값은 무한이 아니다

```bash
javap -c -p -cp <HikariCP-7.0.2 전개경로> com.zaxxer.hikari.HikariConfig | grep -A2 CONNECTION_TIMEOUT
#   ldc2_w  long 30l
#   putstatic  Field CONNECTION_TIMEOUT:J        ← 30초
```

"명시하지 않으면 무한 대기해서 가상 스레드가 쌓인다"는 전제가 **틀렸다.** 30초 후
`SQLTransientConnectionException` 으로 터지고 로그에 남는다. 3초로 줄이면 **정상적인 부하 스파이크에서
실패하기만 더 쉬워진다** — 진단성이 좋아지는 게 아니라 에러율이 올라간다.

#### `maximum-pool-size` 도 근거가 없다

20 이라는 값은 "기본값 10 의 2배" 외에 아무 근거가 없었다. 측정 없이 바꾸면
**문서화된 기본값을 근거 없는 상수로 바꾸는 것**에 불과하고, 다음 사람이 "왜 20인가"를 되물을 때
답할 수 있는 사람이 없다. 가상 스레드는 DB 처리량을 늘려주지 않으므로 풀을 키운다고 빨라지지도 않는다.

**결론: `application-common.yml` 에 hikari 블록을 추가하지 않는다.**

#### 다만 알고는 있어야 하는 변화 — 유입 제한 지점이 옮겨간다

설정은 그대로 두되, **동작이 바뀌는 지점은 인지한다.**

| | 변경 전 | 변경 후 |
|---|---|---|
| 동시 처리 요청 상한 | 톰캣 `threads.max` = **200** | 없음 (가상 스레드) |
| 커넥션 대기자 수 | 최대 200 | **무제한** |
| 초과 요청의 운명 | 톰캣 accept 큐에서 대기 → 연결 거부 | 즉시 수락 → 풀에서 최대 30초 대기 → 500 |

즉 **백프레셔 지점이 톰캣 커넥터에서 커넥션 풀로 내려온다.** 과부하 시 증상이
"연결 거부"에서 "30초 후 500" 으로 바뀐다. 풀 크기(10)는 그대로이므로 **DB 부하 자체는 늘지 않는다.**

**관측만 붙인다.** 값을 바꾸는 것은 데이터를 본 뒤다.

- `hikaricp_connections_pending` — actuator + micrometer 도입 시 (§11-8)
- 미도입 상태에서는 `SQLTransientConnectionException` 발생 여부를 로그에서 본다.
  **이 예외가 나오기 시작하면** 그때 풀 크기를 검토한다 — 그 시점엔 근거가 생긴다.

### 6.4 `global/config/WebSocketConfig.kt` — `AsyncConfig` 교체

`AsyncConfig` 는 별도 파일이 아니라 `WebSocketConfig.kt` 의 두 번째 클래스다(39~56행).

```kotlin
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    // @Primary 는 파라미터 이름 매칭보다 우선한다. 나중에 taskScheduler 가 @Primary 가 되면
    // 하트비트가 조용히 갈아타므로 주입 지점을 고정한다 (설계문서 §6.4 주의점 ③).
    @param:Qualifier("heartBeatScheduler") private val heartBeatScheduler: TaskScheduler,
    private val myDefaultHandshakeHandler: DefaultHandshakeHandler,
) : WebSocketMessageBrokerConfigurer {
    // registerStompEndpoints / configureMessageBroker 는 변경 없음
}

@EnableAsync
@Configuration
class AsyncConfig {
    // 현재 @Async 사용처는 0개다. 그래도 지우지 않는다 — 지우면 자동설정이 살아나는 게 아니라
    // (Executor 타입 빈이 heartBeatScheduler/messageBrokerTaskScheduler 로 2개 더 있어 계속 백오프),
    // 나중에 붙는 @Async 가 AsyncExecutionInterceptor 의 new SimpleAsyncTaskExecutor()
    // (플랫폼 스레드, 무제한, 스프링 관리 밖) 폴백으로 떨어진다 (설계문서 §2.2).
    @Bean(name = ["taskExecutor"])
    @Primary
    fun taskExecutor(): AsyncTaskExecutor =
        SimpleAsyncTaskExecutor("app-async-").apply {
            setVirtualThreads(true)
            setTaskTerminationTimeout(10_000) // 종료 시 in-flight 대기(상한 있음)
        }

    // 이름이 정확히 "taskScheduler" 여야 TaskSchedulerRouter 가 익명 단일 스레드 폴백 대신
    // 이 빈을 고른다. @Primary 를 붙이면 안 된다 — WebSocketConfig 의 하트비트가 갈아탄다.
    // (설계문서 §2.3)
    @Bean
    fun taskScheduler(): TaskScheduler =
        SimpleAsyncTaskScheduler().apply {
            setVirtualThreads(true)
            threadNamePrefix = "app-sched-"
            // hand-off 된 작업은 종료 시 자동으로 기다려주지 않는다 (아래 주의점 ②).
            setTaskTerminationTimeout(10_000)
        }

    // STOMP 브로커 하트비트 전용(5초 간격). 다른 작업과 섞이면 밀리므로 분리 유지 (설계문서 §4.3).
    @Bean
    fun heartBeatScheduler(): TaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 1
            setThreadNamePrefix("stomp-heartbeat-")
            initialize()
        }
}
```

#### `SimpleAsyncTaskScheduler` 의 실제 동작 — 작업 종류에 따라 갈린다

"가상 스레드니까 전부 알아서 할당된다"가 아니다. 내부에 실행기가 두 개 있고
(`new ScheduledThreadPoolExecutor(1, this::newThread)` — 스레드는 1개, 단 `newThread` 가 가상 스레드),
`@Scheduled` 종류에 따라 경로가 다르다.

| 작업 | 경로 | 본문 실행 위치 | aiot 대상 |
|---|---|---|---|
| cron / `fixedRate` | `triggerExecutor` → `scheduledTask()` = `() -> execute(...)` | **매번 새 가상 스레드**로 hand-off | **`@Scheduled` 3개 전부** |
| `fixedDelay` | `fixedDelayExecutor` → `taskOnSchedulerThread()` | **그 스케줄러 스레드에서 직접** | 없음 |

**aiot 의 `@Scheduled` 3개는 전부 cron 이므로 전량 hand-off 경로다.** §2.3 의 "08:00 두 개가 직렬"
문제가 정확히 해소된다 — 각자 새 가상 스레드에서 동시에 시작한다.

주의점 셋:

1. **`fixedDelay` 작업을 나중에 추가하면 그것들끼리는 직렬이다.** `fixedDelayExecutor` 는 스레드 1개다.
   `setTargetTaskExecutor` 는 해법이 아니다 — `doExecute()` 에만 걸리는데 fixedDelay 는
   `taskOnSchedulerThread()` 로 스케줄러 스레드에서 직접 실행되어 이 분기를 타지 않는다.
   fixedDelay 가 3개 이상 필요해지면 `ThreadPoolTaskScheduler(poolSize = N)` 를 쓴다.
2. **종료 시 hand-off 된 작업을 자동으로 기다리지 않는다.** javadoc: "stopping trigger firing and
   fixed-delay task execution but **not stopping the execution of handed-off tasks**".
   08:00 배치 중 배포되면 끊긴다. `setTaskTerminationTimeout(10_000)` 으로 대기 시간을 준다.
   (현재 익명 폴백은 `TaskSchedulerRouter.destroy()` 가 `shutdownNow()` 를 호출해 아예 기다리지 않으므로,
   이것만으로도 개선이다.)
3. **`taskScheduler` 에 `@Primary` 를 붙이면 STOMP 하트비트가 조용히 갈아탄다.**
   `WebSocketConfig` 가 `TaskScheduler` 를 **타입 주입 + 파라미터 이름 매칭**으로 받고 있는데,
   `@Primary` 는 이름 매칭보다 우선한다. 위 `@Qualifier` 로 고정한다.

#### 08:00 동시 실행이 만드는 새 상황

§2.3 의 두 cron 이 이제 **동시에** 돈다. 각각 외부 HTTP fan-out 이므로 합산 부하가 겹친다.

| | 변경 전 | 변경 후 |
|---|---|---|
| `scheduledBatteryDataUpdate` (Mobius) | 다른 cron 이 끝난 뒤 시작. HTTP 자체는 이미 동시(§5.1) | **즉시 시작**, 동시성은 세마포어로 현행 유지(§6.8) |
| `generateDailyMessage` (LLM) | 앞 작업 완료 후 시작 | **동시 시작**, 상한 `Semaphore(5)` 유지(§6.7) |

**LLM 쪽은 `llm.api.concurrency-limit`(기본 5) 상한이 그대로 유지된다.** Mobius 쪽도 동시 호출량은
지금과 같다 — §6.8 에서 Reactor Netty 가 걸고 있던 상한을 계산식으로 이어받는다.
두 하류 시스템이 다르므로 서로 간섭하지 않는다. **의도된 변경이며, 이것이 이번 작업의 목표 중 하나다.**

### 6.5 `WebClientFactory` → `RestClientFactory` (신규)

`global/config/WebClientFactory.kt` 를 `RestClientFactory.kt` 로 대체한다.
`WebClientConfig.kt` 의 `webClientBuilder` 빈은 제거한다 (`RestClientFactory` 가 대신한다).

#### 검증: Boot 의 `ClientHttpRequestFactoryBuilder` 는 쓸 수 없다

safers 계열 코드에서 흔히 쓰는 `ClientHttpRequestFactoryBuilder.jdk()` 는
`org.springframework.boot.http.client` 패키지이고, 이는 **`spring-boot-restclient` 모듈에 들어 있다.**
aiot 의 런타임 클래스패스에는 없다.

```bash
./gradlew -q dependencies --configuration runtimeClasspath | grep -i restclient   # 0건
```

**`spring-web` 에 있는 `JdkClientHttpRequestFactory` 를 직접 쓴다.** 확인:

```bash
javap -cp <spring-web-7.0.5 전개경로> org.springframework.http.client.JdkClientHttpRequestFactory
#   public JdkClientHttpRequestFactory(java.net.http.HttpClient)
#   public void setReadTimeout(java.time.Duration)
javap -cp <같은 경로> 'org.springframework.web.client.RestClient$Builder' | grep requestFactory
#   public abstract RestClient$Builder requestFactory(ClientHttpRequestFactory)
```

> 의존성을 추가해서 Boot 빌더를 쓰는 대안도 있으나 채택하지 않는다 —
> 모듈 하나를 더 끌어오는 값이 `HttpClient.newBuilder()` 두 줄보다 크지 않다.

```kotlin
@Component
class RestClientFactory : DisposableBean {
    // 생성한 HttpClient 를 들고 있다가 종료 시 닫는다 — 아래 라이프사이클 항목 참조.
    private val clients = java.util.concurrent.CopyOnWriteArrayList<HttpClient>()

    // 모든 HttpClient 가 공유한다. 가상 스레드라 개수를 늘릴 이유가 없다.
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    fun createClient(
        baseUrl: String,
        connectionTimeoutMs: Long = 5000,
        readTimeoutMs: Long = 30000,
        // false 면 4xx/5xx 에 예외를 던지지 않고 본문을 그대로 디코딩한다.
        // EDS 처럼 "HTTP 상태가 아니라 응답 본문의 code 로 판정" 하는 계약을 지킬 때 쓴다(§6.6).
        throwOnHttpError: Boolean = true,
    ): RestClient

    override fun destroy() {
        clients.forEach { it.close() }   // HttpClient 는 Java 21+ AutoCloseable
        executor.close()
    }
}
```

**구성 결정 다섯.**

- **`defaultHeaders { it.accept = listOf(MediaType.APPLICATION_JSON) }` 를 유지한다.**
  현재 `WebClientConfig.webClientBuilder` 가 전역으로 걸고 있는 값이다. 그 빈을 제거하면서
  대체하지 않으면 **EDS·LLM 요청에서 `Accept` 헤더가 사라진다** — 콘텐츠 협상을 하는
  엔드포인트가 다른 표현을 돌려주거나 요청을 거부해 본문 디코딩이 깨질 수 있다.
- **`JdkClientHttpRequestFactory(HttpClient)`** 를 쓴다. connect 타임아웃은 `HttpClient.newBuilder()`
  쪽에, read 타임아웃은 팩토리 쪽(`setReadTimeout(Duration)`)에 건다.
- **executor 를 명시한다.** 지정하지 않으면 JDK HttpClient 가 자체 플랫폼 스레드 풀을 만든다.
  **다만 `createClient` 마다 새로 만들지 않고 팩토리가 하나를 공유한다** — 아래 라이프사이클 항목.
- **생성한 `HttpClient` 와 executor 를 스프링이 닫을 수 있어야 한다**(`DisposableBean`).
- `throwOnHttpError = false` 일 때 `defaultStatusHandler({ true }, { _, _ -> })` 를 건다 —
  모든 상태코드를 "에러 아님" 으로 처리해 `retrieve()` 가 본문을 디코딩하게 둔다.

> **`WebClientFactory` / `WebClientConfig` 는 이 시점에 지우지 않는다.** `LlmMessageService`·
> `AiotService`·`AiotServiceKoTest` 가 아직 쓰고 있어 컴파일이 깨진다. 두 팩토리를 공존시키고
> **마지막 소비자가 옮겨간 뒤 제거한다**(§10 커밋 10).

#### 라이프사이클 — 팩토리가 자원을 들고 있어야 한다

`createClient` 는 **`EdsClient`·`NgrokConfig`·`AiotService`·`LlmMessageService` 네 곳에서** 호출된다.
`RestClient` 만 반환하면 그 뒤에 매달린 `HttpClient` 와 executor 를 **스프링이 닫을 방법이 없다.**

```bash
jshell> java.net.http.HttpClient.class.getInterfaces()
$1 ==> [interface java.lang.AutoCloseable]     # Java 21+ 부터 닫을 수 있다 (Java 25 확인)
```

닫지 않으면 `HttpClient` 의 셀렉터 스레드와 커넥션 풀이 컨텍스트 종료 후에도 남는다.
JVM 이 곧 죽는 운영 환경에서는 티가 안 나지만, **`@SpringBootTest` 3개가 컨텍스트를 띄우므로
테스트에서 누적된다.**

> **executor 를 클라이언트마다 만들지 않는 이유.** 가상 스레드 executor 는 플랫폼 스레드를
> 붙잡지 않으므로 여러 개 둘 이득이 없고, 닫아야 할 자원만 늘어난다. 하나를 공유한다.

확인:

```bash
javap -cp <spring-web-7.0.5 전개경로> 'org.springframework.web.client.RestClient$Builder' | grep statusHandler
#   defaultStatusHandler(java.util.function.Predicate<HttpStatusCode>, RestClient$ResponseSpec$ErrorHandler)
```

#### `throwOnHttpError` 는 클라이언트 단위지만 계약은 메서드 단위다

**EDS 는 7개 메서드가 모두 같은 계약이라 클라이언트 옵션 하나로 해결되지만, `AiotService` 는 다르다.**
메서드마다 non-2xx 처리가 갈린다.

| 메서드 | 현재 non-2xx 동작 | 전환 방식 |
|---|---|---|
| `fetchSubscription` | 예외를 던져 호출자가 409 를 잡는다 (`setupSubscriptionForFeature` 의 재시도 경로) | `retrieve()` — 클라이언트 기본값(`true`) 그대로 |
| `fetchDeviceBatteryData` | **`null` 반환.** 코드 주석: "4xx, 5xx 상태코드는 null 반환 (예외 발생 안함)" | **`.exchange { }`** 로 개별 처리 |
| `fetchRemoveSubscription` | **본문을 읽어 로깅하고 계속 진행.** 성공 여부로 후처리만 가른다 | **`.exchange { }`** 로 개별 처리 |

**뒤의 둘을 `retrieve()` 로 바꾸면 예상된 4xx 하나가 루프 전체를 중단시킨다** —
배터리 동기화 fan-out 이나 `removeAllSubscriptions` 순회가 첫 실패에서 끊긴다.

> **클라이언트 옵션은 `true` 로 두고, 계약이 다른 두 메서드만 `.exchange { }` 를 쓴다.**
> `throwOnHttpError = false` 로 클라이언트 전체를 뒤집으면 `fetchSubscription` 의 409 재시도가 죽는다.
> §6.6 에서 EDS 에 대해 확인한 것과 **같은 검사를 메서드 단위로 한 번 더** 해야 한다.

> `AiotService:296` 의 `WebClientResponseException` catch 는
> **`RestClientResponseException` 으로 교체한다**(§6.9). 놓치면 컴파일은 통과하고 런타임에 안 걸린다.

**기존 `WebClientFactory` 와의 차이 — 인지 사항**

| | `WebClientFactory` (현재) | `RestClientFactory` |
|---|---|---|
| 타임아웃 | connect / response / read 3종 (Reactor Netty) | **connect / read 2종** — `responseTimeout` 에 대응하는 개념이 없다 |
| `maxInMemorySize(1MB)` | codec 설정 | RestClient 는 `ByteArray`/`String` 응답을 힙에 그대로 올린다 — **상한이 없어진다** |
| `WriteTimeoutHandler` | 있음 | 없음 — JDK HttpClient 는 write 타임아웃 개념이 없다 |

두 번째가 실질 차이다. `EdsClient.getEventThumbnail()` 이 `ByteArray` 를 받으므로
**EDS 가 거대한 응답을 주면 힙을 그대로 먹는다.** §6.6 에서 이 경로에 **스트림 단계의** 상한을 넣는다 —
`Content-Length` 헤더를 보는 사후 검사로는 대체되지 않는다.

첫 번째와 세 번째는 현 사용 패턴(짧은 JSON 요청/응답)에서 실질 영향이 없다고 판단한다.
**단 `readTimeout` 이 유일한 방어선이 되므로 30초 기본값을 그대로 유지한다.**

### 6.6 `EdsClient` — RestClient 전환

**대상은 7개다.** `client` 호출 지점도 7곳이다.

| 메서드 | 형태 |
|---|---|
| `login` | JSON — `retrieve().body(T)` |
| **`keepAlive`** | JSON — 아래 주의 |
| `getCameraList` | JSON |
| `getRealtimeStreamUrl` | JSON |
| `getRecordStreamUrl` | JSON |
| `getWebSocketUrl` | JSON |
| `getEventThumbnail` | `ByteArray` — 형태가 다르다(뒤에서 별도로 다룬다) |

> **`keepAlive` 를 빠뜨리지 말 것.** 앞의 6개와 형태는 같지만 **역할이 다르다** —
> 이 메서드의 catch 블록이 `login()` 을 호출해 `apiKey` 를 갱신한다.
> §6.10 의 stale api-key 결함이 트리거되는 지점이 바로 여기이므로,
> 전환 후 **재로그인 경로가 살아 있는지 반드시 확인한다**(§8.3).

**전환 전에 EDS 의 에러 계약을 확인해야 한다 — 이것을 놓치면 동작이 바뀐다.**

현재 `exchangeToMono` 는 **4xx/5xx 에도 예외를 던지지 않고** 본문을 그대로 디코딩한다.
그래서 아래 검사가 도메인 에러로 매핑할 기회를 얻는다.

```kotlin
if (response.code != 200 || response.result == null) {
    throw CustomException(ErrorCode.EDS_LOGIN_FAILED, response.message)
}
```

`response.code` 는 **HTTP 상태가 아니라 EDS 응답 본문의 필드**다. 즉 EDS 는
HTTP 200 + `code != 200` 도, HTTP 4xx 도 낼 수 있고 **양쪽 모두 위 검사로 수렴하고 있었다.**

반면 `RestClient.retrieve()` 는 **기본 상태 핸들러가 4xx/5xx 에서 `RestClientResponseException` 을 던진다.**
그대로 옮기면 위 검사에 도달하지 못하고 일반 예외 핸들러를 타 **`EDS_LOGIN_FAILED`/`EDS_API_ERROR`
매핑이 사라지고 500 이 나간다.**

→ **EDS 클라이언트는 `throwOnHttpError = false` 로 만든다**(§6.5). 그러면 `retrieve().body(T)` 가
`exchangeToMono` 와 같은 의미가 되고, 기존 `response.code` 검사가 그대로 동작한다.

```kotlin
// 비-2xx 를 예외 없이 null 로 넘기는 계약이라 .exchange 를 쓴다.
.exchange { _, response ->
    if (!response.statusCode.is2xxSuccessful) return@exchange null
    response.body.use { input ->
        val bytes = input.readNBytes(MAX_THUMBNAIL_BYTES + 1)   // 상한+1 만 읽는다
        if (bytes.size > MAX_THUMBNAIL_BYTES) null else bytes
    }
}
```

**제약 셋.**

- `MAX_THUMBNAIL_BYTES` 는 `Int` 여야 한다 (`readNBytes(int)`). `1024L * 1024` 로 두면 컴파일되지 않는다.
- **`Content-Length` 로 판단하지 않는다.** chunked 응답이면 `-1` 이라 검사를 통과하고, 서버가
  과소 신고해도 통과한다. 그 뒤 전량이 힙에 올라간다.
- **`use` 로 닫는다.** `exchange` 콜백이 응답을 자동으로 닫아주지 않는 경로가 있다.

초과 시 경고 로그를 남기고 `null` 을 반환한다(현행 계약 유지).

`getCameraList` / `getRealtimeStreamUrl` / `getRecordStreamUrl` / `getWebSocketUrl` 도 동일 패턴이다
(`.bodyValue()` → `.body()`, `.exchangeToMono{ resp -> resp.bodyToMono(T) }.block()` → `.retrieve().body(T)`).

**썸네일만 형태가 다르다** — 비-2xx 를 예외 없이 null 로 넘기는 계약이므로 `.exchange` 를 쓴다.
§6.5 에서 사라진 크기 상한을 여기서 복원한다.

**상한은 스트림에서 걸어야 한다.** `Content-Length` 헤더를 보고 판단한 뒤 `bodyTo(ByteArray)` 를
호출하는 방식은 **상한을 복원하지 못한다** — chunked 응답이면 `contentLength` 가 `-1` 이라 검사를
통과하고, 서버가 실제보다 작게 신고해도 통과한다. 그 뒤 `bodyTo` 가 전량을 힙에 올린 다음에야
크기를 재는 꼴이라, WebClient 의 `maxInMemorySize` 가 하던 "디코딩 중 중단" 과 의미가 다르다.

`ClientHttpResponse` 는 `HttpInputMessage` 를 상속하므로 `response.body` 로 `InputStream` 을 얻을 수 있다.
**상한 + 1 바이트만 읽어서 초과 여부를 판정한다** — 할당량이 상한에 묶인다.

```kotlin
// JSON 6개(login·keepAlive·getCameraList·getRealtime/RecordStreamUrl·getWebSocketUrl)가
// 모두 같은 형태로 바뀐다. login() 대표.
-   .bodyValue(request)
-   .exchangeToMono { resp -> resp.bodyToMono(object : ParameterizedTypeReference<...>() {}) }
-   .block()
+   .body(request)
+   .retrieve()
+   .body(object : ParameterizedTypeReference<EdsResponse<EdsLoginResult>>() {})
```

`?: throw CustomException(...)` 과 뒤따르는 `if (response.code != 200) throw ...` 검사는 **그대로 둔다.**
`throwOnHttpError = false` 덕분에 의미가 보존된다.

**`apiKey` 에 `@Volatile` 을 붙인다.** `EdsKeepAliveScheduler` 스레드가 쓰고 요청 스레드가 읽는다.
현재 가시성 보장이 없다 — 기존 결함이며 §6.10 과 같은 부류다.

확인:

```bash
javap -cp <spring-web-7.0.5 전개경로> org.springframework.http.client.ClientHttpResponse
#   public interface ClientHttpResponse extends org.springframework.http.HttpInputMessage, java.io.Closeable
javap -cp <같은 경로> org.springframework.http.HttpInputMessage
#   public abstract java.io.InputStream getBody() throws java.io.IOException
```

> **`use` 로 닫는다.** `exchange` 의 콜백은 응답을 자동으로 닫아주지 않는 경로가 있으므로
> 스트림을 직접 다룰 때는 명시적으로 닫는다.

**`apiKey` 에 `@Volatile` 을 붙인다.** `EdsKeepAliveScheduler` 스레드가 쓰고 요청 스레드가 읽는다.
현재 가시성 보장이 없다 — 기존 결함이며 §6.10 과 같은 부류다.

### 6.7 `LlmMessageService` / `LlmMessageScheduler` / `LlmMessageController`

**Controller / Scheduler** — `runBlocking { }` 을 지우고 직접 호출한다. 그뿐이다.

**Service** — `suspend` 를 전부 제거하고 fan-out 을 가상 스레드로 바꾼다.
`Semaphore(concurrencyLimit)` 은 (B) 유형이므로 **`java.util.concurrent.Semaphore` 로 교체하고 상한을 유지한다.**

```kotlin
@Service
class LlmMessageService(
    ...,
    restClientFactory: RestClientFactory,   // webClientBuilder 대체
) {
    private val client: RestClient = restClientFactory.createClient(llmProperties.baseUrl)

    // (B) 유형이므로 상한을 유지한다(§4.1). 기존 llm.api.concurrency-limit(기본 5) 그대로.
    private val semaphore = java.util.concurrent.Semaphore(llmProperties.concurrencyLimit)

    fun generateAndSaveMessage()                 // suspend 제거
}
```

**변경 제약 넷.**

- `coroutineScope { async { } }` → `Executors.newVirtualThreadPerTaskExecutor().use { }` fan-out.
  `submit` 결과를 `forEach { it.get() }` 로 받아 **예외를 확인한다** (`close()` 가 대기는 해주지만
  실패를 알려주지 않는다).
- `withPermit { }` → `semaphore.acquire()` / `finally { release() }`.
- 사이트별 실패는 삼키고 로그만 남긴다(현행과 동일).
- **`withContext(Dispatchers.IO) { llmMessageRepository.save(...) }` 래핑을 제거한다.**
  이것이 이번 전환의 핵심 이득 중 하나다 — 지금은 JPA save 가 다른 스레드에서 일어나
  트랜잭션 컨텍스트 밖이다. 이 메서드에 `@Transactional` 이 없어 우연히 동작할 뿐이며,
  상위에 트랜잭션이 붙는 순간 깨진다.

**`retrieve().body(T)` 의 null 반환에 주의한다.** 현행 `awaitBody<LlmResponse>()` 는 빈 응답에
예외를 던지지만 RestClient 는 `null` 을 반환한다. `?: throw CustomException(...)` 으로 받는다 —
흘리면 `generatedText` 접근에서 NPE 다. (같은 함정의 파괴적인 사례는 §6.8 참조.)

`generateMessageForSite` / `getHourlyAverageTemperature` 는 `suspend` 를 떼고
`withContext(Dispatchers.IO) { }` 래핑을 제거한다. 본문은 그대로 둔다 — 이미 블로킹 코드다.

```kotlin
        val llmResponse =
            client
                .post()
                .uri("/generate/temperature")
                .body(llmRequest)
                .retrieve()
                .body(LlmResponse::class.java)
                ?: throw CustomException(ErrorCode.LLM_RESPONSE_EMPTY)
```

> `ErrorCode.LLM_RESPONSE_EMPTY` 가 없으면 추가한다. 현재 `awaitBody<LlmResponse>()` 는 빈 응답에
> `WebClientResponseException` 을 던지지만, `RestClient.body()` 는 **null 을 반환한다.**
> 이 차이를 흘리면 `generatedText` 접근에서 NPE 가 난다. **전환 시 놓치기 쉬운 지점이다.**

**`withContext(Dispatchers.IO) { llmMessageRepository.save(...) }` 제거가 이번 전환의 핵심 이득 중 하나다.**
지금은 JPA save 가 다른 스레드에서 일어나 트랜잭션 컨텍스트 밖이다. 이 메서드에 `@Transactional` 이
없어서 우연히 동작할 뿐이며, 상위에 트랜잭션이 붙는 순간 깨진다. 제거하면 이 함정이 사라진다.

### 6.8 Mobius 동기화 — 트랜잭션 경계와 동시성 상한 재설계

**이번 작업에서 가장 위험한 구간이다.** 요구사항이 네 개인데 서로 얽혀 있어, 하나만 보고 고치면
다른 하나가 깨진다. 먼저 전부 나열한다.

| # | 요구사항 | 어기면 |
|---|---|---|
| R1 | 엔티티 변경은 **트랜잭션 스레드**에서 일어나야 한다 | dirty checking 이 조용히 깨진다 |
| R2 | HTTP fan-out 은 **트랜잭션 밖**이어야 한다 | 응답 대기 내내 JDBC 커넥션을 점유한다(풀 기본 10) |
| R3 | 트랜잭션 간에 **엔티티를 넘기면 안 된다** | 뒤 트랜잭션에서 detached → 역시 dirty checking 이 안 된다 |
| R4 | 동시성 상한은 **모든 Mobius 경로에 공유**돼야 한다 | 경로별 상한의 합이 기존 상한을 넘는다 |

R1 과 R2 는 서로 밀어낸다. 둘 다 만족시키려면 **읽기 / HTTP / 쓰기를 세 단계로 쪼개고
단계 사이에는 엔티티가 아니라 식별자와 값만 넘겨야 한다**(R3).

#### 현재 코드의 문제

```kotlin
@Transactional                              // ← 트랜잭션은 호출 스레드에 바인딩
fun statusSynchronize() {
    features.map { async {                  // ← 디스패처 미지정이라 같은 스레드(§5.1)
        feature.updateStatusInfo(...)       // ← 그래서 우연히 동작 중이다
    } }
}
```

현재는 `runBlocking` 단일 스레드 덕분에 R1 이 **우연히** 지켜지고 있다. 가상 스레드로 진짜
병렬화하면 깨진다. 반대로 R2 는 **지금도 위반 중**이다 — HTTP fan-out 전체가 트랜잭션 안에 있다.

#### 단계 분리

세 단계를 **서로 다른 빈**에 둔다. 같은 빈 안에서 호출하면 **프록시를 타지 않아 `@Transactional`
이 아예 적용되지 않는다**(self-invocation). 이것이 별도 빈으로 빼는 유일한 이유다.

```kotlin
// AiotService — 오케스트레이션만. @Transactional 을 붙이지 않는다.
fun statusSynchronize() {
    // ① 읽기 트랜잭션: 엔티티가 아니라 deviceId 만 꺼낸다 (R3)
    val deviceIds = featureQueryService.findAllDeviceIds()
    log.info { "총 ${deviceIds.size}개의 Feature 위치 동기화 시작" }

    // ② 트랜잭션 밖: 순수 HTTP fan-out (R2)
    val statuses = fetchAllStatuses(deviceIds)

    // ③ 쓰기 트랜잭션: 별도 빈이므로 프록시를 탄다 (R1)
    featureStatusWriter.applyStatuses(statuses)

    log.info { "위치 동기화 완료" }
}
```

```kotlin
// 신규. 읽기 전용 조회를 트랜잭션 경계로 감싼다.
@Service
class FeatureQueryService(
    private val featureRepository: FeatureRepository,
) {
    @Transactional(readOnly = true)
    fun findAllDeviceIds(): List<String> = featureRepository.findAll().map { it.deviceId }
}
```

```kotlin
// 신규. 쓰기 단계 전용. AiotService 와 별도 빈이어야 프록시가 적용된다.
@Service
class FeatureStatusWriter(
    private val featureRepository: FeatureRepository,
    private val siteRepository: SiteRepository,
) {
    @Transactional
    fun applyStatuses(statuses: Map<String, DeviceStatus>) {
        if (statuses.isEmpty()) return

        // 이 트랜잭션 안에서 다시 로드해야 영속 상태가 된다 (R3).
        // ①에서 읽은 엔티티를 넘겨받으면 detached 라 변경이 flush 되지 않는다.
        featureRepository.findAllByDeviceIdIn(statuses.keys.toList()).forEach { feature ->
            val status = statuses[feature.deviceId] ?: return@forEach
            val site = siteRepository.findFirstByPointInPolygon(status.longitude, status.latitude)
            feature.updateStatusInfo(status.longitude, status.latitude, status.batteryLevel, site)
        }
    }
}
```

> **`FeatureRepository.findAllByDeviceIdIn(deviceIds: List<String>): List<Feature>` 를 추가한다.**
> 기존에 `deleteAllByDeviceIdIn` 이 있으므로 같은 파생 쿼리 패턴이다.

> **`DeviceStatus` 는 `AiotService` 내부가 아니라 공유 위치로 옮긴다.** 세 빈이 함께 쓰므로
> `data/dto/AiotDto.kt` 에 `internal data class DeviceStatus(longitude, latitude, batteryLevel)` 로 둔다.
> (3차 리뷰에서 `private data class` 로 정의했으나, 단계 분리로 가시성이 맞지 않게 됐다.)

HTTP 단계는 트랜잭션과 무관한 순수 함수가 된다.

HTTP 단계는 트랜잭션과 무관한 순수 함수가 된다.

```kotlin
// 트랜잭션 밖에서 호출된다. 엔티티가 아니라 deviceId 만 받는다(R3).
private fun fetchAllStatuses(deviceIds: List<String>): Map<String, DeviceStatus>
```

**구현 제약 셋.**

- `Executors.newVirtualThreadPerTaskExecutor()` 를 `use { }` 로 감싸 종료를 보장한다.
- **개별 실패는 삼키고 `null` 로 처리한다** — 한 디바이스 실패가 전체 동기화를 중단시키면 안 된다.
  로그는 남긴다(현행 `log.error(e) { "위치 데이터 가져오기 실패: $deviceId" }` 유지).
- **세마포어를 여기서 획득하지 않는다.** leaf 인 `fetchDeviceLocationData` /
  `fetchDeviceBatteryData` 안에서 건다 — 이유는 아래 R4.

#### `handleMobiusUrlUpdated` 의 `@Transactional` 을 제거한다

```kotlin
@EventListener
@Transactional                          // ← 이것 때문에 아래 전부가 한 트랜잭션 안이다
fun handleMobiusUrlUpdated(event: MobiusUrlUpdatedEvent) {
    this.cachedMobiusUrl = event.newUrl
    checkSynchronization()
    statusSynchronize()                 // ← 게다가 자기 호출이라 프록시도 안 탄다
    subscription()
}
```

위 분리를 해놓아도 **이 어노테이션이 남아 있으면 전부 무의미하다** — 바깥 트랜잭션이 살아 있어
HTTP fan-out 이 그 안에서 돌기 때문이다. `@Transactional` 을 떼고 각 메서드가 자기 경계를 갖게 한다.

```kotlin
@EventListener
fun handleMobiusUrlUpdated(event: MobiusUrlUpdatedEvent) {
    this.cachedMobiusUrl = event.newUrl
    checkSynchronization()
    statusSynchronize()
    subscription()
}
```

> **원자성이 바뀐다는 점은 인지한다.** 지금은 셋이 한 트랜잭션이라 중간 실패 시 전부 롤백되지만,
> 분리 후에는 앞 단계가 커밋된 채로 뒤가 실패할 수 있다. 다만 **셋 다 외부 시스템(Mobius) 상태를
> 로컬에 반영하는 동기화 작업**이라 부분 반영이 치명적이지 않고, 다음 동기화에서 수렴한다.
> 애초에 HTTP 를 트랜잭션으로 감싸 원자성을 확보하려는 설계가 잘못이었다.

#### `checkSynchronization` 도 같은 문제다

```kotlin
@Transactional
fun checkSynchronization() {
    val existFeatures = featureRepository.findAll()
    val features = fetchAllMobiusSensorPaths(existFeatures)   // ← HTTP 가 트랜잭션 안
    featureRepository.deleteAllByDeviceIdIn(removedIds)
    featureRepository.saveAll(features)
}
```

**다만 심각도가 다르다.** `fetchAllMobiusSensorPaths` 는 fan-out 이 아니라 **HTTP 호출 1회**
(`GET ?fu=1&ty=3&lvl=2`) + 순수 매핑이다. 점유 시간이 요청 하나 분량이라 §6.8 본체보다 가볍다.

호출 1회를 앞으로 빼는 것으로 충분하다.

```kotlin
// AiotService — @Transactional 제거
fun checkSynchronization() {
    val uril = fetchMobiusUril()              // 트랜잭션 밖. HTTP 1회
    featureSyncWriter.applyPaths(uril)        // 별도 빈, @Transactional
}

// 신규 — 기존 fetchAllMobiusSensorPaths 에서 HTTP 부분만 분리
private fun fetchMobiusUril(): List<String> {
    val response =
        mobiusLimiter { client.get().uri("?fu=1&ty=3&lvl=2").retrieve().body<MobiusUrilResponse>() }

    // 빈 응답을 emptyList() 로 흘리면 안 된다 — 아래 설명.
    return response?.uril ?: throw CustomException(ErrorCode.MOBIUS_EMPTY_RESPONSE)
}
```

#### 빈 응답을 `emptyList()` 로 흘리면 Feature 가 전량 삭제된다

**`?: emptyList()` 는 여기서 파괴적이다.** `RestClient.body(T)` 는 본문이 없으면 `null` 을 반환하는데,
이를 "정상적인 빈 목록" 으로 바꾸면 뒤따르는 동기화 로직이 그대로 실행된다.

```kotlin
val removedIds = existingIds - newIds.toSet()      // newIds 가 비었으므로 = 전체
featureRepository.deleteAllByDeviceIdIn(removedIds)  // 로컬 Feature 전량 삭제
```

**Mobius 가 일시적으로 빈 응답을 한 번 주면 로컬 Feature 가 전부 지워진다.**

현재 `awaitBody<MobiusUrilResponse>()` 는 본문이 없으면 **예외를 던져** 트랜잭션이 롤백되고 DB 가
그대로 남는다. 즉 기본값을 넣는 순간 **"안전하게 실패" 가 "조용히 전량 삭제" 로 바뀐다.**

> `ErrorCode.MOBIUS_EMPTY_RESPONSE` 를 추가한다. 동기화 실패는 다음 주기에 재시도되므로
> 예외로 끝내는 것이 맞다 — **삭제는 되돌릴 수 없지만 실패는 되돌릴 수 있다.**

> **같은 함정이 `retrieve().body(T)` 를 쓰는 모든 곳에 있다.** WebClient 의 `awaitBody` 는
> 본문 부재에 예외를 던지지만 RestClient 의 `body()` 는 `null` 을 반환한다.
> §6.6·§6.7 처럼 `?: throw` 로 받거나, 여기처럼 **기본값을 절대 넣지 않는다.**
> 특히 **결과가 삭제/비활성화로 이어지는 경로**에서는 기본값이 곧 데이터 손실이다.

`applyPaths` 는 기존 `fetchAllMobiusSensorPaths` 의 **매핑 로직을 그대로 옮기되**, 트랜잭션 안에서
`featureRepository.findAll()` 을 다시 호출해 영속 엔티티로 작업한다(R3).

#### R4 — 상한은 모든 Mobius 경로가 공유해야 한다

**세마포어를 `fetchAllStatuses` 에 걸면 안 된다.** Reactor Netty 의 상한은 **호스트당 집계**여서
아래 경로 전부가 하나의 풀을 나눠 쓰고 있었다.

```
FeatureScheduler:110            → fetchDeviceBatteryData        (08:00 cron)
FeatureController:137-139       → checkSynchronization / statusSynchronize / subscription
SensorDataMigrationService:149  → findByDateRange
MobiusConfigController:90       → removeAllSubscriptions
AiotService:410-411             → handleMobiusUrlUpdated
```

경로마다 별도 세마포어를 두면 **합계가 기존 상한을 넘는다.** 특히 §6.4 로 08:00 두 cron 이
**동시에** 돌게 되므로 배터리 동기화와 상태 동기화가 겹친다 — 상한을 재현하려다 오히려 늘리는 셈이다.

**해법: `AiotService` 에 세마포어를 하나 두고, Mobius 를 때리는 모든 지점을 그것으로 감싼다.**
Mobius HTTP 는 전부 `AiotService` 의 `client` 를 지나므로 이 한 곳으로 충분하다.

```kotlin
    // Reactor Netty 공유 커넥션 풀의 호스트당 상한(max(코어,8)×2)을 재현한다.
    // JDK HttpClient 에는 대응 설정이 없어 전환 시 이 상한이 사라진다.
    // 새 상수를 도입하는 게 아니라 기존 동작을 유지하는 것이다 (설계문서 §6.8).
    private val mobiusSemaphore =
        Semaphore(maxOf(Runtime.getRuntime().availableProcessors(), 8) * 2)

    // 반드시 leaf(실제 HTTP 호출)에서만 호출한다. 중첩 획득은 데드락이다 — 아래 주의.
    private fun <T> mobiusLimiter(block: () -> T): T {
        mobiusSemaphore.acquire()
        try {
            return block()
        } finally {
            mobiusSemaphore.release()
        }
    }
```

> **중첩 획득 금지 — `java.util.concurrent.Semaphore` 는 재진입이 안 된다.**
> `fetchAllStatuses` 처럼 fan-out 하는 쪽에서 획득한 뒤 그 안의 `fetchDeviceLocationData` 가
> 또 획득하면 **자기 자신을 기다리는 데드락**이 된다. 그래서 **fan-out 이 아니라 leaf 에만 건다.**
> 이 규칙을 지키면 호출자가 어떻게 조합하든 안전하다.

적용 대상은 **실제로 `client.get()`/`client.post()` 를 호출하는 메서드뿐**이다.

| 메서드 | 성격 | 적용 |
|---|---|---|
| `fetchDeviceLocationData` | leaf | O |
| `fetchDeviceBatteryData` | leaf | O |
| `fetchMobiusUril` | leaf | O |
| `findByDateRange` | leaf | O |
| `fetchSubscription` | leaf | O |
| `fetchRemoveSubscription` | leaf | O |
| `setupSubscriptionForFeature` | **조합** — 아래 참조 | **X** |
| `fetchAllStatuses` / `statusSynchronize` / `subscription` | 조합 | X |

> **`setupSubscriptionForFeature` 는 leaf 가 아니다.** 이름과 달리 HTTP 를 직접 호출하지 않고
> `fetchSubscription` 에 위임하며, **409(이미 존재) 를 받으면 `fetchRemoveSubscription` →
> `fetchSubscription` 재시도 경로를 탄다.**
>
> ```kotlin
> try {
>     fetchSubscription(...)                    // ← HTTP 는 여기
> } catch (e) {
>     if (409 && "resource is already exist") {
>         fetchRemoveSubscription(...)          // ← 재시도 경로에서 또 HTTP
>         fetchSubscription(...)
>     }
> }
> ```
>
> 여기에 permit 을 걸면 **재시도 경로가 permit 을 쥔 채 다시 획득**해 퍼밋 소진 시 데드락이다.
> **판정 기준은 메서드 이름이 아니라 `client` 를 직접 부르는지 여부다.**

#### `FeatureScheduler.scheduledBatteryDataUpdate`

동일한 3단계 구조로 바꾼다. 배터리만 다루므로 `FeatureStatusWriter` 에 메서드를 하나 더 둔다.

```kotlin
// FeatureScheduler — @Transactional 제거
@Profile("!local")
@Scheduled(cron = "0 0 8 * * ?")
fun scheduledBatteryDataUpdate() {
    val deviceIds = featureQueryService.findAllDeviceIds()          // ① 읽기 tx
    val levels = aiotService.fetchAllBatteryLevels(deviceIds)       // ② 트랜잭션 밖 HTTP
    featureStatusWriter.applyBatteryLevels(levels)                  // ③ 쓰기 tx
}
```

`aiotService.fetchAllBatteryLevels` 는 `fetchAllStatuses` 와 같은 형태의 가상 스레드 fan-out 이며,
내부의 `fetchDeviceBatteryData` 가 `mobiusLimiter` 를 통과하므로 상한이 공유된다.

### 6.9 `SensorDataMigrationService` / 나머지

- `SensorDataMigrationService:148` — `runBlocking { aiotService.findByDateRange(...) }` → 직접 호출
- `NgrokConfig:53,90` — `.block()` → RestClient. **단 타임아웃을 함께 옮겨야 한다 — 아래 참조**
- `AiotService:271,394` — `.block()` → RestClient. `WebClientResponseException` 캐치를
  **`RestClientResponseException` 으로 교체한다** (`AiotService:296`)
- `AiotServiceKoTest` — `runBlocking { }` 제거

#### `NgrokConfig` — 연산자 타임아웃이 사라진다

```kotlin
isNgrokRunning() → .timeout(Duration.ofSeconds(2))
fetchNgrokUrl()  → .timeout(Duration.ofSeconds(5))
```

**Reactor 연산자 레벨 타임아웃이라 RestClient 에 대응물이 없다.** 그냥 옮기면 팩토리 기본값
(read 30초)이 적용된다. 이 코드는 `@PostConstruct` 에서 도므로 **ngrok 이 연결은 받고 응답하지
않으면 로컬 프로파일 기동이 최대 30초 블로킹된다.**

**클라이언트 생성 시점에 짧게 건다.**

```kotlin
private val client: RestClient =
    restClientFactory.createClient(
        "http://localhost:4040",
        connectionTimeoutMs = 1000,
        readTimeoutMs = 5000,       // fetchNgrokUrl 기준
    )
```

> **`isNgrokRunning` 의 상한이 2초 → 5초로 늘어난다.** 기동 시 1회 프로브라 허용 가능하다고 본다.
> 정확히 맞추려면 클라이언트를 2개 두면 되지만, 5초도 30초와는 자릿수가 다르다.
> **로컬 전용(`@Profile("local")`)이라 운영 영향은 없다.**

**`WebClientResponseException` → `RestClientResponseException` 교체를 놓치면 컴파일은 통과하고
런타임에 catch 가 안 걸린다.** `AiotService:266` 의 주석("`.retrieve()` 여기서 비-2xx면
`WebClientResponseException` 던짐")도 함께 정정한다.

### 6.10 `EdsWebSocketClient` — 유지하되 기존 결함 두 개를 고친다

**WebFlux 를 그대로 쓴다**(§7). 다만 전환과 무관하게 존재하는 결함을 이번에 잡는다.

**(1) stale api-key 재연결 — 실질 장애 요인**

```kotlin
disposable = client.execute(buildUri()) { session -> ... }   // ← buildUri() 가 connect() 시점에 1회 평가
    .repeatWhen { ... }
    .retryWhen { ... }
```

`buildUri()` 는 평범한 Kotlin 표현식이라 `execute` 호출 **전에 한 번** 평가된다. `Mono` 에 URI 가
박히므로 `repeatWhen`/`retryWhen` 의 재구독은 **같은 api-key 를 재사용한다.**

그런데 `EdsClient.keepAlive()` 는 실패 시 `login()` 을 호출해 `apiKey` 를 갈아끼운다(`EdsClient:77`).
**재로그인 이후 WebSocket 이 끊기면 만료된 api-key 로 최대 2분 백오프로 영원히 재시도한다.**
로그에는 "재연결 시도 N회" 만 계속 찍히고 원인이 드러나지 않는다.

```kotlin
        disposable =
            Mono
                // 재구독마다 api-key 를 다시 읽는다. defer 없이는 connect() 시점의 키가 박혀
                // 재로그인 후 재연결이 영구 실패한다 (설계문서 §6.10).
                .defer { client.execute(buildUri()) { session -> ... } }
                .doOnSuccess { ... }
                .repeatWhen { ... }
                .retryWhen { ... }
                .subscribe()
```

**(2) 가시성**

```kotlin
    @Volatile private var stopped = false
    @Volatile private var disposable: Disposable? = null
```

`disconnect()` 는 라이프사이클 스레드에서, 읽기는 Reactor 스레드에서 일어난다.

**(3) `publishOn(Schedulers.boundedElastic())` 은 그대로 둔다**

`publishOn` 은 단일 worker 에 붙어 **메시지를 순차 처리**한다. 즉 현재 EDS 이벤트 처리 순서가
보장되고 있다. 가상 스레드로 바꾸고 싶은 유혹이 있으나 **순서 보장이 깨진다.**
`edsFacade.processEvent` 가 순서에 의존하는지 확인되지 않았으므로 **이번엔 손대지 않는다**(§11).

## 7. 남기는 것 — `EdsWebSocketClient` 의 WebFlux

사용자 결정으로 이번 범위에서 제외한다. 판단 근거를 기록해둔다.

`spring-websocket-7.0.x` 에 `StandardWebSocketClient` 가 이미 있고
(`CompletableFuture<WebSocketSession> execute(WebSocketHandler, WebSocketHttpHeaders, URI)`),
`spring-boot-starter-websocket` 이 이미 클래스패스에 있으므로 **의존성 추가는 필요 없다.**

문제는 API 교체가 아니라 Reactor 오퍼레이터가 공짜로 해주던 것을 직접 짜야 한다는 점이다.

| 잃는 것 | 직접 구현해야 하는 것 |
|---|---|
| `repeatWhen { delayElements(5s) }` | 정상 종료 후 5초 재연결 |
| `retryWhen(Retry.backoff(MAX, 5s).maxBackoff(2m))` | 지수 백오프 상태 관리 |
| `Disposable.dispose()` | `WebSocketSession.close()` + "이미 닫히는 중" 상태 처리 |
| `publishOn(boundedElastic)` | 컨테이너 스레드 밖으로 hand-off (**순차성 유지 필요** — §6.10) |

`WebSocketConnectionManager` 는 start/stop 라이프사이클만 제공하고 **자동 재연결은 없다.**
재연결 경로는 EDS 서버를 죽였다 살리는 수동 시험이 필요해 검증 비용이 높다.

**서버 측 STOMP(`@EnableWebSocketMessageBroker`)는 `org.springframework.web.socket.*` (서블릿 스택)이라
WebFlux 와 무관하다. 이번 작업으로 영향받지 않는다.**

## 8. 테스트 관점

### 8.1 착수 전 기록

```bash
./gradlew test                # 전량 통과 확인 + 결과 저장 (§3.4)
grep "More than one TaskScheduler bean" <운영로그>    # §2.3 진단 확정
grep "pool-[0-9]*-thread-" <운영로그>                 # §2.3 보조 확인
```

### 8.2 신규 테스트

safers 의 `ExecutorPolicyTest` / `AsyncConfigTest` 와 같은 형태로 **빈이 실제로 가상 스레드를 쓰는지**
런타임 확인한다. 설정 파일을 읽는 테스트가 아니라 스레드를 실제로 띄워 `Thread.isVirtual()` 을 본다.

| 대상 | 확인 |
|---|---|
| `taskExecutor` | `execute { }` 로 제출한 작업의 `Thread.currentThread().isVirtual` 이 `true` |
| `taskScheduler` | 즉시 실행 작업에서 동일 확인 |
| 빈 이름 | `context.getBeanNamesForType(TaskScheduler::class.java)` 에 **`"taskScheduler"` 가 있을 것** |

세 번째가 회귀 테스트의 핵심이다 — **이름이 바뀌면 §2.3 의 익명 폴백으로 조용히 되돌아간다.**
`CountDownLatch` + `AtomicBoolean` 으로 받고, 타임아웃을 걸어 무한 대기를 막는다.

**`taskScheduler` 빈 이름 회귀 테스트도 넣는다.** 이 이름이 바뀌면 §2.3 의 익명 폴백으로 조용히 되돌아간다.

```kotlin
// 이름이 "taskScheduler" 가 아니면 TaskSchedulerRouter 가 익명 단일 스레드로 폴백한다.
// 조용히 되돌아가는 회귀라 테스트로 고정한다 (설계문서 §2.3).
context.getBeanNamesForType(TaskScheduler::class.java) shouldContain "taskScheduler"
```

### 8.3 회귀 확인 대상

| 항목 | 확인 방법 |
|---|---|
| `@Scheduled` 3개가 `app-sched-` 에서 도는가 | 로그 `[%thread]` 확인. `pool-N-thread-` 가 안 나와야 한다 |
| 08:00 두 cron 이 동시에 시작하는가 | 시작 로그 타임스탬프 비교 |
| Mobius 배치 소요 시간 (§6.8) | **전환 전후를 비교한다.** 크게 느려졌다면 세마포어 계산식이 기존 실효 상한을 제대로 재현하지 못한 것이다 |
| STOMP 하트비트가 살아 있는가 | 클라이언트 연결 유지 확인 + `stomp-heartbeat-` 스레드 존재 |
| `@Transactional` dirty checking (§6.8) | `statusSynchronize` 실행 후 DB 반영 확인. **이번 작업 최대 위험** |
| 트랜잭션 점유 시간 (§6.8 R2) | 배치 중 `pg_stat_activity` 의 `idle in transaction` 또는 p6spy 로그로 **HTTP 대기 동안 커넥션을 잡고 있지 않은지** 확인 |
| Mobius 동시 호출 상한 (§6.8 R4) | 08:00 두 cron 이 겹치는 구간에서 Mobius 측 동시 접속 수가 `max(코어,8)×2` 를 넘지 않는지 |
| **빈 응답 방어 (§6.8)** | Mobius 를 빈 본문 200 으로 응답하는 스텁으로 바꾸고 `checkSynchronization` 실행 → **Feature 가 하나도 지워지지 않아야 한다.** 지워지면 `?: emptyList()` 가 남아 있는 것 |
| EDS API 7개 (§6.6) | `eds.enabled=true` 환경에서 수동 호출 |
| **`keepAlive` 재로그인 경로 (§6.6)** | keepAlive 를 실패시켜 catch 의 `login()` 이 돌고 `apiKey` 가 갱신되는지 확인. §6.10 과 함께 본다 |
| **EDS 가 4xx/5xx 를 낼 때 (§6.6)** | 잘못된 api-key 로 호출 → **`EDS_LOGIN_FAILED`/`EDS_API_ERROR` 가 나와야 한다.** 500 이 나오면 `throwOnHttpError` 설정이 빠진 것 |
| 썸네일 크기 상한 (§6.6) | 1MB 초과 응답을 주는 스텁으로 호출 → `null` 반환 + 경고 로그. **chunked 응답(Content-Length 없음)으로도 확인** |
| RestClient null 응답 (§6.7) | LLM 서버 빈 응답 시나리오 |
| Hikari 풀 (§6.3) | 설정 변경 없음. `SQLTransientConnectionException` 이 새로 나타나는지만 본다 |

### 8.4 테스트로 못 잡는 것

- **§6.8 의 dirty checking** — 단위 테스트로는 트랜잭션 스레드 경계가 재현되지 않는다.
  실제 DB 를 쓰는 통합 시나리오나 수동 확인이 필요하다.
- **§6.10 의 stale api-key** — 재로그인 후 연결 끊김이라는 조합이 필요하다. 수동 시험.
- **유입 제한 지점 이동(§6.3)** — 과부하 시에만 드러난다. 부하 시험 없이는 확인할 수 없다.

## 9. 남는 한계 (인지 사항)

1. **`webflux` / `reactor-netty` 가 남는다.** `EdsWebSocketClient` 하나 때문이다.
   Netty 이벤트 루프 스레드(플랫폼)가 계속 존재하지만 가상 스레드와 간섭하지 않는다.
2. **`kotlinx-coroutines-*` 가 사용처 0 인 채로 남는다.** §1.1 의 판단이다.
3. **`applicationTaskExecutor` 는 여전히 만들어지지 않는다.** `Executor` 타입 빈이 6개 있어
   자동설정이 계속 백오프한다. 현재 MVC async 사용처가 0이라 무해하지만, `SseEmitter` 등을
   도입하면 이 사실을 다시 확인해야 한다(§2.2).
4. **`heartBeatScheduler` 는 플랫폼 스레드로 남는다.** poolSize 1 이고 하는 일이 타이밍 관리라 무해하다.
5. **RestClient 로 바뀌면서 `responseTimeout` / write 타임아웃이 사라진다.** connect/read 2종만 남고,
   `readTimeout` 이 유일한 방어선이 된다(§6.5).
6. **Semaphore 값의 성격이 서로 다르다.** LLM 은 기존 `llm.api.concurrency-limit`(기본 5)을
   그대로 옮긴 것이고, Mobius 는 **설정값이 아니라 `max(코어,8)×2` 계산식**이다 —
   Reactor Netty 가 암묵적으로 걸고 있던 상한을 재현한다(§6.8). 후자는 운영 중 조정이 불가능하며,
   조정이 필요해지면 그때 설정으로 승격시킨다(§11-3).
7. **커넥션 풀 대기자 수에 상한이 없어진다**(§6.3). 설정을 바꾸지 않기로 했으므로 이 상태로 운영하며,
   `SQLTransientConnectionException` 이 관측되면 그때 재검토한다.
8. **PostGIS 의존 쿼리가 테스트되지 않는다**(§3.7). H2 `MODE=PostgreSQL` 은 PostGIS 함수를 제공하지 않는다.
9. **`handleMobiusUrlUpdated` 의 원자성이 약해진다**(§6.8). 세 동기화 단계가 각자 트랜잭션을 가지므로
   중간 실패 시 부분 반영이 남는다. 다음 동기화에서 수렴하는 성질에 의존한다.
10. **`mobiusSemaphore` 는 중첩 획득 시 데드락이다**(§6.8 R4). leaf 에만 건다는 규칙을 코드로
   강제하지 못한다. **판정 기준은 메서드 이름이 아니라 `client` 를 직접 부르는지 여부다** —
   `setupSubscriptionForFeature` 가 이름과 달리 조합 메서드인 것이 그 예다.
11. **`RestClient.body()` 의 null 반환이 전역 함정이다**(§6.8). WebClient 의 `awaitBody` 와 계약이
   다르므로 전환 시 모든 호출부에서 기본값을 넣지 않았는지 확인해야 한다. 컴파일러가 잡아주지 않는다.

## 10. 작업 순서 / 커밋 단위

**회귀 시 이분탐색이 가능하도록 커밋을 분리한다.** 각 커밋에서 `./gradlew build` 가 통과해야 한다.

| # | 커밋 | 내용 | 위험도 |
|---|---|---|---|
| 1 | `chore: Gradle 9.7.0 / Kotlin 2.4.10 / Spring Boot 4.1.0 업그레이드` | §6.1 중 플러그인·wrapper | 중 |
| 2 | `chore: kotlin-jdsl·springwolf·influxdb 의존성 버전 정렬` | §3.5, §6.1 | 낮음 |
| 3 | `test: Kotest 6.2.4 마이그레이션` | §3.4 | **높음** — `ProjectConfig` |
| 4 | `fix: EdsWebSocketClient 재연결 시 api-key 재평가` | §6.10 | 낮음 — **단독으로도 가치 있음** |
| 5 | `feat: 가상 스레드 활성화 및 taskExecutor/taskScheduler 명시` | §6.2, §6.4, §8.2 | **높음** — §2.3 해소 |
| 6 | `feat: RestClientFactory 추가 및 EdsClient·NgrokConfig 전환` | §6.5, §6.6, §6.9 Ngrok. **`WebClientFactory`/`WebClientConfig` 는 남겨둔다** | 중 |
| 7 | `refactor: LlmMessageService 코루틴 제거` | §6.7 | 중 |
| 8 | `refactor: Mobius 동기화 트랜잭션 경계 분리 및 동시성 상한 공유` | §6.8 — `FeatureQueryService`·`FeatureStatusWriter` 신설, `handleMobiusUrlUpdated`/`checkSynchronization` 의 `@Transactional` 제거 | **최고** |
| 9 | `refactor: 잔여 runBlocking 제거` | §6.9 | 낮음 |
| 10 | `chore: WebClientFactory·WebClientConfig 제거` | 마지막 소비자가 옮겨간 뒤 | 낮음 |

`§6.3`(HikariCP)은 **설정 변경이 없으므로 커밋이 없다.**

#### 6번에서 옛 인프라를 지우면 안 되는 이유

`RestClientFactory` 를 **추가**하되 `WebClientFactory` / `WebClientConfig.webClientBuilder` 는
**10번까지 남긴다.** 6번에서 지우면 아직 옮기지 않은 소비자가 컴파일되지 않는다.

| 소비자 | 의존 | 옮기는 커밋 |
|---|---|---|
| `LlmMessageService` | `WebClient.Builder` | 7 |
| `AiotService` | `WebClientFactory` | 8 |
| `AiotServiceKoTest` | 위와 동일 | 9 |

**이 순서를 어기면 "각 커밋에서 빌드가 통과한다" 는 전제가 깨지고, 그러면 애초에 커밋을 나눈
이유(회귀 시 이분탐색)가 사라진다.** 두 팩토리가 잠시 공존하는 비용이 훨씬 싸다.

> `EdsWebSocketClient` 는 `WebClientFactory` 를 쓰지 않는다(`ReactorNettyWebSocketClient` +
> 자체 `HttpClient.create()`). 따라서 **10번에서 두 클래스를 완전히 제거할 수 있다** —
> `spring-boot-starter-webflux` 의존성만 §7 사유로 남는다.

**1~3(버전)과 5~10(가상 스레드)은 별도 PR 로 나눌 수 있으면 나눈다.** 성격이 다르고 회귀 원인이 섞이면
분리하기 어렵다. **4번은 단독 hotfix 로 먼저 내보내도 된다** — 나머지와 독립적이고 실제 장애를 막는다.

## 11. 후속 과제

| # | 과제 | 조건 |
|---|---|---|
| 1 | `EdsWebSocketClient` → `StandardWebSocketClient` 전환, `webflux`/`coroutines` 제거 | 재연결 수동 시험 시간 확보 시 (§7) |
| 2 | `EdsClient` → `@HttpExchange` 인터페이스 + `RestClientAdapter` | §6.6 안정화 후. `if (response.code != 200) throw` 반복이 `defaultStatusHandler` 로 모인다 |
| 3 | Mobius 동시성 상한을 설정으로 승격 | 08:00 배치 로그 2주 관측 후, **`max(코어,8)×2` 가 과하다는 것이 측정으로 확인되면** (§6.8). 그전에는 계산식 유지 |
| 4 | `publishOn` 순차성 의존 여부 확인 | `edsFacade.processEvent` 가 이벤트 순서에 의존하는지 (§6.10-3) |
| 5 | H2 → Testcontainers PostgreSQL | **H2 는 `@SpringBootTest` 3개가 실제로 쓰고 있어 제거 대상이 아니다.** 다만 PostGIS 함수를 제공하지 않아 `findFirstByPointInPolygon` 경로가 미검증이다 (§3.7). safers `2026-08-07-postgres-testcontainers-design.md` 참조 |
| 6 | MDC traceId 전파 | safers `2026-08-04-mdc-trace-id-design.md` 참조. 가상 스레드 전환 후가 더 쉽다 |
| 7 | 버전 카탈로그 도입 | 멀티모듈화 시점 (§3.6) |
| 8 | actuator + micrometer 도입 | §6.3 의 `hikaricp_connections_pending` 관측용. **풀 설정 변경 여부는 이 지표를 본 뒤 판단한다** |

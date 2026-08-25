# JWT / 인증 로직 safers-api 정렬 설계

- 작성일: 2026-08-25
- 대상: `authentication/**`, `global/config/CommonSecurityConfig.kt`, `global/properties/JwtProperties.kt`,
  `global/constant/SecurityConstants.kt`, `src/main/resources/application-common.yml`,
  **`src/test/resources/application.yml`**
- 환경: Java 25, Spring Boot 4.0.3(→4.1.0), Spring Security 7, Spring Framework 7.0.5
- 함께 읽을 것: `docs/specs/2026-08-25-virtual-threads-design.md` — §3.1 의존성 표에서 jjwt 제거를 다룬다
- 참조: `safers-api/apps/safers/src/main/kotlin/com/pluxity/safers/auth/**`

### 외부 리뷰 반영 (Codex, 2026-08-25)

| 지적 | 판정 | 반영 |
|---|---|---|
| `signOut` 의 `refreshToken?.let` 가드가 남아 `expireCookie` 변경이 무효화된다 | **타당** — 가드가 두 겹인 것을 놓쳤다 | §4.8 에 `signOut` 재구조화 추가, §3-9 판정 갱신, §6.3 확인 항목 추가 |
| §5.1 의 제목·도입 문장이 "무효화된다" 인데 본문은 "무효화되지 않는다" 로 자기모순 | **타당** — 초안 수정 시 본문만 고치고 제목을 방치했다. 앞부분만 읽으면 정반대로 이해한다 | §5.1 제목·도입 전면 교체, §4.2 문장 정정, §5.2 도입부 보강 |
| §5.2 의 "secret 교체" 가 단수라 액세스 쪽만 갈 여지가 있다. 그러면 리프레시 토큰이 살아남아 재발급으로 세션이 복구돼 **B 안이 아무것도 무효화하지 못한다** | **타당** — aiot 는 secret 이 두 개인데 단수로 썼다 | **secret 교체를 이번 범위에서 제외**(사용자 결정). §5.2 를 "신규 토큰에만 적용" 방침으로 재작성하고, 향후 교체 시 **두 secret 을 함께** 갈아야 한다는 주의를 각주로 남김. §7-1·§9-1 갱신 |
| `WhiteListPath` 에 `auth/sign-out` 이 없어 만료 토큰으로는 로그아웃이 막힌다 — §4.8 의 무조건 쿠키 삭제에 도달하지 못한다 | **타당** — 수명을 10h 로 줄이면 오히려 흔해지는 시나리오다 | §4.7 에 `AUTH_OUT` 추가 + 근거·위험 분석, §3-6-1 판정 추가, §6.2 테스트·§6.3 수동 확인 항목 추가 |
| §5.3 의 `xargs -r redis-cli TTL` 은 `TTL` 이 키 하나만 받으므로 레코드가 2개 이상이면 실패한다 | **타당** — 다만 명령 이전에 **조치 자체가 불필요**했다. B 안 전제로 쓴 절이며, A 방침에서는 레코드가 고아가 아니다 | §5.3 을 "조치 불필요" 로 재작성하고 명령 삭제. `@Id` 가 username 이라 다음 로그인에 TTL 이 자가 갱신된다는 근거 추가 |

## 0. 배경 — 왜 지금인가

aiot 와 safers-api 의 인증 계층은 **같은 코드에서 갈라져 나왔다.** 패키지 경로만 다르고
`AuthenticationController` 는 Swagger 어노테이션까지 동일하며, **JWT secret 값도 두 프로젝트가 같다.**

그 사이 safers 쪽만 여러 차례 수정됐고 aiot 는 원본 상태로 남았다. 이번에 정렬하면서
**갈라진 뒤 safers 에서 고쳐진 결함들이 aiot 에 그대로 남아 있음**을 확인했다. §2 가 그중 가장 심각하다.

동시에 jjwt(`io.jsonwebtoken`) 3개 아티팩트를 nimbus-jose-jwt 1개로 줄인다.

### 사전 검증 결과 (2026-08-25, 실제 코드 / 클래스패스 기준)

| # | 확인 대상 | 결과 |
|---|---|---|
| ① | 토큰 수명 설정값의 단위가 맞는가 | **안 맞는다. 의도의 1000배다.** 액세스 토큰이 416일간 유효하다 (§2) |
| ② | 로그아웃이 액세스 토큰을 무효화하는가 | **안 한다.** Redis 의 리프레시 토큰만 지운다. 액세스 토큰은 자체 완결형이라 416일간 계속 통과한다 (§2.3) |
| ③ | 미인증 요청에 401 이 나가는가 | **아니다. 403 이 나간다.** `AuthenticationEntryPoint` 미지정 → Spring 기본 `Http403ForbiddenEntryPoint` (§3-5) |
| ④ | `WhiteListPath` 가 의도한 경로만 여는가 | **아니다.** 단순 `startsWith` 라 `/health` 가 `/health-actions` 까지 삼킨다 (§3-6) |
| ⑤ | `GET /users/me` 가 인증을 요구하는가 | **아니다.** `HttpMethod.GET permitAll` 이 먼저 매칭돼 익명 요청이 컨트롤러까지 간다 (§3-12) |
| ⑥ | 필터의 `ObjectMapper` 가 Boot 가 설정한 것인가 | **아니다.** Jackson **2** databind 를 직접 `new` 한다. Boot 4 는 Jackson **3**(`tools.jackson`)을 쓴다 (§3-7) |
| ⑦ | DTO 의 `com.fasterxml.jackson.annotation.*` 는 문제인가 | **아니다.** Jackson 3 도 애노테이션 패키지는 `com.fasterxml.jackson.annotation` 을 유지한다 (`jackson-annotations:2.20`). 손대지 않는다 |
| ⑧ | aiot 와 safers 의 권한 모델이 같은가 | **다르다.** aiot 는 `Role` 엔티티 기반 동적 모델, safers 는 `Authority` enum. **정렬 대상이 아니다** (§1 Out of scope) |

### 이번 범위 (In scope)

| 영역 | 내용 |
|---|---|
| **토큰 수명** | `TokenProperties.expiration` 을 `Long`(초 취급) → `Duration` 으로. yml 을 `10h` / `10d` 로 (§2) |
| **JWT 라이브러리** | jjwt 3개 → `com.nimbusds:nimbus-jose-jwt:10.3`. **만료 검증을 직접 구현**해야 한다 (§4.2) |
| **JwtProvider API** | `isAccessTokenValid(): Boolean`(실제로는 throw) → `extractAllClaims` / `validateRefreshToken` |
| **401 응답** | `RestAuthenticationEntryPoint` 신규 추가 |
| **화이트리스트** | `WhiteListPath.matches()` 경로 경계 매칭 |
| **쿠키** | `ResponseCookie` 기반 삭제 + path 단일 소스(`resolveXxxPath`) |
| **필터** | Jackson 3 `JsonMapper`, 예외 로깅 추가 |
| **트랜잭션** | `AuthenticationService` 클래스 `readOnly = true` + 쓰기 메서드만 `@Transactional` |
| **인가 규칙** | `/users/me/**` 순서 수정, `/admin/**` 활성화 (§4.6) |
| **CORS** | `CorsProperties` 도입 |
| **설정 바인딩** | `@ConstructorBinding` 제거, `@EnableConfigurationProperties` 명시 |
| **Redis TTL** | `RefreshToken.timeToLive` `Int` → `Long` |

### 이번 제외 (Out of scope)

| 항목 | 이유 |
|---|---|
| **`Role` → `Authority` enum 전환** | aiot 는 `Role`·`RoleType`·`RolePermission`·`UserRole` 로 **동적 권한 모델**을 갖고 있고 `PermissionCheckAspect` 가 이를 쓴다. safers 의 2값 enum 으로 바꾸는 것은 기능 축소다. **정렬하지 않는다** |
| safers 의 `PermissionContext` / `permission` 패키지 | aiot 에 이미 자체 `permission` 패키지가 있다. 별개 도메인 |
| `CustomException.errorCode` → `.code` 리네임 | 전 코드베이스에 걸친 기계적 변경. 이득 없음 |
| `Code.getStatusName()` → `getCodeName()` | 위와 같음. 필터에서 `.name` 을 쓰고 있고 safers 의 `getCodeName()` 도 `name` 을 반환해 **결과가 같다** |
| `StompSubscribeAuthorizationInterceptor` | STOMP 구독 인가. aiot 에 대응 기능이 없다. 별건 (§9) |
| DTO 의 Jackson 2 애노테이션 | 검증 ⑦ — 문제없다 |
| 비밀키를 yml 평문에서 분리 | 두 프로젝트 공통 문제. 별건 (§9) |

## 2. 최우선 결함 — 토큰 수명이 의도의 1000배다

### 2.1 확인

`application-common.yml`:

```yaml
jwt:
  access-token:
    secret: +iBcUJRWGvl+94+ow4nXV1fzWIq4rph8x7MyRmrtWio=
    expiration: 36000000
  refresh-token:
    secret: gtzRlqF6bIkmOi5i15A9G5xbLdwiAMmZi6JPOeemC1E=
    expiration: 864000000
```

safers-api 는 **같은 secret, 같은 숫자**를 쓰면서 이렇게 적어놨다.

```yaml
# safers-api/apps/safers/src/main/resources/application-common.yml:57-65
jwt:
  access-token:
    secret: +iBcUJRWGvl+94+ow4nXV1fzWIq4rph8x7MyRmrtWio=
    expiration: 10h # Duration 표기(예: 30m, 10h, 10d). 단위 없는 숫자는 ms로 해석된다.
  refresh-token:
    secret: gtzRlqF6bIkmOi5i15A9G5xbLdwiAMmZi6JPOeemC1E=
    expiration: 10d
```

`10h` = **36,000,000 밀리초**. `10d` = **864,000,000 밀리초**. 숫자가 정확히 일치한다 —
**aiot 의 값은 밀리초 의도로 쓰였다.**

**`src/test/resources/application.yml` 에도 같은 값이 복제돼 있다.** 두 파일을 함께 고쳐야 한다.

```yaml
# src/test/resources/application.yml:33-40 — 운영과 동일한 secret / 동일한 숫자
jwt:
  access-token:
    expiration: 36000000
  refresh-token:
    expiration: 864000000
```

그런데 aiot 코드는 이 값을 **초로 해석한다.**

```kotlin
// JwtProperties.kt
val expiration: Long,

// JwtProvider.buildToken
.expiration(Date(System.currentTimeMillis() + expiration * 1000))   // ← 초 → ms 변환
```

### 2.2 소비 지점 4곳 전부 초로 취급한다

| 지점 | 코드 | 단위 해석 | 실제 결과 (access / refresh) |
|---|---|---|---|
| JWT `exp` 클레임 | `Date(now + expiration * 1000)` | 초 | **416.7일** / **10,000일(27.4년)** |
| 인증 쿠키 `maxAge` | `ResponseCookie.maxAge(expiry)` — Long 오버로드는 **초** | 초 | 416.7일 / 27.4년 |
| Redis TTL | `RefreshToken(username, token, expiration.toInt())`, `@TimeToLive` 는 **초** | 초 | — / 27.4년 |
| `expiry` 쿠키 값 | `now + expiration * 1000L` | 초 → ms | — / 27.4년 후 타임스탬프 |

**네 곳이 일관되게 초로 다루고 있으므로 코드 내부에는 모순이 없다.** 설정값의 단위만 어긋났다.
그래서 지금까지 아무 증상 없이 동작했다 — 토큰이 만료되지 않으니 오류가 날 일이 없다.

> `expiration.toInt()` 는 별개로 위험하다. `864000000` 은 `Int` 범위 안이라 지금은 통과하지만,
> `Duration` 전환 없이 값만 키우면 조용히 오버플로한다. §4.5 에서 `Long` 으로 바꾼다.

### 2.3 왜 이게 심각한가 — 로그아웃이 액세스 토큰을 무효화하지 않는다

`AuthenticationService.signOut()` 은 Redis 의 `RefreshToken` 레코드를 지우고 쿠키를 만료시킨다.
그러나 **액세스 토큰은 서명만으로 검증되는 자체 완결형**이고, `JwtProvider` 에 무효화 목록이 없다.

```kotlin
// JwtAuthenticationFilter.authenticateRequest — 서명과 만료만 본다
if (token != null && jwtProvider.isAccessTokenValid(token)) {
    val username = jwtProvider.extractUsername(token)
    ...
}
```

즉 **한 번 발급된 액세스 토큰은 서버가 무엇을 하든 `exp` 까지 유효하다.** 그 `exp` 가 416일이다.

| 시나리오 | 현재 | 의도(10h) |
|---|---|---|
| 로그아웃 후 탈취된 토큰 재사용 | **416일간 가능** | 최대 10시간 |
| 사용자 삭제/권한 회수 후 | 다음 요청에서 `loadUserByUsername` 이 막아준다 | 동일 |
| 브라우저에 남은 쿠키 | 416일간 자동 로그인 | 10시간 |

두 번째 줄이 완화 요인이다 — 필터가 매 요청 `userDetailsService.loadUserByUsername()` 을 호출하므로
**사용자가 삭제되면 즉시 막힌다.** 그러나 로그아웃만으로는 막히지 않는다.

### 2.4 조치

`Duration` 으로 바꾸고 yml 을 safers 와 동일하게 맞춘다(§4.1, §4.5). **배포 영향은 §5 를 반드시 읽는다.**

## 3. 전수 대조

| # | 항목 | aiot (현재) | safers-api | 판정 |
|---|---|---|---|---|
| 1 | JWT 라이브러리 | `io.jsonwebtoken` jjwt-api/impl/jackson **0.12.6** (3개) | `com.nimbusds:nimbus-jose-jwt:10.3` (1개) | **교체** |
| 2 | `expiration` 타입 | `Long` (초 취급) | `Duration` | **교체** — §2 |
| 3 | 액세스 토큰 검증 | `isAccessTokenValid(token): Boolean` — 실패 시 `throw`, 성공 시 `true`. **반환값이 의미 없다** | `extractAllClaims()` 가 클레임 반환 또는 throw | **교체** |
| 4 | 리프레시 토큰 검증 | `isRefreshTokenValid(token): Boolean` — 같은 문제. 호출부가 `if (!valid) throw` 로 한 번 더 감싼다 | `validateRefreshToken(token)` (Unit 또는 throw) | **교체** |
| 5 | 미인증 진입점 | 없음 → Spring 기본 `Http403ForbiddenEntryPoint` → **403** | `RestAuthenticationEntryPoint` → **401** | **추가** |
| 6 | `WhiteListPath` 매칭 | `path.startsWith("/${entry.path}")` | 경로 경계(`/`, `.`) 확인 | **교체** |
| 6-1 | `WhiteListPath` 항목 | `sign-out` **누락** — 만료 토큰으로는 로그아웃이 막힌다 | safers 도 동일하게 누락 | **`AUTH_OUT` 추가**(§4.7). safers 정렬 범위를 넘는 수정 |
| 7 | 필터 JSON 직렬화 | `com.fasterxml.jackson.databind.ObjectMapper()` — **Jackson 2**, Spring 설정 미적용 | `tools.jackson.databind.json.JsonMapper()` — Jackson 3 | **교체** |
| 8 | 필터 예외 로깅 | `CustomException` 아니면 **조용히 무시** | `log.error(exception) { ... }` | **추가** |
| 9 | 쿠키 삭제 | `WebUtils.getCookie(...)?.apply { }` + `signOut` 의 `refreshToken?.let` — **가드가 두 겹이라 리프레시 쿠키가 없으면 아무것도 안 지운다** | `ResponseCookie.maxAge(0)` 무조건 발행. **단 바깥 가드는 safers 도 동일하다** | **교체 + 바깥 가드 제거**(§4.8) |
| 10 | 쿠키 path | 호출부마다 인라인 (`request.contextPath`, `"${contextPath}/"`) | `resolveAccessTokenPath()` / `resolveRefreshTokenPath()` / `resolveExpiryPath()` 단일 소스 | **교체** |
| 11 | 트랜잭션 | 메서드 4개 전부 쓰기 `@Transactional` | 클래스 `@Transactional(readOnly = true)` + `signUp` 만 쓰기 | **교체** |
| 12 | `/users/me` 인가 | `HttpMethod.GET permitAll` 이 먼저 → **익명 통과** | `/users/me/**` `authenticated()` 를 GET permitAll **앞에** 선언 | **교체** — 아래 |
| 13 | `/admin/**` 인가 | `// TODO: 구현 완료 시 적용` 주석 처리 | `hasRole(Authority.ADMIN.name)` | **활성화** — §4.6 |
| 14 | CORS 오리진 | 하드코딩 4개 | `CorsProperties.additionalOriginPatterns` + 로깅 | **교체** |
| 15 | `RefreshToken.timeToLive` | `Int` | `Long` | **교체** |
| 16 | `@ConstructorBinding` | 있음 | 없음 | **제거** (Boot 3+ 단일 생성자는 불필요) |
| 17 | `@EnableConfigurationProperties` | 없음 (`@ConfigurationPropertiesScan` 에 의존) | `CommonSecurityConfig` 에 명시 | **추가** |
| 18 | 권한 모델 | `Role` 엔티티 동적 | `Authority` enum | **정렬하지 않음** — §1 |

### #12 의 실제 영향 — 확인됨

```kotlin
// CommonSecurityConfig.kt:57-60 (현재)
.requestMatchers(HttpMethod.GET).permitAll()      // ← 여기서 GET /users/me 가 먼저 매칭된다
.requestMatchers("/auth/**").permitAll()
.anyRequest().authenticated()
```

```kotlin
// UserController.kt:58
@GetMapping("/me")
fun getUser(authentication: Authentication) =
    ResponseEntity.ok(DataResponseBody(service.findByUsername(authentication.name)))
```

익명 요청이 컨트롤러까지 도달하고 `authentication.name` 이 `"anonymousUser"` 가 되어
`findByUsername("anonymousUser")` 를 시도한다. **401 이어야 할 응답이 `NOT_FOUND_USER` 404 로 나간다.**

`PATCH /users/me`, `PATCH /users/me/password` 는 GET 이 아니라 `anyRequest().authenticated()` 에
걸리므로 정상이다. **`GET /me` 하나만 영향받는다.**

## 4. 변경 내역

### 4.1 `build.gradle.kts` — jjwt 제거

```kotlin
-   implementation("io.jsonwebtoken:jjwt-api:0.12.6")
-   implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
-   implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
+   implementation("com.nimbusds:nimbus-jose-jwt:10.3")
```

`jjwt-jackson` 이 Jackson 2 를 끌어오던 경로 하나가 사라진다 — 다만 다른 경로로도 들어오므로
Jackson 2 자체가 클래스패스에서 없어지지는 않는다(검증 ⑦).

### 4.2 `JwtProvider` — nimbus 전환 (만료 검증 직접 구현)

**가장 중요한 차이: jjwt 는 `parseSignedClaims()` 가 만료를 자동 검사하고 `ExpiredJwtException` 을
던졌지만, nimbus 의 `SignedJWT.verify()` 는 서명만 본다.** 만료 검사를 빠뜨리면
**만료된 토큰이 그대로 통과한다.** 아래 `expirationTime` 블록이 그 대응이다.

```kotlin
package com.pluxity.aiot.authentication.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.pluxity.aiot.authentication.repository.RefreshTokenRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.JwtProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import org.springframework.web.util.WebUtils
import java.time.Duration
import java.util.Base64
import java.util.Date

@Service
class JwtProvider(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
) {
    fun extractUsername(
        token: String,
        isRefreshToken: Boolean = false,
    ): String = extractAllClaims(token, isRefreshToken).subject

    private fun extractAllClaims(
        token: String,
        isRefreshToken: Boolean,
    ): JWTClaimsSet {
        val errorCode = if (isRefreshToken) ErrorCode.INVALID_REFRESH_TOKEN else ErrorCode.INVALID_ACCESS_TOKEN
        try {
            val signedJWT = SignedJWT.parse(token)
            val verifier = MACVerifier(getSecretKeyBytes(isRefreshToken))
            if (!signedJWT.verify(verifier)) {
                throw CustomException(errorCode)
            }
            val claims = signedJWT.jwtClaimsSet
            // jjwt 와 달리 nimbus 는 만료를 자동 검사하지 않는다. 빠뜨리면 만료 토큰이 통과한다.
            val expirationTime = claims.expirationTime
            if (expirationTime != null && expirationTime.before(Date())) {
                throw CustomException(
                    if (isRefreshToken) ErrorCode.EXPIRED_REFRESH_TOKEN else ErrorCode.EXPIRED_ACCESS_TOKEN,
                )
            }
            return claims
        } catch (e: CustomException) {
            throw e
        } catch (_: Exception) {
            throw CustomException(errorCode)
        }
    }

    fun generateAccessToken(
        username: String,
        extraClaims: Map<String, Any> = emptyMap(),
    ): String = buildToken(extraClaims, username, jwtProperties.accessToken.expiration, false)

    fun generateRefreshToken(username: String): String =
        buildToken(emptyMap(), username, jwtProperties.refreshToken.expiration, true)

    private fun buildToken(
        extraClaims: Map<String, Any>,
        username: String,
        expiration: Duration,
        isRefreshToken: Boolean,
    ): String {
        val now = System.currentTimeMillis()
        val claimsBuilder =
            JWTClaimsSet
                .Builder()
                .subject(username)
                .issueTime(Date(now))
                .expirationTime(Date(now + expiration.toMillis()))

        extraClaims.forEach { (key, value) -> claimsBuilder.claim(key, value) }

        val signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build())
        signedJWT.sign(MACSigner(getSecretKeyBytes(isRefreshToken)))
        return signedJWT.serialize()
    }

    fun validateRefreshToken(token: String?) {
        if (token.isNullOrBlank()) throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

        val refreshToken =
            refreshTokenRepository
                .findByToken(token)
                ?: throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

        if (!refreshToken.isValidToken()) {
            throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)
        }

        extractAllClaims(refreshToken.token, true)
    }

    private fun getSecretKeyBytes(isRefreshToken: Boolean): ByteArray {
        val key = if (isRefreshToken) jwtProperties.refreshToken.secretKey else jwtProperties.accessToken.secretKey
        return Base64.getDecoder().decode(key)
    }

    fun getAccessTokenFromRequest(request: HttpServletRequest): String? =
        getJwtFromRequest(jwtProperties.accessToken.name, request)

    fun getJwtFromRequest(
        name: String,
        request: HttpServletRequest,
    ): String? = WebUtils.getCookie(request, name)?.value
}
```

**제거되는 것: `extractClaim(token, claimsResolver, isRefreshToken)`.**
현재 `extractUsername` 만 쓰고 있어(전 코드베이스 검색 결과 외부 호출 없음) 안전하게 지운다.

**서명 알고리즘은 HS256 으로 동일하고 키 바이트도 같다.** jjwt 의
`Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))` 와 nimbus 의 `MACSigner(Base64.decode(secretKey))` 는
같은 base64 문자열을 디코딩한 동일한 바이트 배열을 HMAC 키로 쓴다.
→ **기존 토큰의 서명은 그대로 검증된다.** 라이브러리 교체는 기존 토큰에 아무 영향이 없다.
수명 정정(§2)도 마찬가지다 — **이번 작업 어느 것도 기존 토큰을 무효화하지 않는다**(§5.1).

**키 길이 확인 — nimbus `MACSigner` 는 HS256 에 256비트 이상을 요구한다** (jjwt 도 동일하나 예외 메시지가 다르다).

```
secret 문자열 44바이트 -> secretKey(base64) 디코딩 후 44바이트 = 352비트  ≥ 256비트  OK
```

두 secret 모두 통과한다. **§9-1 로 secret 을 교체할 때 이 하한을 지켜야 한다** —
짧은 값을 넣으면 기동 시가 아니라 **첫 로그인 시점에** `KeyLengthException` 으로 터진다.

### 4.3 `JwtAuthenticationFilter`

```kotlin
-import com.fasterxml.jackson.databind.ObjectMapper
+import io.github.oshai.kotlinlogging.KotlinLogging
+import tools.jackson.databind.json.JsonMapper
+
+private val log = KotlinLogging.logger {}
```

```kotlin
        }.onFailure { exception ->
            if (exception is CustomException) {
                handleAuthenticationError(response, exception)
                return
            }
+           log.error(exception) { "JWT 인증 처리 중 예기치 않은 오류 발생" }
        }
```

```kotlin
    private fun authenticateRequest(request: HttpServletRequest) {
        val token = jwtProvider.getAccessTokenFromRequest(request)

-       if (token != null && jwtProvider.isAccessTokenValid(token)) {
+       if (token != null) {
+           // extractUsername 안에서 서명·만료를 검증하고 실패 시 CustomException 을 던진다.
            val username = jwtProvider.extractUsername(token)
            val userDetails = userDetailsService.loadUserByUsername(username)
            setAuthenticationContext(request, userDetails)
        }
    }
```

`handleAuthenticationError` 의 `ObjectMapper()` → `JsonMapper()`.
`exception.errorCode` 는 aiot 이름을 유지한다(§1 Out of scope).

### 4.4 `RestAuthenticationEntryPoint` — 신규

`authentication/security/RestAuthenticationEntryPoint.kt`

```kotlin
package com.pluxity.aiot.authentication.security

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.response.ErrorResponseBody
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import tools.jackson.databind.json.JsonMapper

/**
 * 인증되지 않은 요청의 진입점. 지정하지 않으면 Spring 기본값이 `Http403ForbiddenEntryPoint` 라서
 * 미인증 요청에도 403이 나간다 — 인증 부재는 401, 권한 부족이 403이므로 여기서 401로 맞춘다.
 *
 * 이 응답은 `ExceptionTranslationFilter` 단계에서 나가 `@RestControllerAdvice` 를 타지 않으므로,
 * 본문을 직접 `ErrorResponseBody` 로 써서 나머지 API 와 형식을 통일한다.
 */
class RestAuthenticationEntryPoint : AuthenticationEntryPoint {
    private val objectMapper = JsonMapper()

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val code = ErrorCode.UNAUTHENTICATED
        response.status = code.getHttpStatus().value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(
            objectMapper.writeValueAsString(
                ErrorResponseBody(
                    status = code.getHttpStatus(),
                    message = code.getMessage(),
                    code = code.getHttpStatus().value().toString(),
                    error = code.name,
                ),
            ),
        )
    }
}
```

**`ErrorCode.UNAUTHENTICATED` 를 새로 추가한다.** aiot 에는 없다.

```kotlin
// global/constant/ErrorCode.kt
+   UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
```

### 4.5 `JwtProperties` / `RefreshToken` / `application-common.yml`

```kotlin
// global/properties/JwtProperties.kt
package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration
import java.util.Base64

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val accessToken: TokenProperties,
    val refreshToken: TokenProperties,
)

data class TokenProperties(
    val name: String,
    val secret: String,
    val expiration: Duration,
) {
    val secretKey: String = Base64.getEncoder().encodeToString(secret.toByteArray())
}
```

`@ConstructorBinding` 을 제거한다 — Boot 3 부터 단일 생성자 클래스에는 불필요하다.

```kotlin
// authentication/entity/RefreshToken.kt
-   @TimeToLive val timeToLive: Int,
+   @TimeToLive val timeToLive: Long,
```

```yaml
# application-common.yml
jwt:
  access-token:
    name: AccessToken
    secret: +iBcUJRWGvl+94+ow4nXV1fzWIq4rph8x7MyRmrtWio=
    expiration: 10h # Duration 표기(예: 30m, 10h, 10d). 단위 없는 숫자는 ms로 해석된다.
  refresh-token:
    name: RefreshToken
    secret: gtzRlqF6bIkmOi5i15A9G5xbLdwiAMmZi6JPOeemC1E=
    expiration: 10d
```

```yaml
# src/test/resources/application.yml — 운영 yml 과 표기를 맞춘다
jwt:
  access-token:
    name: AccessToken
    secret: +iBcUJRWGvl+94+ow4nXV1fzWIq4rph8x7MyRmrtWio=
    expiration: 10h
  refresh-token:
    name: RefreshToken
    secret: gtzRlqF6bIkmOi5i15A9G5xbLdwiAMmZi6JPOeemC1E=
    expiration: 10d
```

**yml 주석을 반드시 함께 넣는다.** 단위 없는 숫자가 ms 로 해석된다는 사실이 §2 결함의 원인이었다.

> **테스트 yml 을 빠뜨려도 컴파일과 테스트는 통과한다.** `Duration` 바인딩에서 단위 없는 `36000000` 은
> **ms 로 해석돼 우연히 10시간이 되기 때문**이다. 즉 값은 맞고 표기만 갈린다.
> 그래서 더 위험하다 — 아무 신호 없이 두 파일의 표기가 어긋난 채 남는다. §6.2 의
> `JwtPropertiesTest` 가 `test` 프로파일로 도는 이상 이 테스트도 통과하므로 **잡아주지 못한다.**
> 체크리스트로만 막을 수 있다.

### 4.6 `CommonSecurityConfig`

```kotlin
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties::class, UserProperties::class, CorsProperties::class)
class CommonSecurityConfig(
    private val repository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val corsProperties: CorsProperties,
) {
```

인가 규칙:

```kotlin
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/actuator/**",
                        "/health",
                        "/subscription",
                        "/info",
                        "/prometheus",
                        "/error",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/swagger-config/**",
                        "/docs/**",
                        "/auth/**",
                    ).permitAll()
                    .requestMatchers("/admin/**")
                    .hasRole(RoleType.ADMIN.name)
                    // GET permitAll 보다 먼저 선언해야 한다. 뒤에 두면 permitAll 이 먼저 매칭돼
                    // 익명 요청이 컨트롤러까지 도달하고 "anonymousUser" 로 조회를 시도한다(설계문서 §3-12).
                    .requestMatchers("/users/me/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET)
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.exceptionHandling { it.authenticationEntryPoint(RestAuthenticationEntryPoint()) }
```

**`/users/me/**` 는 `/users/me` 자체를 포함하지 않는다.** Spring 의 `/**` 는 0개 이상 경로 세그먼트에
매칭되므로 `/users/me` 도 잡힌다 — 하지만 safers 와 동일하게 두되 **테스트로 확정한다**(§6).

**`hasRole(RoleType.ADMIN.name)` 의 전제를 확인한다.** `CustomUserDetails` 는
`user.getRoles().map { SimpleGrantedAuthority(it.getAuthority()) }` 이고
`Role.getAuthority()` 는 `"ROLE_$auth"` 를 반환한다. `hasRole("ADMIN")` 은 `ROLE_ADMIN` 을 찾으므로
**`Role.auth` 값이 정확히 `"ADMIN"` 인 레코드가 있어야 한다.** `RoleType.ADMIN.name` 과 일치한다.

> **착수 전 확인 필수:** 운영 DB 의 `roles` 테이블에서 `auth` 컬럼 값을 확인한다.
> `ADMIN` 이 아닌 값(소문자, 커스텀 문자열)이 쓰이고 있으면 `/admin/**` 를 켜는 순간
> **관리자가 전부 잠긴다.** 이것이 원 작성자가 `// TODO` 로 남겨둔 이유일 가능성이 높다.
> ```sql
> SELECT id, name, auth FROM roles;
> SELECT DISTINCT r.auth FROM roles r JOIN user_roles ur ON ur.role_id = r.id;
> ```
> 값이 맞지 않으면 **#13 만 이번 범위에서 뺀다.** 나머지와 독립적이다.

CORS:

```kotlin
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val defaultPatterns =
            listOf(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.*.*:*",
                "https://*.pluxity.com",
            )

        val allPatterns = defaultPatterns + corsProperties.additionalOriginPatterns
        log.info { "CORS allowedOriginPatterns: $allPatterns" }

        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = allPatterns.toMutableList()
        configuration.allowedMethods = mutableListOf("GET", "PATCH", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = mutableListOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
```

`global/properties/CorsProperties.kt` 신규:

```kotlin
package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    val additionalOriginPatterns: List<String> = emptyList(),
)
```

**aiot 는 `127.0.0.1` 패턴을 유지한다** — safers 에는 없지만 지울 이유가 없다.
safers 의 `*.trycloudflare.com` 은 넣지 않는다(aiot 에 해당 사용 이력 없음).

### 4.7 `WhiteListPath`

```kotlin
enum class WhiteListPath(
    val path: String,
) {
    AUTH_IN("auth/sign-in"),
    AUTH_UP("auth/sign-up"),
    AUTH_OUT("auth/sign-out"),
    REFRESH_TOKEN("auth/refresh-token"),
    ACTUATOR("actuator"),
    APIDOC("api-docs"),
    HEALTH("health"),
    INFO("info"),
    PROMETHEUS("prometheus"),
    SWAGGER("swagger-ui"),
    ;

    companion object {
        /**
         * 경로 경계 기준 매칭. 단순 startsWith 는 "/health" 가 "/health-actions" 까지 삼켜
         * 인증을 건너뛰게 하므로, 정확히 일치하거나 다음 문자가 경로 구분자('/')
         * 또는 확장자 구분자('.', /swagger-ui.html)일 때만 화이트리스트로 본다.
         */
        fun matches(path: String): Boolean =
            entries.any { entry ->
                val prefix = "/${entry.path}"
                path.startsWith(prefix) &&
                    (path.length == prefix.length || path[prefix.length] == '/' || path[prefix.length] == '.')
            }
    }
}
```

**`HEALTH("health")` 는 aiot 고유이므로 유지한다** (safers 에는 없다). 이 항목이야말로
`startsWith` 결함의 실제 위험 대상이다 — `/health` 로 시작하는 다른 엔드포인트가 생기면 인증이 뚫린다.

#### `AUTH_OUT("auth/sign-out")` 을 새로 추가한다 — 없으면 로그아웃이 막힌다

현재 화이트리스트에 `sign-in`·`sign-up`·`refresh-token` 은 있는데 **`sign-out` 이 없다.**
`JwtAuthenticationFilter` 는 인증 실패 시 응답을 쓰고 **필터 체인을 중단**하므로:

```
POST /auth/sign-out  (만료된 AccessToken 쿠키 동봉)
  → authenticationRequired = true         (화이트리스트에 없으므로)
  → extractUsername → EXPIRED_ACCESS_TOKEN
  → handleAuthenticationError 가 401 을 쓰고 return    ← 컨트롤러 미실행
  → signOut() 이 돌지 않아 쿠키·Redis 레코드가 그대로 남는다
```

**§4.8 에서 "쿠키 만료는 조건 없이 항상 수행한다" 로 고쳐도 그 코드에 도달하지 못한다.**

**이번 작업이 이 문제를 더 자주 만든다.** 지금은 액세스 토큰이 416일이라 만료가 드물지만,
10시간으로 줄이면 **10시간 뒤 돌아온 사용자가 로그아웃을 누르는 것이 평범한 시나리오**가 된다.

실질 위험은 재로그인 여부에 갈린다.

| 이후 행동 | 결과 |
|---|---|
| 다시 로그인한다 | `publishToken` 이 같은 이름·path 로 쿠키를 덮어쓰고 Redis 레코드도 upsert 된다 → **자가 치유** |
| 로그아웃만 하고 떠난다 | **리프레시 토큰이 최대 10일간 살아 있다.** `/auth/refresh-token` 은 화이트리스트라 액세스 토큰을 보지 않으므로, 같은 브라우저에서 앱을 다시 열면 세션이 복구된다 |

두 번째가 문제다 — **"로그아웃을 눌렀는데 로그아웃되지 않은 상태"** 가 남는다. 공용 PC 라면
다음 사용자가 세션을 이어받을 수 있다.

**화이트리스트 추가는 안전하다.** `signOut` 은 리프레시 쿠키만 읽어 동작하므로 액세스 토큰 인증이
필요 없고, 쿠키를 가진 주체만 자기 세션을 끊을 수 있다.

`JwtAuthenticationFilter.authenticationRequired` 를 `!WhiteListPath.matches(path)` 로 바꾼다.

### 4.8 `AuthenticationService`

트랜잭션 경계:

```kotlin
@Service
@Transactional(readOnly = true)
class AuthenticationService(
    ...
) {
    @Transactional
    fun signUp(signUpRequest: SignUpRequest): Long { ... }

    fun signIn(...) { ... }      // @Transactional 제거 — 조회 + 쿠키 발급뿐
    fun signOut(...) { ... }     // Redis 삭제는 JPA 트랜잭션 밖이다
    fun refreshToken(...) { ... }
```

> **`signOut` / `publishToken` 의 `refreshTokenRepository` 는 Redis(`@RedisHash`)다.**
> JPA 트랜잭션과 무관하므로 `@Transactional` 을 떼도 동작이 바뀌지 않는다.
> `signUp` 만 실제 JPA 쓰기라 유지한다.

리프레시 검증:

```kotlin
-       if (!jwtProvider.isRefreshTokenValid(refreshToken)) {
-           throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)
-       }
+       jwtProvider.validateRefreshToken(refreshToken)
```

쿠키 path 단일 소스 + `ResponseCookie` 기반 삭제:

```kotlin
    // 쿠키 경로는 생성/삭제가 반드시 동일해야 브라우저가 매칭하여 제거할 수 있다. 단일 소스로 관리한다.
    private fun resolveAccessTokenPath(request: HttpServletRequest): String =
        request.contextPath.takeIf { it.isNotBlank() } ?: "/"

    private fun resolveRefreshTokenPath(request: HttpServletRequest): String = "${request.contextPath}/"

    private fun resolveExpiryPath(request: HttpServletRequest): String =
        request.contextPath.takeIf { it.isNotEmpty() } ?: "/"

    // maxAge=0 + 생성과 동일한 path로 Set-Cookie를 내려 브라우저가 즉시 제거하도록 한다.
    private fun expireCookie(
        name: String,
        path: String,
        response: HttpServletResponse,
    ) {
        val cookie =
            ResponseCookie
                .from(name, "")
                .secure(false)
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(0)
                .path(path)
                .build()
                .toString()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie)
    }
```

**현재 `deleteAuthCookie` 는 `WebUtils.getCookie(request, name)?.apply { ... }` 라서
요청에 쿠키가 없으면 아무 `Set-Cookie` 도 내려보내지 않는다.** 브라우저에 쿠키가 남아 있는데
요청에 실려오지 않는 경우(path 불일치 등) 영원히 지워지지 않는다. `ResponseCookie` 방식은 무조건 발행한다.

#### `signOut` 의 바깥 가드도 함께 걷어낸다 — 안 그러면 위 변경이 무의미하다

가드가 **두 겹**이다. `expireCookie` 로 안쪽을 고쳐도 바깥이 남으면 효과가 없다.

```kotlin
// 현재 — clearAllCookies 가 refreshToken?.let 안에 있다
fun signOut(request, response) {
    val refreshToken = jwtProvider.getJwtFromRequest(jwtProperties.refreshToken.name, request)
    refreshToken?.let {
        refreshTokenRepository.findByToken(it)?.let { token -> refreshTokenRepository.delete(token) }
        clearAllCookies(request, response)      // ← 리프레시 쿠키가 없으면 아예 호출되지 않는다
    }
}
```

리프레시 쿠키는 path 가 `"${contextPath}/"` 라 액세스 토큰 쿠키(`contextPath`)와 경로가 다르다.
**브라우저가 리프레시 쿠키만 안 실어 보내는 상황이 실제로 가능하고, 그때 액세스·expiry 쿠키가 그대로 남는다.**

```kotlin
    fun signOut(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        // Redis 레코드 삭제만 리프레시 토큰 존재에 의존한다.
        jwtProvider.getJwtFromRequest(jwtProperties.refreshToken.name, request)?.let { token ->
            refreshTokenRepository.findByToken(token)?.let { refreshTokenRepository.delete(it) }
        }

        // 쿠키 만료는 조건 없이 항상 수행한다. 지울 쿠키가 없으면 브라우저가 무시할 뿐이고,
        // 남아 있으면 이것이 유일한 제거 수단이다.
        clearAllCookies(request, response)
    }
```

> **safers-api 도 같은 구조다.** 즉 이 결함은 "safers 에 맞춰 정렬" 로는 고쳐지지 않는다.
> 정렬 범위를 넘는 수정이며, safers 쪽에도 동일 수정을 제안할 대상이다(§9-8).

`Duration` 전환에 따른 호출부:

```kotlin
-       refreshTokenRepository.save(RefreshToken(user.username, newRefreshToken, jwtProperties.refreshToken.expiration.toInt()))
+       refreshTokenRepository.save(
+           RefreshToken(user.username, newRefreshToken, jwtProperties.refreshToken.expiration.toSeconds()),
+       )
```

```kotlin
-       val expiryTimeMillis = System.currentTimeMillis() + (jwtProperties.refreshToken.expiration * 1000L)
+       val expiryTimeMillis = System.currentTimeMillis() + jwtProperties.refreshToken.expiration.toMillis()
```

`createAuthCookie(... expiry: Duration ...)` 로 시그니처를 바꾸고 `.maxAge(expiry)` 를 쓴다
(`ResponseCookie.maxAge(Duration)` 오버로드).

## 5. 배포 영향 — 반드시 읽는다

### 5.1 기존 발급 토큰은 무효화되지 않는다

**이번 작업 어느 것도 이미 발급된 토큰을 죽이지 않는다.** 오해하기 쉬운 지점이라 먼저 못 박는다.

| 변경 | 기존 토큰에 미치는 영향 |
|---|---|
| jjwt → nimbus 교체 | **없음.** 서명 알고리즘(HS256)과 키 바이트가 동일해 그대로 검증된다(§4.2) |
| 수명 설정 정정 (`Duration`) | **없음.** `exp` 는 발급 시점에 토큰 안에 박히므로, 설정을 바꿔도 이미 나간 토큰의 `exp` 는 변하지 않는다 |

| | 현재 발급된 토큰 | 정정 배포 후 |
|---|---|---|
| 액세스 토큰 `exp` | 발급 시점 + 416일 | 서명·`exp` 모두 유효 → **계속 통과한다** |
| 리프레시 토큰 `exp` | 발급 시점 + 27년 | **계속 통과한다** |
| 신규 발급 토큰 | — | 10시간 / 10일 |

**즉 수명 정정은 "앞으로 나갈 토큰" 에만 적용된다.** 이미 나가 있는 416일짜리 액세스 토큰은
정정 배포 후에도 최대 416일간 그대로 살아 있다.

### 5.2 방침 — secret 은 그대로 두고 신규 토큰에만 적용한다

**결정: 수명 정정만 배포한다. secret 교체는 이번 범위에 넣지 않는다.**
사용자 영향이 0 이고, 리팩터링과 묶으면 회귀 원인이 섞이기 때문이다.

| | 이번 배포 |
|---|---|
| 신규 발급 토큰 | 10시간 / 10일 |
| 이미 발급된 토큰 | 그대로 유효 — 액세스 최대 416일, 리프레시 최대 27년 |
| 사용자 영향 | 없음 (재로그인 불필요) |

**받아들이는 위험을 명시한다.** 이 방침은 **이미 유출됐을 수 있는 416일짜리 토큰을 회수하지 못한다.**
액세스 토큰에 무효화 목록이 없어(§2.3) 로그아웃으로도 끊을 수 없다. §7-1 에 한계로 기록하고
회수 수단은 §9-2 후속 과제로 넘긴다.

> **회수가 필요해지면 secret 을 교체해야 하는데, 그때 주의할 점을 미리 적어둔다.**
> aiot 는 액세스/리프레시가 **별도 secret** 을 쓴다(`JwtProvider.getSecretKeyBytes(isRefreshToken)`).
> **액세스 secret 만 교체하면 아무것도 무효화되지 않는다** — 기존 리프레시 토큰(27년)이 그대로
> 검증되고 `POST /auth/refresh-token` 이 **새 키로 서명된 액세스 토큰을 발급**해 세션이 복구된다.
> 반드시 **두 secret 을 함께 교체**하거나, 액세스 secret 교체 + Redis 레코드 전량 삭제를 병행한다.
> 새 secret 은 32바이트 이상이어야 한다(§4.2).

### 5.3 Redis 잔여 데이터 — 조치 불필요

기존 `refresh_token` 레코드의 TTL 이 27년으로 박혀 있고 `@TimeToLive` 는 저장 시점에 적용되므로
**배포만으로는 줄어들지 않는다.** 그러나 **손댈 필요가 없다.**

```kotlin
@RedisHash("refresh_token")
data class RefreshToken(
    @Id val username: String,   // ← 사용자당 1건. 로그인마다 upsert 된다
```

- **자가 치유된다.** `@Id` 가 username 이라 다음 로그인에서 같은 키를 덮어쓰고, 그때 TTL 이 10일로 갱신된다.
- **무한 증가하지 않는다.** 레코드 수는 사용자 수로 한정된다.
- **지우면 손해다.** §5.2 방침에서는 기존 리프레시 토큰이 여전히 유효하므로 레코드는 고아가 아니다.
  전량 삭제하면 보안 이득 없이 사용자 리프레시만 끊긴다.

> secret 교체(§5.2 각주)를 수행할 때는 레코드가 고아가 되므로 그때 함께 정리한다.

### 5.4 403 → 401 변경의 클라이언트 영향

**프론트엔드가 403 을 보고 재로그인 유도를 하고 있다면 깨진다.** 배포 전 확인 대상이다.
일반적으로는 401 을 인터셉터에서 잡아 `/auth/refresh-token` 을 호출하는 구조이므로 **개선**이지만,
현재 403 이 나가고 있었다는 사실을 프론트가 이미 우회하고 있을 수 있다.

## 6. 테스트 관점

### 6.1 착수 전

```bash
./gradlew test    # 전량 통과 확인 (2026-08-25 통과 확인됨)
```

`@SpringBootTest` 3개(`FireAlarmProcessorTest`, `TemperatureHumidityProcessorTest`,
`DisplacementGaugeProcessorTest`)가 H2 로 전체 컨텍스트를 띄운다.
`CommonSecurityConfig` 도 이 컨텍스트에 올라가므로 **§4.6 의 빈 구성 변경이 이 3개를 깨뜨릴 수 있다.**
`CorsProperties` 를 `@EnableConfigurationProperties` 에 넣지 않으면 컨텍스트 로딩이 실패한다.

```sql
-- §4.6 의 전제. 값이 'ADMIN' 이 아니면 #13 을 범위에서 뺀다.
SELECT DISTINCT r.auth FROM roles r JOIN user_roles ur ON ur.role_id = r.id;
```

### 6.2 신규 테스트

**`JwtProviderTest` — nimbus 전환의 핵심 회귀 방지.**

```kotlin
class JwtProviderTest : BehaviorSpec({
    given("만료 시각이 지난 액세스 토큰") {
        `when`("extractUsername 을 호출하면") {
            then("EXPIRED_ACCESS_TOKEN 으로 실패한다") {
                // nimbus 는 만료를 자동 검사하지 않는다. 이 테스트가 설계문서 §4.2 의 방어선이다.
                val provider = JwtProvider(refreshTokenRepository, expiredProperties)
                val token = provider.generateAccessToken("tester")
                val ex = shouldThrow<CustomException> { provider.extractUsername(token) }
                ex.errorCode shouldBe ErrorCode.EXPIRED_ACCESS_TOKEN
            }
        }
    }

    given("서명이 위조된 토큰") {
        `when`("extractUsername 을 호출하면") {
            then("INVALID_ACCESS_TOKEN 으로 실패한다") { /* 마지막 세그먼트 변조 */ }
        }
    }

    given("리프레시 secret 으로 서명한 토큰") {
        `when`("액세스 토큰으로 검증하면") {
            then("INVALID_ACCESS_TOKEN 으로 실패한다") { /* 키 분리 확인 */ }
        }
    }

    given("정상 액세스 토큰") {
        `when`("extractUsername 을 호출하면") {
            then("subject 를 반환한다") { }
        }
    }
})
```

> `expiredProperties` 는 `Duration.ofSeconds(-1)` 같은 음수 만료로 만든다.
> `Thread.sleep` 으로 기다리는 테스트는 쓰지 않는다.

**`JwtPropertiesTest` — §2 재발 방지. 이게 이번 문서의 핵심 회귀 테스트다.**

```kotlin
// 설정값 단위가 어긋나면 아무 오류 없이 토큰 수명만 1000배가 된다(설계문서 §2).
// 값 자체를 테스트로 고정한다.
given("application-common.yml 의 jwt 설정") {
    then("액세스 토큰 수명은 10시간이다") {
        jwtProperties.accessToken.expiration shouldBe Duration.ofHours(10)
    }
    then("리프레시 토큰 수명은 10일이다") {
        jwtProperties.refreshToken.expiration shouldBe Duration.ofDays(10)
    }
}
```

**`WhiteListPathTest` — 경로 경계 매칭.**

```kotlin
WhiteListPath.matches("/health") shouldBe true
WhiteListPath.matches("/health/live") shouldBe true
WhiteListPath.matches("/swagger-ui.html") shouldBe true
WhiteListPath.matches("/health-actions") shouldBe false   // ← 현재 코드가 true 를 반환하는 케이스
WhiteListPath.matches("/actuators") shouldBe false
WhiteListPath.matches("/auth/sign-in") shouldBe true
WhiteListPath.matches("/auth/sign-out") shouldBe true      // ← 없으면 로그아웃이 막힌다(§4.7)
```

### 6.3 수동 확인

| 항목 | 확인 방법 |
|---|---|
| 미인증 → 401 | `curl -i -X POST <보호된 엔드포인트>` — 403 이 아니라 401 |
| `GET /users/me` 익명 | 쿠키 없이 호출 → **401**(현재는 404) |
| 로그인/로그아웃 왕복 | `Set-Cookie` 의 `Max-Age` 가 `36000`(10h) 인지 확인. **현재는 36000000** |
| 기존 토큰 계속 동작 | **배포 전 발급한 쿠키로 요청 → 여전히 통과해야 한다.** §5.2 방침의 전제다 |
| 로그아웃 후 쿠키 제거 | 응답에 `Max-Age=0` `Set-Cookie` 3개(Access/Refresh/expiry) |
| **리프레시 쿠키 없이 로그아웃** | `AccessToken` 쿠키만 들고 `/auth/sign-out` 호출 → **여전히 `Set-Cookie` 3개가 나와야 한다.** 현재는 0개(§4.8) |
| **만료 토큰으로 로그아웃** | 만료된 `AccessToken` + 유효한 `RefreshToken` 으로 `/auth/sign-out` → **204 와 `Set-Cookie` 3개.** 401 이 나오면 `AUTH_OUT` 이 빠진 것(§4.7) |
| 리프레시 | `/auth/refresh-token` 200 + 새 쿠키 |
| `/admin/**` | ADMIN 역할 없는 사용자로 403, 있는 사용자로 200 |
| 기존 토큰 호환 | **배포 전 발급한 쿠키로 요청** → 여전히 통과해야 한다(§5.1) |

### 6.4 테스트로 못 잡는 것

- **§5.4 프론트엔드의 403 의존** — 클라이언트 코드를 봐야 한다
- **§4.6 의 `roles.auth` 값** — 운영 DB 확인이 유일한 방법

## 7. 남는 한계 (인지 사항)

1. **액세스 토큰 무효화 수단이 여전히 없다.** 로그아웃해도 `exp` 까지 유효하다(§2.3).
   수명 정정은 **신규 발급분에만** 적용되므로(§5.2), **이미 나가 있는 416일짜리 토큰은
   배포 후에도 최대 416일간 그대로 살아 있다.** 신규 토큰의 노출 창만 10시간으로 줄어든다.
   회수하려면 secret 교체가 필요하고(§5.2 각주), 근본 해결은 denylist(Redis)나
   짧은 액세스 토큰 + 잦은 갱신이다 — §9-2.
2. **secret 이 yml 평문이고 safers-api 와 동일하다.** 이번 범위에서 고치지 않는다 — §9.
3. **`secure(false)` 쿠키.** HTTPS 배포에서도 `Secure` 플래그가 없다. safers 도 동일하다 — §9.
4. **`hasRole("ADMIN")` 은 `roles.auth` 문자열에 의존한다.** DB 값이 바뀌면 조용히 인가가 풀리거나 잠긴다.
   §6.2 의 테스트가 커버하지 못하는 영역이다.
5. **Jackson 2 는 클래스패스에 남는다.** jjwt-jackson 을 지워도 다른 경로로 들어온다.
   DTO 애노테이션은 Jackson 3 이 읽으므로 문제없다(검증 ⑦).
6. **JWT 설정이 운영/테스트 두 yml 에 복제돼 있다.** 한쪽만 고쳐도 아무 오류가 나지 않는다(§4.5).
   근본 해결은 테스트 yml 이 운영 yml 을 상속하도록 바꾸는 것인데, 이번 범위에서 다루지 않는다 — §9-6.

## 8. 작업 순서 / 커밋 단위

각 커밋에서 `./gradlew build` 가 통과해야 한다.

| # | 커밋 | 내용 | 위험도 |
|---|---|---|---|
| 1 | `fix: 화이트리스트 경로 경계 매칭으로 수정` | §4.7 + 테스트 | 낮음 — **단독 가치 있음** |
| 2 | `fix: 미인증 요청에 401 응답 (RestAuthenticationEntryPoint 추가)` | §4.4 + `ErrorCode.UNAUTHENTICATED` | 중 — §5.4 |
| 3 | `fix: GET /users/me 인가 순서 수정` | §4.6 인가 규칙 중 `/users/me/**` | 낮음 |
| 4 | `refactor: jjwt 를 nimbus-jose-jwt 로 교체` | §4.1, §4.2 + `JwtProviderTest` | **높음** — 만료 검증 |
| 5 | `fix: 토큰 수명 설정 단위를 Duration 으로 정정` | §4.5(**운영 yml + 테스트 yml 둘 다**), §4.8 호출부 + `JwtPropertiesTest` | **최고** — §5 |
| 6 | `refactor: 인증 쿠키 삭제를 ResponseCookie 기반으로 변경` | §4.8 쿠키 부분 | 중 |
| 7 | `refactor: AuthenticationService 트랜잭션 경계 정리` | §4.8 트랜잭션 | 낮음 |
| 8 | `refactor: JwtAuthenticationFilter Jackson 3 전환 및 예외 로깅 추가` | §4.3 | 낮음 |
| 9 | `feat: CORS 추가 오리진을 설정으로 분리` | §4.6 CORS + `CorsProperties` | 낮음 |
| 10 | `feat: /admin/** 에 ADMIN 역할 인가 적용` | §4.6 — **§6.1 DB 확인 통과 시에만** | **높음** |

**1~3 은 나머지와 독립적이고 명백한 결함 수정이라 먼저 내보낼 수 있다.**
**5 는 §5 를 읽고 배포 시점을 정한 뒤 진행한다.**
**10 은 DB 확인 결과에 따라 이번 범위에서 뺀다.**

가상 스레드 작업(`2026-08-25-virtual-threads-design.md` §10)과는 **PR 을 분리한다.**
겹치는 파일은 `build.gradle.kts` 뿐이고, 그쪽 커밋 2번(의존성 정렬)과 여기 커밋 4번이 같은 파일을 만진다 —
**어느 한쪽을 먼저 머지하고 리베이스한다.**

## 9. 후속 과제

| # | 과제 | 조건 |
|---|---|---|
| 1 | JWT secret 을 yml 평문에서 환경변수/시크릿 매니저로 분리 | safers-api 와 동일 값이라는 점도 함께 해소. **분리 시 두 secret 을 함께 교체**하면 기존 토큰 회수까지 동시에 달성된다(§5.2 각주) |
| 2 | 액세스 토큰 무효화(denylist 또는 짧은 수명 + 자동 갱신) | §7-1. 수명 정정으로 급한 불은 끈 뒤 |
| 3 | 운영 환경 쿠키 `Secure` 플래그 | 프로필별 분기 필요. §7-3 |
| 4 | STOMP 구독 인가 (`StompSubscribeAuthorizationInterceptor`) | safers 참조. aiot 는 `MyDefaultHandshakeHandler` 만 있다 |
| 5 | 인증 계층을 공용 모듈로 추출 | 두 프로젝트가 같은 코드를 복제 중. 멀티모듈화 시점에 |
| 6 | 테스트 yml 의 설정 복제 제거 | `src/test/resources/application.yml` 이 운영 설정을 통째로 복제 중이다. 프로파일 상속이나 `@DynamicPropertySource` 로 정리. §7-6 |
| 7 | safers-api 에 `signOut` 가드 · `sign-out` 화이트리스트 수정 제안 | §4.8 · §4.7 — 두 결함 모두 그쪽에도 있다 |

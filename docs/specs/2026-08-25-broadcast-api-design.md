# 스피커(TTS) · 전광판(LED) 송출 API 스펙

- 작성일: 2026-08-25
- 대상 이슈: #22 #23 #24 #25 #26 #27 #28
- 대상 코드: `speaker/**`, `display/**`, `broadcast/**`
- 상태: **계약 확정용 스텁**. Controller · DTO 는 확정, Service 는 비어 있다.

## 0. 왜 스텁부터인가

프론트(`plug-platform-atlas · apps/a-iot`)가 화면 작업을 시작하려면 요청/응답 계약이 먼저 필요하다.
반면 **하드웨어 업체 API 스펙이 아직 오지 않아** 송출 구현과 표출/음성 옵션 필드를 확정할 수 없다.

그래서 이번 PR 은 **Swagger 에 노출되는 계약만 확정**한다. Controller 와 DTO 는 실구현에서 그대로 쓴다.

Service 는 **가짜 데이터를 돌려주지 않는다.** 목록은 빈 응답, 단건 조회는 404, 쓰기는 no-op 이다.
필드별 예시 값은 DTO 의 `@Schema(example = ...)` 로만 제공하며 프론트는 Swagger UI 의 스키마·예시를 보고 화면을 만든다.
스텁 응답을 실데이터로 오해할 여지를 남기지 않기 위한 선택이다.

## 1. 이번 범위

이슈에 적힌 기능 중 프론트가 당장 필요한 것만 잘랐다.

| 포함 | 제외 |
|---|---|
| 스피커/전광판 **목록 조회** | 장치 등록 · 수정 · 삭제 (#24) |
| 스피커/전광판 **송출** (#26 #28) | 표출/음성 옵션 필드 (업체 스펙 미수령) |
| **송출 이력** 목록 (#26 #28) | 예약 송출 (이슈에서 이미 제외) |
| 프리셋 **CRUD 5종** (#25 #27) | |

## 2. 엔드포인트 16개

### 스피커 (TTS)

| Method | Path | 응답 | 설명 |
|---|---|---|---|
| GET | `/speakers` | `DataResponseBody<List<SpeakerResponse>>` | `siteId` 필터. 비페이징 |
| POST | `/speakers/broadcasts` | `204` | 프리셋 또는 직접 입력 송출 |
| GET | `/speakers/broadcasts` | `DataResponseBody<PageResponse<SpeakerBroadcastResponse>>` | `page` `size` `from` `to` `userId` `siteId` |
| GET | `/speaker-presets` | `DataResponseBody<PageResponse<SpeakerPresetResponse>>` | `page` `size` `title` |
| GET | `/speaker-presets/{presetId}` | `DataResponseBody<SpeakerPresetResponse>` | |
| POST | `/speaker-presets` | `201` + `Long` | `@ResponseCreated` |
| PUT | `/speaker-presets/{presetId}` | `204` | |
| DELETE | `/speaker-presets/{presetId}` | `204` | |

Swagger Tag 는 도메인별로 「장치 · 송출 · 이력」 과 「프리셋」 둘로 나뉜다.
프론트 송출 화면이 장치 목록과 송출을 함께 쓰므로 한 그룹에 두는 편이 찾기 쉽다.

### 전광판 (LED)

`/displays`, `/displays/broadcasts`, `/display-presets/**` — 스피커와 **완전히 동일한 형태**다.
`speakerIds` 자리에 `displayIds` 가 들어가는 것만 다르다.

## 3. 스키마

```kotlin
// 스피커/전광판 공통 형태
SpeakerResponse(
    id: Long,
    name: String,           // 명칭
    deviceId: String,       // 업체 장비 식별자 — 송출 시 업체 API 로 전달
    location: String?,      // 설치 위치
    status: DeviceStatus,   // NORMAL | OFFLINE | UNKNOWN
    site: SiteResponse?,
)

SpeakerBroadcastRequest(
    presetId: Long?,          // presetId ⊕ message — 정확히 하나
    message: String?,         // 최대 1000자
    speakerIds: List<Long>?,  // speakerIds ∪ siteIds — 하나 이상
    siteIds: List<Long>?,     // 해당 현장의 모든 장치로 송출
)

// 이력 1건 = 장치 1건. 요청 한 번이 스피커 N개를 대상으로 하면 이력 N건이 남는다
SpeakerBroadcastResponse(
    id: Long, message: String,
    presetId: Long?, presetTitle: String?,     // 직접 입력이면 둘 다 null
    speakerId: Long, speakerName: String,
    siteId: Long?, siteName: String?,
    userId: String, userName: String,          // 계정 아이디 / 성명
    broadcastAt: String,
    success: Boolean, failureReason: String?,  // 성공 시 사유는 null
)

SpeakerPresetRequest(
    title: String,    // @NotBlank, 최대 50자
    message: String,  // @NotBlank, 최대 1000자
)

SpeakerPresetResponse(id, title, message, @JsonUnwrapped BaseResponse)
```

## 4. 결정과 근거

| 결정 | 근거 |
|---|---|
| **이력 1건 = 장치 1건** | 업체 API 가 장치 단건 처리다 — 한 대에 보내고 결과를 받는 형태. 요청 단위로 묶어 집계 필드를 두면 실제 호출 단위와 어긋난다. 장치 단위로 남기면 실패한 장치만 재송출하기도 쉽다 |
| 이력에 **집계 카운트를 두지 않음** | 위와 같은 이유. 건수가 필요하면 목록의 `totalElements` 로 충분하고, 성공/실패 집계는 필터로 얻는 편이 정확하다 |
| 이력 **상세 조회 없음** | 이력이 장치 단위라 목록 항목 하나가 이미 완결된 정보(대상 · 성공 여부 · 실패 사유)를 담는다. 상세로 더 보여줄 것이 없다 |
| 송출 응답을 **`204`** 로. 결과는 이력에서 확인 | 기존 `AnnouncementController.broadcast` 와 같은 형태. 송출은 요청 접수이고 장치별 결과는 이력이 단일 출처가 된다 |
| 이력 경로를 **`GET /speakers/broadcasts`** 로 (송출과 같은 경로) | 같은 컬렉션에 `POST` 는 송출, `GET` 은 이력. 프론트 입장에서 짝이 명확하다 |
| 프리셋을 `/speakers/presets` 가 아니라 **`/speaker-presets`** 로 분리 | 후속에 `GET /speakers/{id}` 가 붙을 때 경로 변수와 섞이지 않는다 |
| 프리셋 저장소를 **스피커/전광판 분리** | 업체 스펙 확정 후 붙일 옵션 필드(음색·볼륨 vs 스크롤·색상)가 서로 겹치지 않는다. 공유 테이블로 두면 nullable 범벅이 된다 |
| 송출 대상을 **장치 ID + 현장 ID 둘 다** 허용 | 개별 선택(#26)과 현장 일괄(#28)이 둘 다 요구사항이다. 현장 일괄을 프론트가 목록 조회 후 전체 선택으로 흉내내면 장치가 늘 때마다 요청이 커진다 |
| 장치 목록 조회는 **비페이징** | 기존 `CctvController.getAll` 과 같은 형태. 한 현장의 장치 수가 페이징이 필요할 규모가 아니다 |
| 표출/음성 옵션 **이번 범위 제외** | 업체 스펙 미수령. 추측으로 필드를 만들면 프론트가 그 UI 를 만든 뒤 다시 뜯어야 한다 |
| `DeviceStatus` 를 **스피커/전광판 공용** | 값이 같고 프론트가 타입을 하나만 쓰면 된다. 업체별로 갈리면 그때 분리한다 |

## 5. 미확정 지점

| 항목 | 확정 조건 |
|---|---|
| 업체 송출 API 호출 | 하드웨어 업체 스펙 수령. `SpeakerBroadcastService.broadcast` / `DisplayBroadcastService.broadcast` 의 `TODO` 지점 |
| 다중 장치 송출의 처리 방식 | 업체가 단건 처리라 N개 대상이면 N번 호출한다. 순차/병렬 여부와 일부 실패 시 나머지 진행 정책은 스펙 수령 후 결정 |
| 표출/음성 옵션 필드 | 업체 스펙 수령 후 `SpeakerPresetRequest` 에 `options` 중첩 객체로 추가 (기존 필드는 그대로 두는 additive 변경) |
| `DeviceStatus` 조회 방식 | 업체 API 가 상태를 주는지에 따라. 안 주면 `UNKNOWN` 고정 |
| 송출 성공/실패 판정 시점 | 업체 API 가 동기 응답을 주면 이력 적재 시점에 확정. 비동기면 이력에 `PENDING` 상태 필드가 추가된다 |
| 장치 등록/수정/삭제 (#24) | 프론트 관리 화면 착수 시점 |

## 6. 후속 작업

1. 엔티티 · 리포지토리 구현 — `Speaker`, `Display`, `SpeakerPreset`, `DisplayPreset`, 송출 이력 (`ddl-auto: update` 라 마이그레이션 파일 불필요)
2. 프리셋 CRUD 실구현 → `SpeakerPresetService` / `DisplayPresetService` 스텁 교체
3. 송출 이력 적재 + 조회 실구현, 업체 API 클라이언트 인터페이스 분리
4. 업체 스펙 수령 후 옵션 필드와 실제 호출 채우기

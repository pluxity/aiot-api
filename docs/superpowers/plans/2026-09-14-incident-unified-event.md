# 이벤트 통합 관리(incident) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 센서(EventHistory)·CCTV(EdsEvent)·AI마이크(MicEvent) 이벤트를 `incident` 테이블 하나로 모아 `/events` API에서 공통 조치 워크플로우(미조치/조치중/조치완료, 조치 이력)를 제공한다.

**Architecture:** 원본 세 테이블은 유지하고 `incident`가 (source_type, source_id)로 원본을 가리킨다. 원본 이벤트가 생성될 때 같은 트랜잭션에서 `IncidentService.open`으로 incident를 무조건 생성한다. 조치 상태·조치 이력·목록·대시보드·통계는 모두 incident 기준으로 동작하고, 센서 상세 값은 목록 쿼리에서 EventHistory를 left join 해 채운다.

**Tech Stack:** Kotlin, Spring Boot, JPA/Hibernate(ddl-auto update), Kotlin JDSL, PostgreSQL, Kotest + MockK

**Spec:** 메모리 `incident-unified-event-design` (2026-09-14 확정). CCTV·MIC는 level WARNING 고정, 정책 없이 전부 incident 생성, EDS ENDED 시 자동 조치완료 없음, API 경로 `/events` 유지.

## Global Constraints

- API 경로는 `/events`를 유지한다. 응답 필드는 기존 이름을 유지하고 `sourceType`, `title`을 추가한다.
- 센서 외 이벤트의 level은 `ConditionLevel.WARNING` 고정.
- incident는 원본 이벤트 생성과 같은 트랜잭션에서 동기 생성한다.
- 주석은 지우면 깨지는 함정만 남긴다.
- 운영 DB는 ddl-auto update이므로 컬럼 제거·백필은 `docs/migration/incident.sql`로 배포 전에 수동 실행한다.

---

### Task 1: Incident 엔티티·저장소·서비스

**Files:**
- Create: `src/main/kotlin/com/pluxity/aiot/incident/Incident.kt`
- Create: `src/main/kotlin/com/pluxity/aiot/incident/IncidentSourceType.kt`
- Create: `src/main/kotlin/com/pluxity/aiot/incident/IncidentRepository.kt`
- Create: `src/main/kotlin/com/pluxity/aiot/incident/IncidentService.kt`
- Test: `src/test/kotlin/com/pluxity/aiot/incident/IncidentServiceKoTest.kt`

**Interfaces:**
- Produces: `IncidentService.open(sourceType, sourceId, site, deviceId, deviceName, title, level, occurredAt, latitude, longitude, guideMessage = null): Incident`
- Produces: `Incident.changeStatus(EventStatus)`, `Incident.status`, `Incident.site`
- site가 null이고 좌표가 있으면 `siteRepository.findFirstByPointInPolygon`으로 보완한다.

- [ ] 엔티티 작성 (unique(source_type, source_id), index(status,id), index(site_id, occurred_at))
- [ ] IncidentServiceKoTest: site 보완, 좌표 없음, 저장값 검증
- [ ] 커밋 `feat: incident 엔티티와 생성 서비스 추가`

### Task 2: 센서 이벤트 → incident 생성, 알림 ID를 incident ID로

**Files:**
- Modify: `data/subscription/processor/SensorDataProcessor.kt` (processEvent, processEventConditions에 `incidentService` 파라미터 추가)
- Modify: `data/subscription/processor/impl/*Processor.kt` 7개 (생성자 주입 + 전달)
- Modify: `event/entity/EventHistory.kt` (`status` 제거)
- Test: `data/subscription/processor/ProcessorTestHelper.kt`, `impl/*TestHelper.kt`, `impl/*Test.kt` (status 검증을 incident 검증으로)

**Interfaces:**
- SensorAlarmPayload.eventId, SensorEventNotified.eventId = incident.requiredId
- Incident: deviceId=deviceId, deviceName=sensorType.description, title=fieldDescription, level=trigger.level, site=feature.site

- [ ] processEvent에서 EventHistory 저장 직후 `incidentService.open(SENSOR, eventHistory.requiredId, ...)`
- [ ] 프로세서 테스트: `incidentRepository.findBySourceTypeAndSourceId` 로 검증
- [ ] 커밋 `feat: 센서 이벤트 발생 시 incident 생성`

### Task 3: /events 조회·상태 변경·통계·대시보드를 incident 기준으로

**Files:**
- Create: `incident/IncidentRow.kt` (JDSL projection) — 또는 `event/dto/EventResponse.kt` 내 `EventHistoryRow` → `IncidentRow` 교체
- Create: `incident/IncidentCustomRepository.kt`, `incident/IncidentCustomRepositoryImpl.kt`
- Modify: `event/EventService.kt`, `event/EventController.kt` (sourceType 필터), `event/EventStatusChangeNotifier.kt`, `dashboard/DashboardService.kt`, `global/messaging/dto/MessagePayload.kt`
- Delete: `event/repository/EventHistoryRepositoryCustom.kt`, `impl/EventHistoryRepositoryCustomImpl.kt`
- Test: `event/EventServiceKoTest.kt`, `dashboard/DashboardServiceKoTest.kt`, `action/entity/DummyEntities.kt`

**Interfaces:**
- `IncidentCustomRepository.findEventList(from, to, siteId, status): List<IncidentRow>`
- `IncidentCustomRepository.findEventListWithPaging(from, to, siteId, status, level, sensorType, sourceType, size, lastId, lastStatus): List<IncidentRow>`
- `IncidentRow.toEventResponse()`; EventResponse에 `sourceType: String`, `title: String` 추가, `objectId/fieldKey/value/updatedBy` nullable
- 목록 쿼리: `Incident` left join `EventHistory` on (sourceType = SENSOR and sourceId = EventHistory.id), left join `Incident::site`
- 커서 정렬 `(status asc, id desc)` 유지
- time-series SQL: `event_history` → `incident`
- `EventStatusChangeNotifier.notifyStatusChanged(incident: Incident)`; site는 incident.site, 없으면 좌표 폴리곤

- [ ] 커밋 `feat: /events 목록·상태·통계·대시보드를 incident 기준으로 전환`

### Task 4: ActionHistory FK를 incident로

**Files:**
- Modify: `action/ActionHistory.kt` (`incident: Incident`, join column `incident_id`), `action/ActionHistoryRepository.kt` (`findByIncident`, `findByIdAndIncident`), `action/ActionHistoryService.kt`
- Test: `action/ActionHistoryServiceKoTest.kt`, `action/entity/DummyEntities.kt`

- [ ] 조치 등록 시 `incident.changeStatus(RESOLVED)` + notifier
- [ ] 커밋 `feat: 조치 이력을 incident에 연결`

### Task 5: CCTV·MIC 이벤트 → incident 생성

**Files:**
- Modify: `eds/EdsEventService.kt` (신규 생성 분기에서 open), `cctv/repository/CctvRepository.kt` (`findByEdsCameraId`)
- Modify: `mic/MicEventService.kt`, `mic/MicRepository.kt` (`findByVendorMicId`)
- Test: `mic/MicEventServiceKoTest.kt`, 신규 `eds/EdsEventServiceKoTest.kt`

**Interfaces:**
- CCTV: deviceId=cameraId, deviceName=cctv?.name ?: cameraId, title=eventType?.description ?: profileName, occurredAt=eventStart("yyyy/MM/dd HH:mm:ss.SSS") 파싱 실패 시 now, 좌표는 payload → cctv 순
- MIC: deviceId=micId, deviceName=micName, title=labelNameKo ?: labelNameEn ?: "소음 감지", occurredAt=micEvent.occurredAt
- EdsEvent 갱신(진행중/종료) 경로에서는 incident를 건드리지 않는다

- [ ] 커밋 `feat: CCTV·AI마이크 이벤트 발생 시 incident 생성`

### Task 6: 수동 마이그레이션 SQL

**Files:**
- Create: `docs/migration/incident.sql`

내용: incident 테이블 생성(Hibernate 스키마와 동일) → event_history에서 백필(feature 조인으로 site_id) → action_history.incident_id 추가·백필·NOT NULL → event_history_id, event_history.status·인덱스 제거.

- [ ] 커밋 `docs: incident 전환 수동 마이그레이션 SQL`

### Task 7: 전체 테스트·spotless·PR

- [ ] `./gradlew spotlessApply test`
- [ ] PR → develop, squash 머지 여부는 사용자 확인

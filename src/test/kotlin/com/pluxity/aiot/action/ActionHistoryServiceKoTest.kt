package com.pluxity.aiot.action

import com.pluxity.aiot.action.entity.dummyActionHistory
import com.pluxity.aiot.action.entity.dummyEventHistory
import com.pluxity.aiot.event.EventStatusChangeNotifier
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.file.service.FileService
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class ActionHistoryServiceKoTest :
    BehaviorSpec({

        val actionHistoryRepository: ActionHistoryRepository = mockk()
        val eventHistoryRepository: EventHistoryRepository = mockk()
        val actionHistoryFileRepository: ActionHistoryFileRepository = mockk()
        val fileService: FileService = mockk(relaxed = true)
        val eventStatusChangeNotifier: EventStatusChangeNotifier = mockk(relaxed = true)

        val actionHistoryService =
            ActionHistoryService(
                actionHistoryRepository,
                eventHistoryRepository,
                actionHistoryFileRepository,
                fileService,
                eventStatusChangeNotifier,
            )

        Given("조치를 등록할 때") {
            When("유효한 요청으로 조치 등록 요청") {
                val id = 10L
                val request = ActionHistoryRequest("조치")
                val eventHistory = dummyEventHistory()
                every {
                    eventHistoryRepository.findByIdOrNull(any())
                } returns eventHistory

                every {
                    actionHistoryRepository.save(any())
                } returns dummyActionHistory(id = id, eventHistory = eventHistory)

                val saveId = actionHistoryService.save(1L, request)

                Then("성공") {
                    saveId shouldBe id
                }
            }

            When("파일과 함께 조치 등록 요청") {
                val id = 10L
                val fileIds = listOf(1L, 2L)
                val request = ActionHistoryRequest("조치", fileIds)
                val eventHistory = dummyEventHistory()
                val savedActionHistory = dummyActionHistory(id = id, eventHistory = eventHistory)

                every {
                    eventHistoryRepository.findByIdOrNull(any())
                } returns eventHistory

                every {
                    actionHistoryRepository.save(any())
                } returns savedActionHistory

                val filesSlot = slot<List<ActionHistoryFile>>()
                every {
                    actionHistoryFileRepository.saveAll(capture(filesSlot))
                } returns emptyList()

                val saveId = actionHistoryService.save(1L, request)

                Then("파일 업로드 finalize 및 연관 엔티티들을 저장") {
                    saveId shouldBe id

                    fileIds.forEach { fileId ->
                        verify(exactly = 1) {
                            fileService.finalizeUpload(fileId, "action-histories/$id/")
                        }
                    }
                    verify(exactly = 1) {
                        actionHistoryFileRepository.saveAll(any<List<ActionHistoryFile>>())
                    }

                    filesSlot.captured.map { it.fileId } shouldBe fileIds
                    filesSlot.captured.map { it.actionHistory } shouldBe listOf(savedActionHistory, savedActionHistory)
                }
            }

            When("잘못된 eventId로 등록 요청") {
                val eventId = 10L
                val request = ActionHistoryRequest("조치")

                every {
                    eventHistoryRepository.findByIdOrNull(any())
                } returns null

                val exception =
                    shouldThrowExactly<CustomException> {
                        actionHistoryService.save(eventId, request)
                    }

                Then("NOT_FOUND_EVENT_HISTORY 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_EVENT_HISTORY.getMessage().format(eventId)
                }
            }
        }

        Given("조치 목록을 조회할 때") {
            When("유효한 eventId로 조회 요청") {
                val eventId = 1L
                val eventHistory = dummyEventHistory(id = 1L)
                val actionHistories =
                    listOf(
                        dummyActionHistory(id = 1L, eventHistory = eventHistory),
                        dummyActionHistory(id = 2L, eventHistory = eventHistory),
                    )

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns eventHistory

                every {
                    actionHistoryRepository.findByEventHistory(eventHistory)
                } returns actionHistories

                val result = actionHistoryService.findAll(eventId)

                Then("조치 목록 반환") {
                    result.size shouldBe 2
                }
            }

            When("조치가 없는 eventId로 조회 요청") {
                val eventId = 1L
                val eventHistory = dummyEventHistory(id = eventId)

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns eventHistory

                every {
                    actionHistoryRepository.findByEventHistory(eventHistory)
                } returns emptyList()

                val result = actionHistoryService.findAll(eventId)

                Then("빈 목록 반환") {
                    result shouldBe emptyList()
                }
            }

            When("잘못된 eventId로 조회 요청") {
                val eventId = 999L

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns null

                val exception =
                    shouldThrowExactly<CustomException> {
                        actionHistoryService.findAll(eventId)
                    }

                Then("NOT_FOUND_EVENT_HISTORY 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_EVENT_HISTORY.getMessage().format(eventId)
                }
            }
        }

        Given("조치를 수정할 때") {
            When("유효한 요청으로 수정 요청") {
                val eventId = 1L
                val actionId = 10L
                val request = ActionHistoryRequest("수정된 조치")
                val eventHistory = dummyEventHistory(id = eventId)
                val actionHistory = dummyActionHistory(id = actionId, eventHistory = eventHistory)

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns eventHistory

                every {
                    actionHistoryRepository.findByIdAndEventHistory(actionId, eventHistory)
                } returns actionHistory

                actionHistoryService.update(eventId, actionId, request)

                Then("성공") {
                    actionHistory.content shouldBe "수정된 조치"
                }
            }

            When("파일과 함께 수정 요청") {
                val eventId = 1L
                val actionId = 10L
                val request = ActionHistoryRequest("수정된 조치", listOf(1L, 2L))
                val eventHistory = dummyEventHistory(id = eventId)
                val actionHistory = dummyActionHistory(id = actionId, eventHistory = eventHistory)

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns eventHistory

                every {
                    actionHistoryRepository.findByIdAndEventHistory(actionId, eventHistory)
                } returns actionHistory

                every {
                    actionHistoryFileRepository.deleteByIdIn(any())
                } just runs

                every {
                    actionHistoryFileRepository.saveAll(any<List<ActionHistoryFile>>())
                } returns emptyList()

                actionHistoryService.update(eventId, actionId, request)

                Then("성공") {
                    actionHistory.content shouldBe "수정된 조치"
                }
            }

            When("잘못된 eventId로 수정 요청") {
                val eventId = 999L
                val actionId = 10L
                val request = ActionHistoryRequest("수정된 조치")

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns null

                val exception =
                    shouldThrowExactly<CustomException> {
                        actionHistoryService.update(eventId, actionId, request)
                    }

                Then("NOT_FOUND_EVENT_HISTORY 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_EVENT_HISTORY.getMessage().format(eventId)
                }
            }

            When("잘못된 actionId로 수정 요청") {
                val eventId = 1L
                val actionId = 999L
                val request = ActionHistoryRequest("수정된 조치")
                val eventHistory = dummyEventHistory(id = eventId)

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns eventHistory

                every {
                    actionHistoryRepository.findByIdAndEventHistory(actionId, eventHistory)
                } returns null

                val exception =
                    shouldThrowExactly<CustomException> {
                        actionHistoryService.update(eventId, actionId, request)
                    }

                Then("NOT_FOUND_ACTION_HISTORY 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_ACTION_HISTORY.getMessage().format(actionId)
                }
            }
        }

        Given("조치를 삭제할 때") {
            When("유효한 요청으로 삭제 요청") {
                val eventId = 1L
                val actionId = 10L
                val eventHistory = dummyEventHistory(id = eventId)
                val actionHistory = dummyActionHistory(id = actionId, eventHistory = eventHistory)

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns eventHistory

                every {
                    actionHistoryRepository.findByIdAndEventHistory(actionId, eventHistory)
                } returns actionHistory

                every {
                    actionHistoryFileRepository.deleteAll(any())
                } returns Unit

                every {
                    actionHistoryRepository.delete(any())
                } returns Unit

                actionHistoryService.delete(eventId, actionId)

                Then("성공") {
                    verify(exactly = 1) { actionHistoryFileRepository.deleteAll(any()) }
                    verify(exactly = 1) { actionHistoryRepository.delete(any()) }
                }
            }

            When("잘못된 eventId로 삭제 요청") {
                val eventId = 999L
                val actionId = 10L

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns null

                val exception =
                    shouldThrowExactly<CustomException> {
                        actionHistoryService.delete(eventId, actionId)
                    }

                Then("NOT_FOUND_EVENT_HISTORY 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_EVENT_HISTORY.getMessage().format(eventId)
                }
            }

            When("잘못된 actionId로 삭제 요청") {
                val eventId = 1L
                val actionId = 999L
                val eventHistory = dummyEventHistory(id = eventId)

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns eventHistory

                every {
                    actionHistoryRepository.findByIdAndEventHistory(actionId, eventHistory)
                } returns null

                val exception =
                    shouldThrowExactly<CustomException> {
                        actionHistoryService.delete(eventId, actionId)
                    }

                Then("NOT_FOUND_ACTION_HISTORY 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_ACTION_HISTORY.getMessage().format(actionId)
                }
            }
        }
    })

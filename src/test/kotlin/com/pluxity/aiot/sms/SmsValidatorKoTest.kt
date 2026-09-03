package com.pluxity.aiot.sms

import com.pluxity.aiot.sms.dto.SmsSendRequest
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private fun request(
    title: String = "이벤트 알림",
    message: String = "화재 감지",
    targetNumber: String = "010-1234-5678",
) = SmsSendRequest(title, message, targetNumber)

class SmsValidatorKoTest :
    BehaviorSpec({

        Given("문자 발송 요청 검증") {
            When("하이픈 포함 정상 번호") {
                Then("통과한다") {
                    SmsValidator.validate(request(), "032-000-0000").shouldBeNull()
                }
            }

            When("수신번호가 하이픈 제외 12자 (상한)") {
                Then("통과한다") {
                    SmsValidator.validate(request(targetNumber = "010-1234-5678-9"), "032-000-0000").shouldBeNull()
                }
            }

            When("수신번호가 하이픈 제외 12자를 초과") {
                Then("사유를 반환한다") {
                    SmsValidator.validate(request(targetNumber = "010-1234-5678-90"), "032-000-0000").shouldNotBeNull() shouldContain "수신번호"
                }
            }

            When("발신번호가 하이픈 제외 12자를 초과") {
                Then("사유를 반환한다") {
                    SmsValidator.validate(request(), "032-0000-0000-000").shouldNotBeNull() shouldContain "발신번호"
                }
            }

            When("발신번호가 설정되지 않음") {
                Then("사유를 반환한다") {
                    SmsValidator.validate(request(), "").shouldNotBeNull() shouldContain "발신번호가 설정되지"
                }
            }

            When("제목이 50자를 초과") {
                Then("사유를 반환한다") {
                    SmsValidator.validate(request(title = "가".repeat(51)), "032-000-0000").shouldNotBeNull() shouldContain "제목"
                }
            }

            When("내용이 2000자를 초과") {
                Then("사유를 반환한다") {
                    SmsValidator.validate(request(message = "가".repeat(2001)), "032-000-0000").shouldNotBeNull() shouldContain "내용"
                }
            }

            When("내용이 비어 있음") {
                Then("사유를 반환한다") {
                    SmsValidator.validate(request(message = "  "), "032-000-0000").shouldNotBeNull() shouldContain "내용이 비어"
                }
            }

            When("숫자가 하나도 없는 값") {
                Then("숫자 0자로 상한을 통과하지 않고 걸러진다") {
                    SmsValidator.validate(request(targetNumber = "abc"), "032-000-0000").shouldNotBeNull() shouldContain "수신번호"
                    SmsValidator.validate(request(targetNumber = "---"), "032-000-0000").shouldNotBeNull() shouldContain "수신번호"
                    SmsValidator.validate(request(), "abc").shouldNotBeNull() shouldContain "발신번호"
                }
            }

            When("자릿수가 하한 미만") {
                Then("사유를 반환한다") {
                    SmsValidator.validate(request(targetNumber = "1234567"), "032-000-0000").shouldNotBeNull() shouldContain "미만"
                }
            }

            When("숫자와 하이픈 외 문자가 섞임") {
                Then("사유를 반환한다") {
                    SmsValidator.validate(request(targetNumber = "010 1234 5678"), "032-000-0000").shouldNotBeNull() shouldContain "숫자와 하이픈"
                }
            }
        }

        Given("번호 정규화") {
            When("표기가 다른 같은 번호") {
                Then("같은 값으로 정규화된다") {
                    SmsValidator.normalizeNumber("010-1234-5678") shouldBe "01012345678"
                    SmsValidator.normalizeNumber("01012345678") shouldBe "01012345678"
                }
            }
        }

        Given("UMS 응답 코드 매핑") {
            When("프로시저가 반환한 stat 코드") {
                Then("정의된 값으로 매핑된다") {
                    UmsSendStat.fromCode(0) shouldBe UmsSendStat.SUCCESS
                    UmsSendStat.fromCode(-1) shouldBe UmsSendStat.NO_ACCOUNT
                    UmsSendStat.fromCode(9) shouldBe UmsSendStat.NUMBER_TOO_LONG
                    UmsSendStat.fromCode(null) shouldBe UmsSendStat.NOT_SENT
                    UmsSendStat.fromCode(77) shouldBe UmsSendStat.NOT_SENT
                }
            }

            When("view_sendResult의 결과·상태 코드") {
                Then("정의된 값으로 매핑된다") {
                    UmsResultCode.fromCode(903) shouldBe UmsResultCode.SUCCESS
                    UmsResultCode.fromCode(905) shouldBe UmsResultCode.FAILURE
                    UmsResultCode.fromCode(null).shouldBeNull()
                    UmsStatusCode.fromCode(333) shouldBe UmsStatusCode.COMPLETED
                    UmsStatusCode.fromCode(334) shouldBe UmsStatusCode.CANCELED
                    UmsStatusCode.fromCode(335) shouldBe UmsStatusCode.ERROR
                }
            }
        }
    })

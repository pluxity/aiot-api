package com.pluxity.aiot.data.subscription.dto

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/** 규격상 con.Timestamp는 선택이라 악취 단말은 안 보낸다. cin 생성 시각 ct로 대신한다. */
class SubscriptionConTimestampKoTest :
    BehaviorSpec({
        val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

        fun alarm(con: String) =
            """
            {"m2m:sgn":{"nev":{"rep":{"m2m:cin":{"ty":4,"ct":"20260915T040446","con":$con}},"net":3},
             "sur":"Mobius/SLENO-NC.SNIOT_pluxity/SNIOT-P-BOS-001/34959_1.0_0/data-report/prod-SNIOT-P-BOS-001-34959_1.0_0"}}
            """.trimIndent()

        Given("Timestamp가 없는 악취 단말 알림") {
            val body = alarm("""{"H2S":31,"NH3":21,"Humidity":48.35,"Temperature":27.36}""")

            When("역직렬화하면") {
                val con =
                    mapper
                        .readValue(body, SubscriptionAlarm::class.java)
                        .sgn.nev.rep.cin.con

                Then("ct를 측정 시각으로 쓴다") {
                    con.timestamp shouldBe "20260915T040446"
                    con.h2s shouldBe 31
                }
            }
        }

        Given("Timestamp가 있는 쓰레기 감지기 알림") {
            val body = alarm("""{"Timestamp":"20260915T040324","ActualFilling":13,"HighThreshold":60}""")

            When("역직렬화하면") {
                val con =
                    mapper
                        .readValue(body, SubscriptionAlarm::class.java)
                        .sgn.nev.rep.cin.con

                Then("ct가 아니라 Timestamp를 쓴다") {
                    con.timestamp shouldBe "20260915T040324"
                }
            }
        }
    })

package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.ConstructorExtension
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.Spec
import io.kotest.extensions.spring.SpringExtension
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/** 패키지와 클래스명이 계약이다. 옮기면 컴파일은 되지만 조용히 무시된다. */
class ProjectConfig : AbstractProjectConfig() {
    // com.pluxity.aiot.config.ProjectConfig에 있던 설정을 옮겼다. 스펙들이 이 격리를 전제로
    // 쓰여 있어(형제 When의 목 호출이 누적되면 verify가 깨진다) 값을 그대로 유지한다.
    override val isolationMode = IsolationMode.InstancePerLeaf

    override val extensions = listOf(SpringConstructorInjection)
}

/**
 * 생성자 주입. Kotest 6에서 5.x의 @AutoScan 자동 등록이 사라져 직접 등록해야 한다.
 * SpringExtension()을 그대로 넣으면 생성자 파라미터가 없는 스펙에서 깨져 그 분기만 씌운다.
 */
object SpringConstructorInjection : ConstructorExtension {
    private val delegate = SpringExtension()

    override fun <T : Spec> instantiate(clazz: KClass<T>): Spec? =
        if (clazz.primaryConstructor?.parameters.isNullOrEmpty()) null else delegate.instantiate(clazz)
}

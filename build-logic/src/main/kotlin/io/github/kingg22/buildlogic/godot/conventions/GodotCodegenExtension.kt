package io.github.kingg22.buildlogic.godot.conventions

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

/**
 * DSL extension for configuring Godot codegen conventions.
 *
 * Usage:
 *   godotCodegen {
 *       // Top-level defaults (used when no combinations defined)
 *       backend.set(CodegenBackend.KOTLIN_NATIVE)
 *       kind.set(CodegenKind.API)
 *
 *       // Define named combinations:
 *       combinations {
 *           "callable" {
 *               backend.set(CodegenBackend.KOTLIN_NATIVE)
 *               kind.set(CodegenKind.CALLABLE)
 *           }
 *       }
 *   }
 */
abstract class GodotCodegenExtension @Inject constructor(objectFactory: ObjectFactory) : GodotCodegenDsl {
    val combinations: NamedDomainObjectContainer<CombinationSpec> = objectFactory.domainObjectContainer(
        CombinationSpec::class,
    ) { name ->
        val conventionsExtension = this@GodotCodegenExtension
        objectFactory.newInstance<CombinationSpec>(name).apply {
            this.backend.convention(conventionsExtension.backend)
            this.kind.convention(conventionsExtension.kind)
            this.packageName.convention(conventionsExtension.packageName)
            this.skipPlatformSpecificApis.convention(conventionsExtension.skipPlatformSpecificApis)
            this.excludeTypes.convention(conventionsExtension.excludeTypes)
        }
    }

    abstract class CombinationSpec @Inject constructor(private val receiverName: String) :
        GodotCodegenDsl,
        Named {
        override fun getName(): String = receiverName
    }
}

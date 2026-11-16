package dev.minimal.launcher

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

/**
 * Import rules that keep the package layout honest (see docs/architecture.md):
 * app → feature → core, features never import each other, core never imports upward,
 * and core.domain stays plain Kotlin.
 */
class ArchitectureTest {
    private val base = "dev.minimal.launcher"
    private val production = Konsist.scopeFromProduction()

    @Test
    fun `features do not import other features`() {
        production.files
            .withPackage("$base.feature..")
            .assertFalse(testName = "features do not import other features") { file ->
                val own = file.ownFeature()
                file.imports.any { it.name.startsWith("$base.feature.") && !it.name.startsWith("$base.feature.$own.") }
            }
    }

    @Test
    fun `core does not import features or app`() {
        production.files
            .withPackage("$base.core..")
            .assertFalse(testName = "core does not import features or app") { file ->
                file.imports.any { it.name.startsWith("$base.feature.") || it.name.startsWith("$base.app.") }
            }
    }

    @Test
    fun `domain is plain Kotlin without Android or Compose`() {
        production.files
            .withPackage("$base.core.domain..")
            .assertFalse(testName = "domain is plain Kotlin") { file ->
                file.imports.any { it.name.startsWith("android") || it.name.startsWith("androidx") }
            }
    }

    private fun KoFileDeclaration.ownFeature(): String =
        packagee?.name.orEmpty().removePrefix("$base.feature.").substringBefore('.')
}

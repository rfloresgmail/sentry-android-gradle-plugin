package io.sentry.compose

import androidx.compose.ui.Modifier
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.sentry.SentryKotlinCompilerPlugin
import io.sentry.SentryKotlinCompilerPluginCommandLineProcessor
import kotlin.test.assertEquals
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class JetpackComposeInstrumentationTest {

    class Fixture {

        // A Fake modifier, which provides hooks so we can not only verify that our code compiles,
        // but also execute it and ensure our tags are set correctly.
        private val fakeSentryModifier = SourceFile.kotlin(
            name = "TestTagModifier.kt",
            contents =
            // language=kotlin
            """
            package io.test.compose

            import androidx.compose.ui.Modifier
            import androidx.compose.ui.semantics.SemanticsPropertyKey
            import androidx.compose.ui.semantics.semantics
            import androidx.compose.ui.ExperimentalComposeUiApi
//            import androidx.compose.ui.semantics.testTagsAsResourceId

            // Based on TestTag
            // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/semantics/SemanticsProperties.kt;l=166;drc=76bc6975d1b520c545b6f8786ff5c9f0bc22bd1f
            private val TestTag = SemanticsPropertyKey<String>(
                name = "TestTag",
                mergePolicy = { parentValue, _ ->
                    // Never merge SentryTags, to avoid leaking internal test tags to parents.
                    parentValue
                }
            )

            object TestTagModifier {

                private var callback: (tag: String) -> Unit = {}

                @JvmStatic
                public fun setCallback(c: (tag: String) -> Unit) {
                    callback = c
                }

                @OptIn(ExperimentalComposeUiApi::class)
                @JvmStatic
                public fun Modifier.testTag(tag: String): Modifier {
                    callback(tag)
                    return semantics(
                        properties = {
                            //testTagsAsResourceId = true
                            this[TestTag] = tag
                        }
                    )
                }
            }
            """.trimIndent()
        )
        private val fakeComposeFunction = SourceFile.kotlin(
            name = "ComposableFunction.kt",
            contents = """
            package io.test.compose
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.platform.testTag
            import androidx.compose.runtime.Composable

            @Composable
            fun ComposableFunction(
                modifier : Modifier = Modifier,
                text: String
            ) {
                // no-op
            }
            """.trimIndent()
        )

        fun compileFile(
            file: SourceFile,
            includeFakeSentryModifier: Boolean = true
        ): KotlinCompilation.Result {
            val result = KotlinCompilation().apply {
                sources = if (includeFakeSentryModifier) {
                    listOf(
                        fakeSentryModifier,
                        fakeComposeFunction,
                        file
                    )
                } else {
                    listOf(fakeComposeFunction, file)
                }
                compilerPluginRegistrars = listOf(SentryKotlinCompilerPlugin())
                commandLineProcessors = listOf(SentryKotlinCompilerPluginCommandLineProcessor())
                inheritClassPath = true
                messageOutputStream = System.out // see diagnostics in real time
            }.compile()

            return result
        }

        /**
         * Executes the compiled code.
         * Also registers a hook in our fake SentryModifier and collects all calls to it.
         * This way we can ensure the Compiler Plugin actually added the correct .sentryTag
         * calls, and they don't fail during execution
         */
        fun execute(
            compilation: KotlinCompilation.Result,
            className: String = "io.sentry.samples.Example",
            method: String,
            methodArgTypes: List<String> = emptyList(),
            methodArgs: List<Any> = emptyList()
        ): List<String> {
            // inject a callback into our fake modifier
            val tags = mutableListOf<String>()
            try {
                val fakeModifierClass =
                    compilation.classLoader.loadClass("io.test.compose.TestTagModifier")
                val setCallbackMethod =
                    fakeModifierClass.getMethod("setCallback", Function1::class.java)
                setCallbackMethod.invoke(fakeModifierClass, { tag: String ->
                    tags.add(tag)
                })
            } catch (ex: ClassNotFoundException) {
                // no-op
            }

            val kClazz = compilation.classLoader.loadClass(className)
            val exampleObj = kClazz.getDeclaredConstructor().newInstance()

            val argClasses =
                methodArgTypes.map { compilation.classLoader.loadClass(it) }.toTypedArray()
            val args = methodArgs.toTypedArray()
            kClazz.getMethod(method, *argClasses).invoke(exampleObj, *args)

            return tags
        }
    }

    @Test
    fun `When no modifier is present, inject sentry modifier`() {
        val kotlinSource = SourceFile.kotlin(
            name = "Example.kt",
            contents = """
            package io.sentry.samples
            import androidx.compose.runtime.Composable
            import io.test.compose.ComposableFunction

            class Example {
                @Composable
                fun NoMoodifier() {
                    // expected:
                    // val sentryModifier = Modifier.sentryTag("NoMoodifier")
                    // ComposableFunction(modifier = sentryModifier, text = ..
                    ComposableFunction(
                        text = "No Moodifier Argument"
                    )
                }
            }
            """.trimIndent()
        )

        val fixture = Fixture()

        val compilation = fixture.compileFile(kotlinSource)
        assert(compilation.exitCode == KotlinCompilation.ExitCode.OK)

        val tags = fixture.execute(compilation, method = "NoMoodifier")
        assertEquals(1, tags.size)
        assertEquals("NoMoodifier", tags[0])
    }

    @Test
    fun `Modifier Companion calls are replaced with sentry modifier`() {
        val kotlinSource = SourceFile.kotlin(
            name = "Example.kt",
            contents = """
            package io.sentry.samples

            import androidx.compose.foundation.layout.fillMaxSize
            import androidx.compose.foundation.layout.padding
            import androidx.compose.ui.unit.dp
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import io.test.compose.ComposableFunction

            class Example {
                @Composable
                fun ExistingModifier() {
                    ComposableFunction(
                        // expected:
                        // val sentryModifier = Modifier.sentryTag("ExistingModifier")
                        // val modifier = sentryModifier.then(Modifier.fillMaxSize().padding(8.dp))
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        text = "Existing Modifier"
                    )
                }
            }
            """.trimIndent()
        )

        val fixture = Fixture()

        val compilation = fixture.compileFile(kotlinSource)
        assert(compilation.exitCode == KotlinCompilation.ExitCode.OK)

        val tags = fixture.execute(compilation, method = "ExistingModifier")
        assertEquals(1, tags.size)
        assertEquals("ExistingModifier", tags[0])
    }

    @Test
    fun `modifier arguments are enriched with sentry modifier`() {
        // when a modifier gets passed as a function argument
        val kotlinSource = SourceFile.kotlin(
            name = "Example.kt",
            contents = """
            package io.sentry.samples

            import androidx.compose.foundation.layout.fillMaxSize
            import androidx.compose.foundation.layout.padding
            import androidx.compose.ui.unit.dp
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import io.test.compose.ComposableFunction

            class Example {
                @Composable
                fun ModifierAsParam(modifier : Modifier = Modifier.Companion) {
                    ComposableFunction(
                        // expected:
                        // val sentryModifier = Modifier.sentryTag("ModifierAsParam")
                        // val modifier = sentryModifier.then(modifier.fillMaxSize().padding(8.dp))
                        modifier = modifier.fillMaxSize().padding(8.dp),
                        text = "ModifierAsParam"
                    )
                }
            }
            """.trimIndent()
        )

        val fixture = Fixture()

        // then it should compile fine
        val compilation = fixture.compileFile(kotlinSource)
        assert(compilation.exitCode == KotlinCompilation.ExitCode.OK)

        // and
        val tags = fixture.execute(
            compilation,
            className = "io.sentry.samples.Example",
            method = "ModifierAsParam",
            methodArgTypes = listOf("androidx.compose.ui.Modifier"),
            methodArgs = listOf(Modifier)
        )
        assertEquals(1, tags.size)
        assertEquals("ModifierAsParam", tags[0])
    }

    @Test
    fun `when sentry modifier does not exist, still compiles`() {
        // when an example is compiled without our sentryModifier
        val kotlinSource = SourceFile.kotlin(
            name = "Example.kt",
            contents = """
            package io.sentry.samples

            import androidx.compose.runtime.Composable
            import io.test.compose.ComposableFunction

            class Example {
                @Composable
                fun Example() {
                    ComposableFunction(
                        text = "Example"
                    )
                }
            }
            """.trimIndent()
        )

        val fixture = Fixture()

        val compilation = fixture.compileFile(
            kotlinSource,
            includeFakeSentryModifier = false
        )

        // then it should still compile fine
        assert(compilation.exitCode == KotlinCompilation.ExitCode.OK)

        // emit a compiler warning
        assert(
            compilation.messages.contains("io.sentry.compose.Modifier.sentryTag() not found")
//            compilation.messages.contains("testTag() not found")
        )

        // and still execute fine
        val tags = fixture.execute(
            compilation,
            className = "io.sentry.samples.Example",
            method = "Example"
        )
        assertEquals(0, tags.size)
    }

    @Test
    fun `When multiple composables are present, inject sentry modifier`() {
        val kotlinSource = SourceFile.kotlin(
            name = "Example.kt",
            contents = """
            package io.sentry.samples
            import androidx.compose.runtime.Composable
            import io.test.compose.ComposableFunction
            import androidx.compose.material.Text

            class Example {
                @Composable
                fun NoMoodifier() {
                    // expected:
                    // val sentryModifier = Modifier.sentryTag("NoMoodifier")
                    // ComposableFunction(modifier = sentryModifier, text = ..
                    ComposableFunction(
                        text = "No Moodifier Argument first"
                    )
                    ComposableFunction(
                        text = "No Moodifier Argument second"
                    )
                }
                @Composable
                fun SecondFunction() {
                    // expected:
                    // val sentryModifier = Modifier.sentryTag("NoMoodifier")
                    // ComposableFunction(modifier = sentryModifier, text = ..
                    ComposableFunction(
                        text = "second fun"
                    )
                }

            }
            """.trimIndent()
        )

        val fixture = Fixture()

        val compilation = fixture.compileFile(kotlinSource)
        assert(compilation.exitCode == KotlinCompilation.ExitCode.OK)

        val tags = fixture.execute(compilation, method = "NoMoodifier")
        assertEquals(1, tags.size)
        assertEquals("NoMoodifier", tags[0])

        val tags2 = fixture.execute(compilation, method = "SecondFunction")
        assertEquals(1, tags2.size)
        assertEquals("SecondFunction", tags2[0])
    }
}

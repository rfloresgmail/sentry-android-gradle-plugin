package io.test.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.testTagsAsResourceId

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
    public fun Modifier.testingTag(tag: String): Modifier {
        callback(tag)
        return semantics(
            properties = {
                testTagsAsResourceId = true
                this[TestTag] = tag
            }
        )
    }
}

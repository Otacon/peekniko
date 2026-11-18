package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import peekniko.SpannedTextBuilderFactoryBBText
import peekniko.internal.bodyrenderer.SpannedTextDefaults

internal class ProjectConfig : AbstractProjectConfig() {
    init {
        SpannedTextDefaults.factory = SpannedTextBuilderFactoryBBText
    }

    override val coroutineTestScope = true
}

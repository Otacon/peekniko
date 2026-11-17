package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig

internal class ProjectConfig : AbstractProjectConfig() {
    override val coroutineTestScope = true
}

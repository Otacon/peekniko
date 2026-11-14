package peekniko.internal.bodyrenderer

internal actual fun defaultSpannedTextBuilderFactory(): SpannedTextBuilderFactory =
    SpannedTextBuilderFactory { ComposeSpannedTextBuilder() }

package peekniko

import peekniko.internal.bodyrenderer.BodyRendererTheme
import peekniko.internal.bodyrenderer.Color
import peekniko.internal.bodyrenderer.SpannedText
import peekniko.internal.bodyrenderer.SpannedTextBuilder

internal class SpannedTextBuilderBBText : SpannedTextBuilder {
    private val content = StringBuilder()

    override fun append(text: String, color: Color?, isBold: Boolean) {
        if (isBold) content.append("[b]")
        color?.let { content.append("[c=").append(themeRole(it)).append("]") }
        content.append(text.escapeBBText())
        if (color != null) content.append("[/c]")
        if (isBold) content.append("[/b]")
    }

    override fun append(other: SpannedTextBuilder) {
        content.append(other as SpannedTextBuilderBBText)
    }

    @Deprecated("Does not work on tests", replaceWith = ReplaceWith("buildText"))
    override fun build(): SpannedText = throw IllegalStateException("Use buildText() instead")

    fun buildText(): String = content.toString()

    private fun themeRole(color: Color): String {
        val candidates = listOf(
            "Dark.key" to BodyRendererTheme.Dark.key,
            "Dark.string" to BodyRendererTheme.Dark.string,
            "Dark.integer" to BodyRendererTheme.Dark.integer,
            "Dark.number" to BodyRendererTheme.Dark.number,
            "Dark.boolean" to BodyRendererTheme.Dark.boolean,
            "Dark.nullValue" to BodyRendererTheme.Dark.nullValue,
            "Dark.separator" to BodyRendererTheme.Dark.separator,
            "Dark.tag" to BodyRendererTheme.Dark.tag,
            "Dark.attributeName" to BodyRendererTheme.Dark.attributeName,
            "Dark.attributeValue" to BodyRendererTheme.Dark.attributeValue,
            "Dark.comment" to BodyRendererTheme.Dark.comment,
            "Dark.doctype" to BodyRendererTheme.Dark.doctype,
            "Dark.text" to BodyRendererTheme.Dark.text,
            "Light.key" to BodyRendererTheme.Light.key,
            "Light.string" to BodyRendererTheme.Light.string,
            "Light.integer" to BodyRendererTheme.Light.integer,
            "Light.number" to BodyRendererTheme.Light.number,
            "Light.boolean" to BodyRendererTheme.Light.boolean,
            "Light.nullValue" to BodyRendererTheme.Light.nullValue,
            "Light.separator" to BodyRendererTheme.Light.separator,
            "Light.tag" to BodyRendererTheme.Light.tag,
            "Light.attributeName" to BodyRendererTheme.Light.attributeName,
            "Light.attributeValue" to BodyRendererTheme.Light.attributeValue,
            "Light.comment" to BodyRendererTheme.Light.comment,
            "Light.doctype" to BodyRendererTheme.Light.doctype,
            "Light.text" to BodyRendererTheme.Light.text,
        )
        return candidates.firstOrNull { (_, themed) ->
            color.red == themed.red && color.green == themed.green && color.blue == themed.blue && color.alpha == themed.alpha
        }?.first ?: error("BBText snapshots use BodyRendererTheme role names")
    }

    private fun String.escapeBBText(): String =
        replace("\\", "\\\\")
            .replace("[", "\\[")
            .replace("]", "\\]")
}

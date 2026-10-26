package restarhalf.stellar.schedule.ui.screens.announcement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.LocalReferenceLinkHandler
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownBlockQuote
import com.mikepenz.markdown.compose.elements.MarkdownCheckBox
import com.mikepenz.markdown.compose.elements.MarkdownCodeBackground
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownHeader
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.ReferenceLinkHandler
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AnnouncementMarkdown(
    content: String,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    val textStyles = MiuixTheme.textStyles

    val mdColors = remember(colors) {
        DefaultMarkdownColors(
            text = colors.onBackground,
            codeBackground = colors.surfaceContainerHighest,
            inlineCodeBackground = colors.surfaceContainerHighest,
            dividerColor = colors.dividerLine,
            tableBackground = colors.surfaceContainerHigh.copy(alpha = 0.4f),
            highlightText = colors.onPrimaryContainer,
            highlightBackground = colors.primaryContainer,
        )
    }
    val mdTypography = remember(textStyles, colors) {
        DefaultMarkdownTypography(
            h1 = textStyles.title3.copy(fontWeight = FontWeight.Bold),
            h2 = textStyles.title4.copy(fontWeight = FontWeight.Bold),
            h3 = textStyles.headline2.copy(fontWeight = FontWeight.Bold),
            h4 = textStyles.body1.copy(fontWeight = FontWeight.Bold),
            h5 = textStyles.subtitle.copy(fontWeight = FontWeight.Bold),
            h6 = textStyles.footnote1.copy(fontWeight = FontWeight.Bold),
            text = textStyles.main.copy(color = colors.onBackground),
            code = textStyles.body2.copy(fontFamily = FontFamily.Monospace, color = colors.onBackground),
            inlineCode = textStyles.body2.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            quote = textStyles.body2.copy(
                fontStyle = FontStyle.Italic,
                color = colors.onSurfaceVariantSummary,
            ),
            paragraph = textStyles.paragraph.copy(color = colors.onBackground),
            ordered = textStyles.paragraph.copy(color = colors.onBackground),
            bullet = textStyles.paragraph.copy(color = colors.onBackground),
            list = textStyles.paragraph.copy(color = colors.onBackground),
            textLink = TextLinkStyles(
                style = SpanStyle(
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                ),
            ),
            table = textStyles.body2.copy(color = colors.onBackground),
        )
    }

    val components = remember(mdColors, mdTypography) {
        markdownComponents(
            heading1 = { AnnouncementHeading(it, mdTypography.h1, accent = true) },
            heading2 = { AnnouncementHeading(it, mdTypography.h2, accent = true) },
            heading3 = { AnnouncementHeading(it, mdTypography.h3, accent = false) },
            heading4 = { AnnouncementHeading(it, mdTypography.h4, accent = false) },
            heading5 = { AnnouncementHeading(it, mdTypography.h5, accent = false) },
            heading6 = { AnnouncementHeading(it, mdTypography.h6, accent = false) },
            setextHeading1 = { AnnouncementHeading(it, mdTypography.h1, accent = true, contentChildType = MarkdownTokenTypes.SETEXT_CONTENT) },
            setextHeading2 = { AnnouncementHeading(it, mdTypography.h2, accent = true, contentChildType = MarkdownTokenTypes.SETEXT_CONTENT) },
            blockQuote = { model ->
                AnnouncementBlockQuote(model.content, model.node, model.typography.quote)
            },
            codeFence = { model ->
                MarkdownCodeFence(
                    content = model.content,
                    node = model.node,
                    style = model.typography.code,
                ) { code, language, style ->
                    AnnouncementCodeBlock(code = code, language = language, style = style)
                }
            },
            codeBlock = { model ->
                MarkdownCodeBlock(
                    content = model.content,
                    node = model.node,
                    style = model.typography.code,
                ) { code, language, style ->
                    AnnouncementCodeBlock(code = code, language = language, style = style)
                }
            },
            image = { model -> AnnouncementMarkdownImage(model.content, model.node, onImageClick) },
            checkbox = { model ->
                AnnouncementCheckBox(model.content, model.node, model.typography.text)
            },
        )
    }

    Markdown(
        content = content,
        colors = mdColors,
        typography = mdTypography,
        imageTransformer = Coil3ImageTransformerImpl,
        components = components,
        modifier = modifier,
    )
}

@Composable
private fun AnnouncementHeading(
    model: MarkdownComponentModel,
    style: TextStyle,
    accent: Boolean,
    contentChildType: IElementType = MarkdownTokenTypes.ATX_CONTENT,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accent) {
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(colors.primary, RoundedCornerShape(2.dp)),
            )
        }
        MarkdownHeader(
            content = model.content,
            node = model.node,
            style = style.copy(color = colors.onSurface),
            contentChildType = contentChildType,
        )
    }
}

@Composable
private fun AnnouncementBlockQuote(
    content: String,
    node: ASTNode,
    style: TextStyle,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceContainerHigh.copy(alpha = 0.5f)),
    ) {
        MarkdownBlockQuote(
            content = content,
            node = node,
            style = style.copy(color = colors.primary.copy(alpha = 0.6f)),
        )
    }
}

@Composable
private fun AnnouncementCodeBlock(
    code: String,
    language: String?,
    style: TextStyle,
) {
    val colors = MiuixTheme.colorScheme
    MarkdownCodeBackground(
        color = colors.surfaceContainerHighest,
        shape = RoundedCornerShape(12.dp),
        showHeader = true,
        language = language,
        code = code,
    ) {
        Text(
            text = code,
            style = style,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        )
    }
}

@Composable
private fun AnnouncementMarkdownImage(
    content: String,
    node: ASTNode,
    onImageClick: (String) -> Unit,
) {
    val link = node.resolveAnnouncementImageLink(content, LocalReferenceLinkHandler.current) ?: return
    val alt = node.resolveAnnouncementImageAlt(content)
    val colors = MiuixTheme.colorScheme

    var attempt by remember(link) { mutableIntStateOf(0) }
    val platformContext = LocalPlatformContext.current
    val model = remember(link, attempt) {

        ImageRequest.Builder(platformContext)
            .data(if (attempt > 0) "$link#retry-$attempt" else link)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onImageClick(link) },
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = alt,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
            loading = {
                PlaceholderBox {
                    Text(
                        text = "图片加载中…",
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
            },
            error = {
                PlaceholderBox(
                    modifier = Modifier.clickable { attempt++ },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "图片加载失败",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )
                        Text(
                            text = "点击重试",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.primary,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun PlaceholderBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun AnnouncementCheckBox(
    content: String,
    node: ASTNode,
    style: TextStyle,
) {
    val colors = MiuixTheme.colorScheme
    MarkdownCheckBox(
        content = content,
        node = node,
        style = style,
        checkedIndicator = { checked, modifier ->
            Box(
                modifier = modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
                    .background(
                        color = if (checked) colors.primary else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (checked) colors.primary else colors.outline,
                        shape = RoundedCornerShape(6.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Text(
                        text = "✓",
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onPrimary,
                    )
                }
            }
        },
    )
}

private fun ASTNode.findChildOfTypeRecursiveAnnouncement(type: IElementType): ASTNode? {
    children.forEach {
        if (it.type == type) {
            return it
        } else {
            val found = it.findChildOfTypeRecursiveAnnouncement(type)
            if (found != null) {
                return found
            }
        }
    }
    return null
}

private fun ASTNode.resolveAnnouncementImageLink(
    content: String,
    referenceLinkHandler: ReferenceLinkHandler?,
): String? {
    findChildOfTypeRecursiveAnnouncement(MarkdownElementTypes.LINK_DESTINATION)?.let {
        return it.getUnescapedTextInNode(content)
    }
    val refNode = findChildOfTypeRecursiveAnnouncement(MarkdownElementTypes.FULL_REFERENCE_LINK)
        ?: findChildOfTypeRecursiveAnnouncement(MarkdownElementTypes.SHORT_REFERENCE_LINK)
        ?: return null
    val label = refNode.findChildOfType(MarkdownElementTypes.LINK_LABEL)
        ?.getUnescapedTextInNode(content)
        ?: return null
    return referenceLinkHandler?.find(label)?.takeIf { it.isNotEmpty() }
}

private fun ASTNode.resolveAnnouncementImageAlt(content: String): String? {
    findChildOfTypeRecursiveAnnouncement(MarkdownElementTypes.LINK_TEXT)?.let {
        val text = it.getUnescapedTextInNode(content).trim('[', ']').trim()
        if (text.isNotEmpty()) return text
    }
    findChildOfTypeRecursiveAnnouncement(MarkdownElementTypes.LINK_LABEL)?.let {
        val text = it.getUnescapedTextInNode(content).trim('[', ']').trim()
        if (text.isNotEmpty()) return text
    }
    return null
}

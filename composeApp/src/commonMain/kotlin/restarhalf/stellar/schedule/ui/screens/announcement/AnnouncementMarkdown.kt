package restarhalf.stellar.schedule.ui.screens.announcement

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
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
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
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
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 公告正文 Markdown 渲染（纯 core 库 + miuix 风格定制）。
 *
 * 不引入 -m3 模块：colors/typography 从 MiuixTheme 提取，所有组件经
 * [markdownComponents] 逐一映射为 miuix 观感：
 * - h1/h2 左侧 primary 竖条装饰，h3-h6 纯加粗分级
 * - 引用块圆角浅色底 + primary 竖线
 * - 代码块圆角高对比底色 + 语言标签 + 复制按钮（库自带）
 * - 图片圆角、点击全屏预览
 * - 任务列表 checkbox 换成 miuix 风格小方块
 */
@Composable
fun AnnouncementMarkdown(
    content: String,
    modifier: Modifier = Modifier,
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
            setextHeading1 = { AnnouncementHeading(it, mdTypography.h1, accent = true) },
            setextHeading2 = { AnnouncementHeading(it, mdTypography.h2, accent = true) },
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
            image = { model -> AnnouncementMarkdownImage(model.content, model.node) },
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

/**
 * 标题：h1/h2 带 primary 竖条装饰，h3-h6 纯加粗分级。
 */
@Composable
private fun AnnouncementHeading(
    model: MarkdownComponentModel,
    style: TextStyle,
    accent: Boolean,
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
        )
    }
}

/**
 * 引用块：圆角浅色底，左侧 primary 竖线（由库 drawBehind 绘制，颜色随 style）。
 */
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

/**
 * 代码块：圆角高对比底色 + 顶栏（语言标签 + 复制按钮，库自带）。
 *
 * 复制走 Compose 的 LocalClipboardManager（Android/iOS 均可用），无需平台定制。
 */
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

/**
 * 块级图片：圆角 + 点击全屏预览。
 */
@Composable
private fun AnnouncementMarkdownImage(
    content: String,
    node: ASTNode,
) {
    val link = node.resolveAnnouncementImageLink(content, LocalReferenceLinkHandler.current) ?: return
    val alt = node.resolveAnnouncementImageAlt(content)
    var previewing by remember(link) { mutableStateOf(false) }

    Coil3ImageTransformerImpl.transform(link).let { imageData ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { previewing = true },
        ) {
            Image(
                painter = imageData.painter,
                contentDescription = alt ?: imageData.contentDescription,
                modifier = Modifier.fillMaxWidth(),
                alignment = imageData.alignment,
                contentScale = imageData.contentScale,
                alpha = imageData.alpha,
                colorFilter = imageData.colorFilter,
            )
        }
    }

    if (previewing) {
        AnnouncementImagePreviewDialog(
            url = link,
            alt = alt,
            onDismiss = { previewing = false },
        )
    }
}

/**
 * 全屏图片预览：miuix OverlayDialog 承载网络大图，点击图片/外部关闭。
 */
@Composable
private fun AnnouncementImagePreviewDialog(
    url: String,
    alt: String?,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = alt ?: "图片预览",
        titleColor = DialogDefaults.titleColor(),
        summary = "点击图片关闭",
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = null,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        renderInRootScaffold = true,
        content = {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = alt,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDismiss() },
            )
        },
    )
}

/**
 * 任务列表 checkbox：miuix 风格小方块（选中 primary 实底 + 对勾）。
 */
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
                        fontSize = 12.sp,
                        color = colors.onPrimary,
                    )
                }
            }
        },
    )
}

/*
 * ── 图片解析（私有实现） ──────────────────────────────────────────────
 * 库内 `resolveImageLink` / `resolveImageAlt` / `findChildOfTypeRecursive`
 * 均为 internal，跨模块不可见。以下为行为等价的自写版本，仅依赖 public API：
 * `org.intellij.markdown.ast.findChildOfType` + 库的 public
 * `getUnescapedTextInNode`（内部用 public 的 EntityConverter 处理转义）。
 */

/** 先序递归查找第一个指定类型的后代节点（库 internal 版本的等价实现）。 */
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

/**
 * 解析 IMAGE 节点的目标 URL。
 * 优先取内联 `LINK_DESTINATION`；否则按 `FULL_REFERENCE_LINK` /
 * `SHORT_REFERENCE_LINK` 引用形式，用 `LINK_LABEL` 经 [referenceLinkHandler]
 * 查引用定义（即 `![alt][id]` 引用式图片）。
 */
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

/** 解析 IMAGE 节点的 alt 文本，依次回退 LINK_TEXT / LINK_LABEL。 */
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

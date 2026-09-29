package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import restarhalf.stellar.schedule.data.remote.PEFreeApplyAtt
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.image.attachmentBadgeLabel
import restarhalf.stellar.schedule.ui.image.isImageAttachment
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.PEFreeApplyViewModel
import restarhalf.stellar.schedule.ui.viewmodel.freeApplyStatusText
import restarhalf.stellar.schedule.ui.viewmodel.freeApplyTypeText
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 免测申请详情页
 *
 * 证明材料走 Coil URL 预览（与公告图片同一套缓存），点开复用公告全屏看图器。
 */
@Composable
fun PEFreeApplyDetailScreen(
    vm: PEFreeApplyViewModel,
    applyId: String,
    onImageClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme
    val detail = uiState.detail

    LaunchedEffect(applyId) {
        vm.loadDetail(applyId)
    }

    val statusText = when {
        uiState.detailError != null -> uiState.detailError
        detail == null && !uiState.detailLoading -> null
        else -> null
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "申请详情",
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Back,
                                contentDescription = "返回",
                            )
                        }
                    },
                )
                AnimatedVisibility(
                    visible = statusText != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.clip(CircleShape)
                                .background(colors.surfaceContainerHigh)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(style = MiuixTheme.textStyles.footnote1, text = statusText ?: "")
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = uiState.detailLoading,
            onRefresh = { vm.loadDetail(applyId) },
            pullToRefreshState = pullToRefreshState,
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
            modifier = Modifier.fillMaxSize().padding(
                PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = 0.dp
                )
            )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
                contentPadding = appPageContentPadding(
                    innerPadding = PaddingValues(),
                    outerPadding = appScaffoldPadding,
                    extraTop = 12.dp,
                    extraStart = 12.dp,
                    extraEnd = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (detail != null) {
                    item { SmallTitle(text = "申请信息") }
                    item {
                        AppCard {
                            DetailRow(label = "学年", value = detail.schoolYear.ifBlank { "—" })
                            DetailRow(
                                label = "申请类型",
                                value = freeApplyTypeText(
                                    detail.freeApplyType,
                                    uiState.typeLabelMap,
                                ),
                            )
                            DetailRow(
                                label = "状态",
                                value = freeApplyStatusText(
                                    detail.applyStatus,
                                    uiState.statusLabelMap,
                                ),
                            )
                            if (detail.stuName.isNotBlank()) {
                                DetailRow(label = "姓名", value = detail.stuName)
                            }
                            if (detail.stdNumber.isNotBlank()) {
                                DetailRow(label = "学号", value = detail.stdNumber)
                            }
                            if (detail.schoolGrade.isNotBlank()) {
                                DetailRow(label = "年级", value = detail.schoolGrade)
                            }
                            if (detail.displayTime.isNotBlank()) {
                                DetailRow(label = "申请时间", value = detail.displayTime)
                            }
                            if (detail.remark.isNotBlank()) {
                                DetailRow(label = "备注", value = detail.remark)
                            }
                        }
                    }

                    val attList = detail.attList
                    item { SmallTitle(text = "证明材料") }
                    item {
                        AppCard {
                            if (attList.isEmpty()) {
                                Text(
                                    text = "暂无附件",
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = colors.onSurfaceVariantSummary,
                                    modifier = Modifier.padding(16.dp),
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    attList.forEach { att ->
                                        val isImage = isImageAttachment("", att.fileName)
                                        val url = if (isImage) vm.attPreviewUrl(att.attId) else ""
                                        AttachmentRow(
                                            att = att,
                                            previewBytes = if (isImage) {
                                                uiState.detailAttPreviews[att.attId]
                                            } else {
                                                null
                                            },
                                            loading = att.attId in uiState.detailAttLoading,
                                            canPreview = isImage && url.isNotBlank(),
                                            onLoad = { vm.loadAttPreview(att.attId) },
                                            onOpen = {
                                                if (isImage && url.isNotBlank()) {
                                                    onImageClick(url)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!uiState.detailLoading && uiState.detailError == null) {
                    item {
                        Text(
                            text = "暂无详情",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = colors.onSurfaceVariantSummary,
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun AttachmentRow(
    att: PEFreeApplyAtt,
    previewBytes: ByteArray?,
    loading: Boolean,
    canPreview: Boolean,
    onLoad: () -> Unit,
    onOpen: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val hasPreview = previewBytes != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canPreview) {
                if (hasPreview) onOpen() else onLoad()
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (previewBytes != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(previewBytes)
                        .memoryCacheKey("pe-free-att-${att.attId}")
                        .crossfade(true)
                        .build(),
                    contentDescription = att.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = if (loading) "…" else attachmentBadgeLabel(att.fileName, ""),
                    style = MiuixTheme.textStyles.footnote1,
                    color = colors.onSurfaceVariantSummary,
                )
            }
        }
        Text(
            text = att.fileName.ifBlank { att.attId },
            style = MiuixTheme.textStyles.footnote1,
            modifier = Modifier.weight(1f),
        )
        if (canPreview) {
            Text(
                text = when {
                    loading -> "下载中"
                    hasPreview -> "查看"
                    else -> "加载"
                },
                style = MiuixTheme.textStyles.footnote1,
                color = colors.primary,
            )
        }
    }
}

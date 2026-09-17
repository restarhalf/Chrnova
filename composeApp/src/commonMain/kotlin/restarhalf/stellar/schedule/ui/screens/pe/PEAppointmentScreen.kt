package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.data.remote.PEAppointmentItem
import restarhalf.stellar.schedule.data.remote.PEAppointmentTimeSlot
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.PEAppointmentViewModel
import restarhalf.stellar.schedule.ui.viewmodel.appointmentStatusText
import restarhalf.stellar.schedule.ui.viewmodel.canCancelAppointment
import restarhalf.stellar.schedule.ui.viewmodel.canEnterAppointment
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 学生体测预约屏幕
 *
 * 对齐真实接口链路：
 * 列表 → 详情（选日期）→ 选时段 → appointmentEnter
 */
@Composable
fun PEAppointmentScreen(
    vm: PEAppointmentViewModel,
    onLogin: () -> Unit,
    onBack: () -> Unit,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val loggedIn by vm.isLoggedIn.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            vm.loadMyAppointments()
            vm.loadAvailableAppointments()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.selectedTab) {
            vm.selectTab(pagerState.currentPage)
        }
    }

    val statusText = when {
        uiState.error != null -> uiState.error
        !loggedIn -> null
        uiState.loadedMy && uiState.selectedTab == 0 && uiState.myItems.isEmpty() -> "暂无我的预约"
        uiState.loadedAvailable && uiState.selectedTab == 1 && uiState.availableItems.isEmpty() -> "暂无可预约场次"
        else -> null
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "体测预约",
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Back, contentDescription = "返回")
                        }
                    },
                )
                if (loggedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TabRowWithContour(
                            tabs = listOf("我的预约", "可预约"),
                            selectedTabIndex = pagerState.currentPage,
                            onTabSelected = { index ->
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                        )
                    }
                }
                AnimatedVisibility(
                    visible = statusText != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.clip(CircleShape)
                                .background(colors.surfaceContainerHigh)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(style = MiuixTheme.textStyles.footnote1, text = statusText ?: "")
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = uiState.loading,
            onRefresh = { vm.refreshCurrent() },
            pullToRefreshState = pullToRefreshState,
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
            modifier = Modifier.fillMaxSize().padding(
                PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = 0.dp,
                ),
            ),
        ) {
            if (!loggedIn) {
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
                ) {
                    item {
                        SmallTitle(text = "账号")
                        AppCard {
                            ArrowPreference(
                                title = "登录体测系统",
                                summary = "登录后可查看与管理体测预约",
                                onClick = onLogin,
                            )
                        }
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().padding(
                        PaddingValues(
                            top = 0.dp,
                            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = 0.dp,
                        ),
                    ),
                ) { page ->
                    when (page) {
                        0 -> AppointmentListPage(
                            items = uiState.myItems,
                            isMy = true,
                            loadingMore = uiState.loadingMore,
                            hasMore = uiState.myHasMore,
                            scrollBehavior = topAppBarScrollBehavior,
                            outerPadding = appScaffoldPadding,
                            onLoadMore = { vm.loadMoreMy() },
                            onItemClick = { item ->
                                if (canCancelAppointment(item)) vm.requestCancel(item)
                            },
                        )
                        else -> AppointmentListPage(
                            items = uiState.availableItems,
                            isMy = false,
                            loadingMore = uiState.loadingMore,
                            hasMore = uiState.availableHasMore,
                            scrollBehavior = topAppBarScrollBehavior,
                            outerPadding = appScaffoldPadding,
                            onLoadMore = { vm.loadMoreAvailable() },
                            onItemClick = { item ->
                                if (canEnterAppointment(item)) vm.openBooking(item)
                            },
                        )
                    }
                }
            }
        }
    }

    val cancelTarget = uiState.cancelTarget
    if (cancelTarget != null) {
        WindowDialog(
            show = true,
            title = "取消预约",
            summary = "确认要取消「${cancelTarget.appointmentName.ifBlank { "该场次" }}」吗？",
            onDismissRequest = { vm.dismissCancel() },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { vm.dismissCancel() },
                ) {
                    Text(text = "再想想")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = { vm.confirmCancel() },
                ) {
                    Text(text = "取消预约", color = colors.onPrimary)
                }
            }
        }
    }

    val booking = uiState.booking
    if (booking != null) {
        BookingSheet(
            state = booking,
            actionInFlight = uiState.actionInFlight,
            onDismiss = { vm.dismissBooking() },
            onSelectDate = { vm.selectBookingDate(it) },
            onSelectTime = { vm.selectTimeSlot(it) },
            onConfirm = { vm.confirmEnter() },
        )
    }

    val actionMessage = uiState.actionMessage
    if (actionMessage != null) {
        WindowDialog(
            show = true,
            title = "提示",
            summary = actionMessage,
            onDismissRequest = { vm.consumeActionMessage() },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = { vm.consumeActionMessage() },
                ) {
                    Text(text = "知道了", color = colors.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun BookingSheet(
    state: PEAppointmentViewModel.BookingSheetState,
    actionInFlight: Boolean,
    onDismiss: () -> Unit,
    onSelectDate: (String) -> Unit,
    onSelectTime: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val detail = state.detail
    val dates = detail?.availableDates.orEmpty().ifEmpty {
        listOfNotNull(state.selectedDate)
    }

    WindowDialog(
        show = true,
        title = "预约体测",
        summary = state.item.appointmentName.ifBlank { "选择日期与时段" },
        onDismissRequest = { if (!actionInFlight) onDismiss() },
    ) {
        // 按钮固定在底部，中间内容可滚动，避免时段过多把按钮挤出可视区
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.loadingDetail) {
                    Text(text = "正在加载场次详情...", style = MiuixTheme.textStyles.footnote1)
                } else {
                    val content = detail?.appointmentContent
                        ?: state.item.appointmentContent
                    if (content.isNotBlank()) {
                        Text(
                            text = content,
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                    HorizontalDivider()
                    SmallTitle(text = "预约日期")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        dates.forEach { date ->
                            val selected = date == state.selectedDate
                            DateChip(
                                text = date,
                                selected = selected,
                                onClick = { if (!selected) onSelectDate(date) },
                            )
                        }
                    }

                    SmallTitle(text = "预约时段")
                    when {
                        state.selectedDate == null -> {
                            Text(text = "暂无可选日期", style = MiuixTheme.textStyles.footnote1)
                        }
                        state.loadingTimes -> {
                            Text(text = "正在加载时段...", style = MiuixTheme.textStyles.footnote1)
                        }
                        state.timeSlots.isEmpty() -> {
                            Text(
                                text = if (state.loadedTimes) "该日期暂无可用时段" else "请选择日期",
                                style = MiuixTheme.textStyles.footnote1,
                                color = colors.onSurfaceVariantSummary,
                            )
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                state.timeSlots.forEach { slot ->
                                    TimeSlotRow(
                                        slot = slot,
                                        selected = slot.timesId == state.selectedTimesId,
                                        onClick = { onSelectTime(slot.timesId) },
                                    )
                                }
                            }
                        }
                    }

                    state.error?.let { err ->
                        Text(
                            text = err,
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.error,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            HorizontalDivider()
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !actionInFlight,
                    onClick = onDismiss,
                ) {
                    Text(text = "取消")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    enabled = !actionInFlight && state.selectedDate != null && state.selectedTimesId != null,
                    onClick = onConfirm,
                ) {
                    Text(
                        text = if (actionInFlight) "提交中..." else "确认预约",
                        color = colors.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DateChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) colors.primary.copy(alpha = 0.18f)
                else colors.surfaceContainerHigh,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote1,
            color = if (selected) colors.primary else colors.onSurface,
        )
    }
}

@Composable
private fun TimeSlotRow(
    slot: PEAppointmentTimeSlot,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val full = slot.enters >= slot.quota && slot.quota > 0
    AppCard(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = !full && slot.isEntered != "1", onClick = onClick),
        colors = if (selected) colors.primary.copy(alpha = 0.12f) else colors.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = slot.label,
                style = MiuixTheme.textStyles.body2,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                text = when {
                    slot.isEntered == "1" -> "已预约"
                    full -> "已满"
                    else -> "${slot.enters}/${slot.quota}"
                },
                style = MiuixTheme.textStyles.footnote1,
                color = colors.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun AppointmentListPage(
    items: List<PEAppointmentItem>,
    isMy: Boolean,
    loadingMore: Boolean,
    hasMore: Boolean,
    scrollBehavior: ScrollBehavior,
    outerPadding: PaddingValues,
    onLoadMore: () -> Unit,
    onItemClick: (PEAppointmentItem) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    LaunchedEffect(items.size, hasMore) {
        if (hasMore && items.isNotEmpty() && items.size <= 5) {
            onLoadMore()
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .pageScrollModifiers(scrollBehavior = scrollBehavior),
        contentPadding = appPageContentPadding(
            innerPadding = PaddingValues(),
            outerPadding = outerPadding,
            extraTop = 12.dp,
            extraStart = 12.dp,
            extraEnd = 12.dp,
        ),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items,
            key = {
                it.temporaryId.ifBlank { it.appointmentId }
                    .ifBlank { "${it.appointmentName}-${it.appointmentDate}" }
            },
        ) { item ->
            val canAct = if (isMy) canCancelAppointment(item) else canEnterAppointment(item)
            AppCard(
                modifier = Modifier.fillMaxWidth().clickable(enabled = canAct) { onItemClick(item) },
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.appointmentName.ifBlank { "体测预约" },
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        StatusChip(status = item.appointmentStatus)
                    }
                    if (item.appointmentContent.isNotBlank()) {
                        Text(
                            text = item.appointmentContent,
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                    if (isMy && item.crtTime.isNotBlank()) {
                        MetaRow(label = "报名时间", value = item.crtTime)
                    }
                    if (!isMy && item.enterStartTime.isNotBlank()) {
                        MetaRow(label = "报名开始", value = item.enterStartTime)
                    }
                    if (!isMy && item.enterEndTime.isNotBlank()) {
                        MetaRow(label = "报名结束", value = item.enterEndTime)
                    }
                    if (item.appointmentDate.isNotBlank()) {
                        MetaRow(label = "预约日期", value = item.appointmentDate)
                    }
                    if (item.appointmentTimes.isNotBlank()) {
                        MetaRow(label = "预约时段", value = item.appointmentTimes)
                    }
                    if (item.timeQuota > 0) {
                        MetaRow(
                            label = if (isMy) "预约人数" else "名额",
                            value = "${item.alreadyQuota}/${item.timeQuota}",
                        )
                    }
                    if (canAct) {
                        Button(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = { onItemClick(item) },
                        ) {
                            Text(text = if (isMy) "取消预约" else "预约", color = colors.onPrimary)
                        }
                    }
                }
            }
        }
        if (hasMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (loadingMore) "加载中..." else "正在加载更多...",
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    val colors = MiuixTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = colors.onSurfaceVariantSummary,
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}

@Composable
private fun StatusChip(status: String) {
    val colors = MiuixTheme.colorScheme
    val label = appointmentStatusText(status)
    Box(
        modifier = Modifier.clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text = label, style = MiuixTheme.textStyles.footnote1)
    }
}

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Add
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.icons.Send
import restarhalf.stellar.schedule.ui.icons.Stop
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.AgentViewModel
import restarhalf.stellar.schedule.ui.viewmodel.AgentViewModel.Conversation
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Snackbar
import top.yukonga.miuix.kmp.basic.SnackbarDefaults
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixOverscrollEffect
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun AgentScreen(
    onBack: () -> Unit,
) {
    val vm: AgentViewModel = koinViewModel()
    val agentUiState by vm.uiState.collectAsState()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val overscrollEffect = MiuixOverscrollEffect()
    val listState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }

    val lastMessage =
        agentUiState.messages.lastOrNull()
    val lastMessageText =
        lastMessage?.text.orEmpty()
    @Suppress("Deprecation")
    val clipboardManager = LocalClipboardManager.current

    var renameTargetId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    val shouldFollowBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex =
                layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val lastIndex =
                (agentUiState.messages.size - 1)
                    .coerceAtLeast(0)
            lastVisibleIndex >= lastIndex - 1
        }
    }

    LaunchedEffect(
        agentUiState.messages.lastOrNull()?.id,
        lastMessageText,
    ) {
        if (
            agentUiState.messages.isNotEmpty()
            && shouldFollowBottom
        ) {
            listState.animateScrollToItem(
                agentUiState.messages.lastIndex
            )
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 40f) vm.onDrawerOpenChange(true)
                        if (dragAmount < -40f) vm.onDrawerOpenChange(false)
                    }
                },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(
                state = snackBarHostState,
                content = {
                    Snackbar(
                        data = it,
                        colors = SnackbarDefaults.snackbarColors(
                            containerColor = MiuixTheme.colorScheme.errorContainer,
                            contentColor = MiuixTheme.colorScheme.onErrorContainer
                        )
                ) }
            ) },
            topBar = {
                AppPageTopBar(
                    title = agentUiState.conversations
                        .firstOrNull { it.id == agentUiState.activeConversationId }
                        ?.summary ?: "Chrnova Helper",
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = { onBack() }) {
                            Icon(imageVector = Back, contentDescription = "")
                        }
                    },
                )
            },
            bottomBar = {
                BottomInputField(
                    userInput = agentUiState.userInput,
                    onInputChange = vm::onUserInputChange,
                    send = vm::sendMessage,
                    stop = vm::stopMessage,
                    streaming = agentUiState.streaming,
                )
            },
            popupHost = {
                OverlayDialog(
                    show = renameTargetId != null,
                    title = "重命名对话",
                    onDismissRequest = { renameTargetId = null },
                    ){
                        Column {
                            TextField(
                                value = renameText,
                                onValueChange = { renameText = it },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                label = "请输入标题",
                            )
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    modifier = Modifier.weight(1f).padding(end = 5.dp),
                                    onClick = { renameTargetId = null },
                                ) {
                                    Text(text = "取消")
                                }
                                Button(
                                    modifier = Modifier.weight(1f).padding(start = 5.dp),
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    onClick = {
                                        val targetId = renameTargetId ?: return@Button
                                        vm.renameConversation(targetId, renameText)
                                        renameTargetId = null
                                    },
                                ) {
                                    Text(text = "确定")
                                }
                            }
                        }
                    }
            }
        ) { paddingValues ->
            LaunchedEffect(paddingValues.calculateBottomPadding()) {
                if (agentUiState.messages.isNotEmpty()) {
                    listState.animateScrollToItem(agentUiState.messages.size - 1)
                }
            }
            LazyColumn(
                state = listState,
                modifier =
                    Modifier.fillMaxSize()
                        .padding(
                            PaddingValues(
                                top = paddingValues.calculateTopPadding(),
                                start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                                end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                                bottom = paddingValues.calculateBottomPadding(),
                            ),
                        ),
                contentPadding =
                    appPageContentPadding(
                        innerPadding = PaddingValues(),
                        outerPadding = appScaffoldPadding,
                        extraTop = 12.dp,
                        extraStart = 16.dp,
                        extraEnd = 16.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                overscrollEffect = overscrollEffect,
            ) {
                items(agentUiState.messages, { it.id }) { message ->
                    if(message.fromUser)
                    {
                        Menu(
                            menuItems = listOf("复制", "回溯"),
                            onMenuItemClick = { index ->
                                when (index) {
                                    0 -> {
                                        clipboardManager.setText(AnnotatedString(message.text))
                                    }
                                    1 -> vm.revertMessage(message.id)
                                }
                            },
                        ) {
                            MessageBubble(
                                text = message.text,
                                fromUser = message.fromUser,
                                streaming = message.streaming,
                            )
                        }
                    }else{
                        Menu(
                            menuItems = listOf("复制"),
                            onMenuItemClick = { index ->
                                when (index) {
                                    0-> clipboardManager.setText(AnnotatedString(message.text))
                                }
                            },
                        ) {
                            MessageBubble(
                                text = message.text,
                                fromUser = message.fromUser,
                                streaming = message.streaming,
                            )
                        }
                    }

                }
                agentUiState.pendingConfirmation?.let { pending ->
                    item(key = "pending-confirmation-${pending.toolCallId}") {
                        ConfirmationCard(
                            toolName = pending.toolName,
                            policy = pending.policy,
                            arguments = pending.arguments,
                            submitting = pending.submitting,
                            onApprove = { vm.decidePendingConfirmation(true) },
                            onDeny = { vm.decidePendingConfirmation(false) },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = agentUiState.drawerOpen,
            enter = fadeIn() + slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy,
                ),
            ),
            exit = fadeOut() + slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioNoBouncy,
                ),
            ),
        ) {
            Box(Modifier.fillMaxSize()) {
                ConversationsDrawer(
                    paddingValues = PaddingValues(top = appScaffoldPadding.calculateTopPadding()),
                    conversations = agentUiState.conversations,
                    activeConversationId = agentUiState.activeConversationId,
                    delete = vm::deleteConversation,
                    rename = { id ->
                        val currentTitle = agentUiState.conversations.firstOrNull { it.id == id }?.summary.orEmpty()
                        renameTargetId = id
                        renameText = currentTitle
                    },
                    trans = vm::transConversation,
                    newConversation = vm::newConversation,
                )
            }
        }
    }
}

@Composable
private fun BottomInputField(
    userInput: String,
    onInputChange: (String) -> Unit,
    send: () -> Unit,
    stop: () -> Unit,
    streaming: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = MiuixTheme.colorScheme.background, shape = RoundedCornerShape(16.dp))
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 10.dp)
                .imePadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TextField(
            value = userInput,
            onValueChange = { onInputChange(it) },
            modifier = Modifier.weight(1f).padding(end = 10.dp),
        )
        val canSend = userInput.isNotBlank()
        IconButton(
            onClick = { if (streaming) stop() else send() },
            backgroundColor = if (!streaming && !canSend) MiuixTheme.colorScheme.disabledPrimaryButton else MiuixTheme.colorScheme.primary,
            cornerRadius = 100.dp,
            enabled = streaming || canSend,
            minHeight = 48.dp,
            minWidth = 48.dp,
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = if (streaming) Stop else Send,
                contentDescription = "",
                tint = if (!streaming && !canSend) MiuixTheme.colorScheme.disabledOnPrimaryButton else MiuixTheme.colorScheme.onPrimary,
            )
        }
    }
}
@Composable
fun ThinkingText(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition()

    val scale by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(
                    MiuixTheme.colorScheme.primary
                )
        )

        Spacer(Modifier.width(8.dp))

        Text(
            "思考中",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSecondaryVariant
        )
    }
}
@Composable
fun MessageBubble(
    text: String,
    fromUser: Boolean,
    streaming: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bigRadius = 18.dp
    val tailRadius = 6.dp
    val showText = text.trim()
    val bubbleColor = if (fromUser) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer
    val textColor = if (fromUser) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceContainer

    val shape = if (fromUser) {
        RoundedCornerShape(
            topStart = bigRadius, topEnd = bigRadius,
            bottomEnd = tailRadius, bottomStart = bigRadius,
        )
    } else {
        RoundedCornerShape(
            topStart = bigRadius, topEnd = bigRadius,
            bottomEnd = bigRadius, bottomStart = tailRadius,
        )
    }
    if (streaming && showText.isEmpty()) {
        ThinkingText()
    }
    else if(showText.isNotEmpty()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(max = 320.dp),
                shape = shape,
                color = bubbleColor,
                contentColor = textColor,
            ) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Markdown(
                        modifier = Modifier.wrapContentWidth(),
                        content = showText,
                        colors = if (fromUser) {
                            DefaultMarkdownColors(
                                text = MiuixTheme.colorScheme.onPrimary,
                                codeBackground = MiuixTheme.colorScheme.primaryContainer,
                                inlineCodeBackground = MiuixTheme.colorScheme.primaryContainer,
                                dividerColor = MiuixTheme.colorScheme.onPrimaryVariant,
                                tableBackground = MiuixTheme.colorScheme.primaryContainer,
                            )
                        } else {
                            DefaultMarkdownColors(
                                text = MiuixTheme.colorScheme.onSurfaceContainer,
                                codeBackground = MiuixTheme.colorScheme.surfaceContainerHigh,
                                inlineCodeBackground = MiuixTheme.colorScheme.surfaceContainerHigh,
                                dividerColor = MiuixTheme.colorScheme.dividerLine,
                                tableBackground = MiuixTheme.colorScheme.surfaceContainerHigh,
                            )
                        },
                        typography = DefaultMarkdownTypography(
                            h1 = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                            h2 = MiuixTheme.textStyles.title2.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                            h3 = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                            h4 = MiuixTheme.textStyles.title4.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                            h5 = MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            h6 = MiuixTheme.textStyles.headline2.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            text = MiuixTheme.textStyles.body1.copy(fontSize = 14.sp),
                            code = MiuixTheme.textStyles.body2.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            inlineCode = MiuixTheme.textStyles.body2.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            quote = MiuixTheme.textStyles.body1.copy(fontStyle = FontStyle.Italic, fontSize = 14.sp),
                            paragraph = MiuixTheme.textStyles.paragraph.copy(fontSize = 14.sp),
                            ordered = MiuixTheme.textStyles.body1.copy(fontSize = 14.sp),
                            bullet = MiuixTheme.textStyles.body1.copy(fontSize = 14.sp),
                            list = MiuixTheme.textStyles.body1.copy(fontSize = 14.sp),
                            textLink = TextLinkStyles(
                                style = SpanStyle(
                                    color = MiuixTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ),
                            table = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
                        ),
                    )
                }
            }
        }
    }
    else{null}

}

@Composable
private fun ConfirmationCard(
    toolName: String,
    policy: String,
    arguments: Map<String, String>,
    submitting: Boolean,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "工具调用确认", style = MiuixTheme.textStyles.title3)
            Text(text = "工具：$toolName", style = MiuixTheme.textStyles.body1)
            Text(text = "权限：$policy", style = MiuixTheme.textStyles.body2)
            val summary = arguments.entries.joinToString { "${it.key}=${it.value}" }.ifBlank { "无参数" }
            Text(text = summary, style = MiuixTheme.textStyles.body2)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onDeny,
                    enabled = !submitting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("拒绝")
                }
                Button(
                    onClick = onApprove,
                    enabled = !submitting,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("允许")
                }
            }
        }
    }
}

@Composable
private fun ConversationsDrawer(
    paddingValues: PaddingValues,
    conversations: List<Conversation>,
    activeConversationId: String?,
    delete: (String) -> Unit,
    rename: (String) -> Unit,
    trans: (String) -> Unit,
    newConversation: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxHeight(),
        color = MiuixTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(start = 14.dp, end = 14.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "对话列表",
                    style = MiuixTheme.textStyles.title2,
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    items(conversations, { it.id }) { conversation ->
                        val selected = conversation.id == activeConversationId
                        Menu(
                            menuItems = listOf("重命名", "删除"),
                            onMenuItemClick = { index ->
                                when (index) {
                                    0 -> rename(conversation.id)
                                    1 -> delete(conversation.id)
                                }
                            },
                            onClick = { trans(conversation.id) },
                        ) {
                            Surface(
                                color = if (selected) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.padding(vertical = 3.dp),
                            ) {
                                BasicComponent {
                                    Text(
                                        text = conversation.summary,
                                        style = MiuixTheme.textStyles.subtitle,
                                        modifier = Modifier.padding(start = 6.dp),
                                        textAlign = TextAlign.Start,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = newConversation,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(25.dp),
                        tint = MiuixTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "新建对话",
                        color = MiuixTheme.colorScheme.onPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Menu(
    menuItems: List<String>,
    onMenuItemClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var touchWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var componentPositionInWindow by remember { mutableStateOf(Offset.Zero) }

    val touchPositionProvider = remember(touchWindowOffset) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowBounds: IntRect,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
                popupMargin: IntRect,
                alignment: PopupPositionProvider.Align,
            ): IntOffset {
                val tx = touchWindowOffset.x.toInt()
                val ty = touchWindowOffset.y.toInt()
                val x = if (windowBounds.right - tx >= popupContentSize.width) tx else tx - popupContentSize.width
                val y = if (windowBounds.bottom - ty >= popupContentSize.height) ty else ty - popupContentSize.height
                return IntOffset(
                    x = x.coerceIn(windowBounds.left, (windowBounds.right - popupContentSize.width).coerceAtLeast(windowBounds.left)),
                    y = y.coerceIn(windowBounds.top, (windowBounds.bottom - popupContentSize.height).coerceAtLeast(windowBounds.top)),
                )
            }
            override fun getMargins(): PaddingValues = PaddingValues(0.dp)
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                componentPositionInWindow = coords.positionInWindow()
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchWindowOffset = componentPositionInWindow + down.position
                    waitForUpOrCancellation()
                }
            }
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = { showMenu = true },
            ),
    ) {
        content()

        WindowListPopup(
            show = showMenu,
            popupPositionProvider = touchPositionProvider,
            alignment = PopupPositionProvider.Align.Start,
            onDismissRequest = { showMenu = false },
        ) {
            ListPopupColumn {
                menuItems.forEachIndexed { index, item ->
                    DropdownImpl(
                        text = item,
                        optionSize = menuItems.size,
                        isSelected = false,
                        index = index,
                        onSelectedIndexChange = { idx ->
                            onMenuItemClick(idx)
                            showMenu = false
                        },
                    )
                }
            }
        }
    }
}



package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haooz.softglass.backdrop.GlassBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.DropdownColors
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.interactive.DropdownPanelDragSelectState
import top.yukonga.miuix.kmp.overlay.LocalSheetContentBackdrop
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun resolveSelectedText(entries: List<DropdownEntry>): String? {
    for (entry in entries) {
        for (item in entry.items) {
            if (item.selected) return item.text
        }
    }
    return null
}

@Composable
private fun RowScope.DropdownTriggerContent(
    selectedText: String?,
    actionColor: Color,
    alpha: () -> Float,
) {
    if (selectedText != null) {
        Text(
            text = selectedText,
            fontSize = 14.2.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            maxLines = 1,
            modifier =
                Modifier
                    .padding(end = 8.dp)
                    .graphicsLayer { this.alpha = alpha() },
        )
    }
    DropdownArrowEndAction(
        actionColor = actionColor,
        modifier = Modifier.graphicsLayer { this.alpha = alpha() },
    )
}

private val DefaultCollapseExtra = DpSize(14.dp, 10.dp)

@Composable
fun GlassDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    summary: String? = null,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    collapseOnSelection: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onSelectedIndexChange: ((Int) -> Unit)? = null,
) {
    val entry =
        remember(items, selectedIndex, onSelectedIndexChange) {
            DropdownEntry(
                items.mapIndexed { index, item ->
                    DropdownItem(
                        text = item,
                        selected = index == selectedIndex,
                        onClick = { onSelectedIndexChange?.invoke(index) },
                    )
                },
            )
        }
    GlassDropdownMenu(
        entry = entry,
        title = title,
        modifier = modifier,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        dropdownColors =
            DropdownDefaults.dropdownColors(
                containerColor = Color.Transparent,
                selectedContainerColor = Color.Transparent,
            ),
        startAction = startAction,
        bottomAction = bottomAction,
        insideMargin = insideMargin,
        maxHeight = maxHeight,
        enabled = enabled,
        collapseOnSelection = collapseOnSelection,
        onExpandedChange = onExpandedChange,
        liquidGlassBackdrop = LocalSheetContentBackdrop.current ?: LocalLiquidGlassBackdrop.current,
    )
}

@Composable
fun GlassDropdownMenu(
    entry: DropdownEntry,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    renderInRootScaffold: Boolean = true,
    collapseOnSelection: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    liquidGlassBackdrop: GlassBackdrop? = null,
    collapseExtra: DpSize = DefaultCollapseExtra,
) {
    val entries = remember(entry) { listOf(entry) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDropdownExpanded = remember { mutableStateOf(false) }
    val isHoldDown = remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val currentHapticFeedback by rememberUpdatedState(hapticFeedback)
    val currentOnExpandedChange = rememberUpdatedState(onExpandedChange)
    val coroutineScope = rememberCoroutineScope()

    var popupFullyDismissed by remember { mutableStateOf(true) }

    val setExpanded: (Boolean) -> Unit =
        remember {
            { expanded ->
                if (isDropdownExpanded.value != expanded) {
                    isDropdownExpanded.value = expanded
                    currentOnExpandedChange.value?.invoke(expanded)
                }
            }
        }

    LaunchedEffect(isDropdownExpanded.value) {
        if (isDropdownExpanded.value) {
            repeat(2) { withFrameNanos { } }
            popupFullyDismissed = false
        }
    }

    val fractionState = remember { mutableStateOf(0f) }
    var triggerSize by remember { mutableStateOf(IntSize.Zero) }
    val triggerAlpha: () -> Float = { if (popupFullyDismissed) 1f else 0f }

    val dragSelectState = remember { DropdownPanelDragSelectState() }

    val hasEntries = entry.items.isNotEmpty()
    val actualEnabled = enabled && hasEntries
    val selectedText = resolveSelectedText(entries)
    val actionColor =
        if (actualEnabled) {
            MiuixTheme.colorScheme.onSurfaceVariantActions
        } else {
            MiuixTheme.colorScheme.disabledOnSecondaryVariant
        }

    val handleClick: () -> Unit =
        remember(actualEnabled) {
            {
                if (actualEnabled) {
                    setExpanded(!isDropdownExpanded.value)
                    if (isDropdownExpanded.value) {
                        isHoldDown.value = true
                        currentHapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }
                }
            }
        }

    BasicComponent(
        modifier = modifier,
        interactionSource = interactionSource,
        insideMargin = insideMargin,
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        startAction = startAction,
        endActions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.onSizeChanged { triggerSize = it },
            ) {
                DropdownTriggerContent(
                    selectedText = selectedText,
                    actionColor = actionColor,
                    alpha = { triggerAlpha() },
                )
            }
            if (hasEntries) {
                OverlayDropdownPopup(
                    entries = entries,
                    show = isDropdownExpanded.value,
                    onDismiss = { setExpanded(false) },
                    onDismissFinished = {
                        isHoldDown.value = false
                        coroutineScope.launch {
                            repeat(3) { withFrameNanos { } }
                            popupFullyDismissed = true
                        }
                    },
                    maxHeight = maxHeight,
                    dropdownColors = dropdownColors,
                    renderInRootScaffold = renderInRootScaffold,
                    collapseOnSelection = collapseOnSelection,
                    liquidGlassBackdrop = liquidGlassBackdrop,
                    onFractionProgress = { fractionState.value = it },
                    collapseSize = triggerSize,
                    collapseExtra = collapseExtra,
                    dragSelectState = dragSelectState,
                    collapseContent = {
                        DropdownTriggerContent(
                            selectedText = selectedText,
                            actionColor = actionColor,
                            alpha = { 1f },
                        )
                    },
                )
            }
        },
        bottomAction = bottomAction,
        onClick = handleClick,
        role = Role.DropdownList,
        holdDownState = isHoldDown.value,
        enabled = actualEnabled,
    )
}

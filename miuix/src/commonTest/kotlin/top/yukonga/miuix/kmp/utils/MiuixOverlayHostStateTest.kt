package top.yukonga.miuix.kmp.utils

import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiuixOverlayHostStateTest {
    @Test
    fun idleRegistrationsDoNotActivateHost() {
        val host = MiuixOverlayHostState()
        host.dialogs.add(MiuixPopupUtils.DialogState(mutableStateOf(false), 1f))
        host.popups.add(MiuixPopupUtils.PopupState(mutableStateOf(false), 2f))
        assertFalse(host.isActive)
    }

    @Test
    fun showRequestActivatesBeforeRendererStarts() {
        val host = MiuixOverlayHostState()
        val show = mutableStateOf(false)
        host.dialogs.add(MiuixPopupUtils.DialogState(show, 1f))
        show.value = true
        assertTrue(host.isActive)
    }

    @Test
    fun exitingEntryKeepsHostActiveUntilAnimationFinishes() {
        val host = MiuixOverlayHostState()
        val dialog = MiuixPopupUtils.DialogState(mutableStateOf(false), 1f)
        host.dialogs.add(dialog)
        dialog.isAnimating = true
        assertTrue(host.isActive)
        dialog.isAnimating = false
        assertFalse(host.isActive)
    }

    @Test
    fun closingOneOverlayDoesNotReleaseAnotherOverlay() {
        val host = MiuixOverlayHostState()
        val dialog = MiuixPopupUtils.DialogState(mutableStateOf(false), 1f)
        val popup = MiuixPopupUtils.PopupState(mutableStateOf(false), 2f)
        host.dialogs.add(dialog)
        host.popups.add(popup)
        dialog.isAnimating = true
        popup.isAnimating = true
        dialog.isAnimating = false
        host.dialogs.remove(dialog)
        assertTrue(host.isActive)
        popup.isAnimating = false
        assertFalse(host.isActive)
    }

    @Test
    fun dimExitAndPendingReopenKeepEntryActive() {
        assertFalse(active())
        assertTrue(active(dimCurrent = true))
        assertTrue(active(dimTarget = true))
        assertTrue(active(dimIdle = false))
        assertTrue(active(pendingOpen = true))
        assertTrue(active(contentCurrent = true))
        assertTrue(active(contentTarget = true))
        assertTrue(active(contentIdle = false))
        assertTrue(active(show = true))
    }

    private fun active(
        show: Boolean = false,
        pendingOpen: Boolean = false,
        contentCurrent: Boolean = false,
        contentTarget: Boolean = false,
        contentIdle: Boolean = true,
        dimCurrent: Boolean = false,
        dimTarget: Boolean = false,
        dimIdle: Boolean = true,
    ): Boolean = overlayEntryIsActive(
        show = show,
        pendingOpen = pendingOpen,
        contentCurrent = contentCurrent,
        contentTarget = contentTarget,
        contentIdle = contentIdle,
        dimCurrent = dimCurrent,
        dimTarget = dimTarget,
        dimIdle = dimIdle,
    )
}

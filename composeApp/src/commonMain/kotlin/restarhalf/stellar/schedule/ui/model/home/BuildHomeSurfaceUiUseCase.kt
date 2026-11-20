package restarhalf.stellar.schedule.ui.model.home

class BuildHomeSurfaceUiUseCase {

    enum class HeaderBackgroundMode {
        IMAGE_OVERLAY,
        PRIMARY_SOLID,
    }

    data class SurfaceUi(
        val headerBackgroundMode: HeaderBackgroundMode,
        val contentSurfaceAlpha: Float,
    )

    operator fun invoke(hasBackground: Boolean, componentsAlpha: Float): SurfaceUi =
        SurfaceUi(
            headerBackgroundMode =
                if (hasBackground) HeaderBackgroundMode.IMAGE_OVERLAY
                else HeaderBackgroundMode.PRIMARY_SOLID,
            contentSurfaceAlpha = resolveComponentsAlpha(hasBackground, componentsAlpha),
        )
}

fun resolveComponentsAlpha(hasWallpaper: Boolean, componentsAlpha: Float): Float =
    if (hasWallpaper) componentsAlpha.coerceIn(0f, 1f) else 1f

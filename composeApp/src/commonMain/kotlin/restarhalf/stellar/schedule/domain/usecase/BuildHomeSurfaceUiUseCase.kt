package restarhalf.stellar.schedule.domain.usecase

class BuildHomeSurfaceUiUseCase {

    enum class HeaderBackgroundMode {
        IMAGE_OVERLAY,
        PRIMARY_SOLID,
    }

    data class SurfaceUi(
        val headerBackgroundMode: HeaderBackgroundMode,
        val contentSurfaceAlpha: Float,
    )

    operator fun invoke(hasBackground: Boolean): SurfaceUi {
        return if (hasBackground) {
            SurfaceUi(
                headerBackgroundMode = HeaderBackgroundMode.IMAGE_OVERLAY,
                contentSurfaceAlpha = 0.4f
            )
        } else {
            SurfaceUi(
                headerBackgroundMode = HeaderBackgroundMode.PRIMARY_SOLID,
                contentSurfaceAlpha = 1f
            )
        }
    }
}

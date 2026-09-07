package restarhalf.stellar.schedule.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals

class BuildHomeSurfaceUiUseCaseTest {

    private val useCase = BuildHomeSurfaceUiUseCase()

    @Test
    fun `有背景图时走图片叠加模式`() {
        val ui = useCase(hasBackground = true, componentsAlpha = 0.5f)
        assertEquals(BuildHomeSurfaceUiUseCase.HeaderBackgroundMode.IMAGE_OVERLAY, ui.headerBackgroundMode)
        assertEquals(0.5f, ui.contentSurfaceAlpha)
    }

    @Test
    fun `透明度下限收敛到0`() {
        val ui = useCase(hasBackground = true, componentsAlpha = -1f)
        assertEquals(0f, ui.contentSurfaceAlpha)
    }

    @Test
    fun `透明度上限收敛到1`() {
        val ui = useCase(hasBackground = true, componentsAlpha = 2f)
        assertEquals(1f, ui.contentSurfaceAlpha)
    }

    @Test
    fun `无背景图时走纯色模式且不透明`() {
        val ui = useCase(hasBackground = false, componentsAlpha = 0.3f)
        assertEquals(BuildHomeSurfaceUiUseCase.HeaderBackgroundMode.PRIMARY_SOLID, ui.headerBackgroundMode)
        assertEquals(1f, ui.contentSurfaceAlpha)
    }
}

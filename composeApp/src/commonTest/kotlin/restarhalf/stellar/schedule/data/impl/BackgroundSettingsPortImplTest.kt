package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.CardEdgeThickness
import restarhalf.stellar.schedule.domain.model.CardRefractionLevel
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackgroundSettingsPortImplTest {

    private val impl = BackgroundSettingsPortImpl(MapSettings())

    @Test
    fun backgroundUriDefaultNullAndRoundTrip() = runTest {
        assertNull(impl.getBackgroundImageUri())
        assertNull(impl.observeBackgroundImageUri().first())
        impl.setBackgroundImageUri("content://bg/1")
        assertEquals("content://bg/1", impl.getBackgroundImageUri())
        assertEquals("content://bg/1", impl.observeBackgroundImageUri().first())
        impl.setBackgroundImageUri(null)
        assertNull(impl.getBackgroundImageUri())
    }

    @Test
    fun backgroundAlphaDefaultOneAndRoundTrip() = runTest {
        assertEquals(1f, impl.getBackgroundAlpha())
        impl.setBackgroundAlpha(0.5f)
        assertEquals(0.5f, impl.getBackgroundAlpha())
        assertEquals(0.5f, impl.observeBackgroundAlpha().first())
    }

    @Test
    fun backgroundBlurDefaultZeroAndRoundTrip() = runTest {
        assertEquals(0f, impl.getBackgroundBlur())
        impl.setBackgroundBlur(0.25f)
        assertEquals(0.25f, impl.getBackgroundBlur())
        assertEquals(0.25f, impl.observeBackgroundBlur().first())
    }

    @Test
    fun componentsAlphaDefaultAndRoundTrip() = runTest {
        assertEquals(BackgroundSettingsPort.DEFAULT_COMPONENTS_ALPHA, impl.getComponentsAlpha())
        impl.setComponentsAlpha(0.8f)
        assertEquals(0.8f, impl.getComponentsAlpha())
        assertEquals(0.8f, impl.observeComponentsAlpha().first())
    }

    @Test
    fun cardGlassSettingsDefaultsAndRoundTrip() = runTest {
        assertEquals(BackgroundSettingsPort.DEFAULT_CARD_BLUR_PX, impl.getCardBlurPx())
        impl.setCardBlurPx(18f)
        assertEquals(18f, impl.getCardBlurPx())
        assertEquals(18f, impl.observeCardBlurPx().first())

        assertEquals(CardRefractionLevel.DEFAULT, impl.getCardRefraction())
        impl.setCardRefraction(CardRefractionLevel.STRONG)
        assertEquals(CardRefractionLevel.STRONG, impl.getCardRefraction())
        assertEquals(CardRefractionLevel.STRONG, impl.observeCardRefraction().first())

        assertEquals(CardEdgeThickness.DEFAULT, impl.getCardEdgeThickness())
        impl.setCardEdgeThickness(CardEdgeThickness.LARGER)
        assertEquals(CardEdgeThickness.LARGER, impl.getCardEdgeThickness())
        assertEquals(CardEdgeThickness.LARGER, impl.observeCardEdgeThickness().first())

        assertTrue(impl.getCardRim())
        impl.setCardRim(false)
        assertFalse(impl.getCardRim())
        assertFalse(impl.observeCardRim().first())
    }
}

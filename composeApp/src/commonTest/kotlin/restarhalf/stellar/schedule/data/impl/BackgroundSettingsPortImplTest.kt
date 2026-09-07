package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.Campus
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun componentsAlphaDefaultOneAndRoundTrip() = runTest {
        assertEquals(1f, impl.getComponentsAlpha())
        impl.setComponentsAlpha(0.8f)
        assertEquals(0.8f, impl.getComponentsAlpha())
        assertEquals(0.8f, impl.observeComponentsAlpha().first())
    }
}

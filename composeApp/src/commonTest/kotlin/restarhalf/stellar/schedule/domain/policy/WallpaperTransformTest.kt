package restarhalf.stellar.schedule.domain.policy

import kotlin.test.Test
import kotlin.test.assertEquals

class WallpaperTransformTest {

    private val bitmapW = 1000
    private val bitmapH = 1000
    private val screenW = 1000f
    private val screenH = 2000f

    @Test
    fun fitScale取两者较小值() {
        assertEquals(1f, WallpaperTransform.fitScale(bitmapW, bitmapH, screenW, screenH))
        assertEquals(1f, WallpaperTransform.fitScale(bitmapW, bitmapH, screenH, screenW))
        assertEquals(0.5f, WallpaperTransform.fitScale(bitmapW, bitmapH, 500f, screenH))
    }

    @Test
    fun coverScale取两者较大值() {
        assertEquals(2f, WallpaperTransform.coverScale(bitmapW, bitmapH, screenW, screenH))
        assertEquals(2f, WallpaperTransform.coverScale(bitmapW, bitmapH, screenH, screenW))
    }

    @Test
    fun minScale是相对Fit的铺满倍数() {
        assertEquals(2f, WallpaperTransform.minScale(bitmapW, bitmapH, screenW, screenH))
    }

    @Test
    fun 铺满时横向可平移半屏纵向无余量() {
        val (x, y) = WallpaperTransform.maxOffset(bitmapW, bitmapH, screenW, screenH, scale = 2f)
        assertEquals(500f, x)
        assertEquals(0f, y)
    }

    @Test
    fun 缩小到Fit时没有可平移余量() {
        val (x, y) = WallpaperTransform.maxOffset(bitmapW, bitmapH, screenW, screenH, scale = 1f)
        assertEquals(0f, x)
        assertEquals(0f, y)
    }

    @Test
    fun 非法尺寸退回1倍且不留负余量() {
        assertEquals(1f, WallpaperTransform.minScale(0, 0, screenW, screenH))
        val (x, y) = WallpaperTransform.maxOffset(0, 0, screenW, screenH, scale = 1f)
        assertEquals(0f, x)
        assertEquals(0f, y)
    }
}

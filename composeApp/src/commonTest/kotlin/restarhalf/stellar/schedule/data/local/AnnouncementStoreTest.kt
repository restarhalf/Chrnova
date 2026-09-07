package restarhalf.stellar.schedule.data.local

import com.russhwolf.settings.ObservableSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.domain.model.Announcement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 公告本地缓存 Store：只负责读写（命中策略在 UseCase 层）。
 *
 * settings 用 map-backed stub（Mokkery calls）模拟真实键值存储，
 * 覆盖序列化往返、坏 JSON 容错、时间戳与已读推进。
 */
class AnnouncementStoreTest {

    private val storage = mutableMapOf<String, Any?>()

    private val settings = mock<ObservableSettings>(MockMode.autofill) {
        every { putString(any(), any()) } calls { (key: String, value: String) ->
            storage[key] = value
        }
        every { putLong(any(), any()) } calls { (key: String, value: Long) ->
            storage[key] = value
        }
        every { getStringOrNull(any()) } calls { (key: String) ->
            storage[key] as String?
        }
        every { getLong(any(), any()) } calls { (key: String, defaultValue: Long) ->
            storage[key] as? Long ?: defaultValue
        }
    }

    private val store = AnnouncementStore(settings)

    private fun jsonOf(vararg announcements: Announcement): String =
        Json.encodeToString(ListSerializer(Announcement.serializer()), announcements.toList())

    @Test
    fun `缓存写入后可读回（序列化往返）`() {
        val announcements = listOf(
            Announcement(id = "1", title = "新版发布", pinned = true, createdAt = 1000L),
            Announcement(id = "2", title = "常规公告", createdAt = 2000L, updatedAt = 2500L),
        )

        store.setCachedAnnouncements(announcements)
        val cached = store.getCachedAnnouncements()

        assertEquals(announcements, cached)
    }

    @Test
    fun `写缓存时同步写入时间戳`() {
        store.setCachedAnnouncements(emptyList())

        assertTrue(store.getCacheTimestampMs() > 0)
    }

    @Test
    fun `无缓存时返回 null`() {
        assertNull(store.getCachedAnnouncements())
    }

    @Test
    fun `坏 JSON 容错返回 null`() {
        every { settings.getStringOrNull(any()) } returns "{{{not-json"

        assertNull(store.getCachedAnnouncements())
    }

    @Test
    fun `空白缓存字符串返回 null`() {
        every { settings.getStringOrNull(any()) } returns "   "

        assertNull(store.getCachedAnnouncements())
    }

    @Test
    fun `合法 JSON 解析字段映射正确`() {
        val raw = """[{"id":"1","title":"含时间公告","created_at":1000,"updated_at":1500,"priority":1,"pinned":true}]"""
        every { settings.getStringOrNull(any()) } returns raw

        val result = store.getCachedAnnouncements()

        assertEquals(1, result?.size)
        val item = result!!.single()
        assertEquals("1", item.id)
        assertEquals("含时间公告", item.title)
        assertEquals(1000L, item.createdAt)
        assertEquals(1500L, item.updatedAt)
        assertEquals(1, item.priority)
        assertTrue(item.isImportant)
        assertTrue(item.pinned)
    }

    @Test
    fun `缓存 TTL 为 10 分钟`() {
        assertEquals(10L * 60L * 1000L, store.getCacheTtlMs())
    }

    @Test
    fun `已读时间默认 0`() {
        assertEquals(0L, store.getLastReadAtMs())
    }

    @Test
    fun `推进已读时间后可读回`() {
        store.setLastReadAtMs(700_000L)

        assertEquals(700_000L, store.getLastReadAtMs())
    }
}

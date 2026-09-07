package restarhalf.stellar.schedule.core.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReleaseUpdateSupportTest {

    // ---------- normalizeVersion ----------

    @Test
    fun stripsVPrefix() {
        assertEquals(listOf(1, 2, 3), normalizeVersion("v1.2.3"))
        assertEquals(listOf(1, 2, 3), normalizeVersion("V1.2.3"))
    }

    @Test
    fun plainVersion() {
        assertEquals(listOf(1, 2, 3), normalizeVersion("1.2.3"))
    }

    @Test
    fun nonNumericSuffixConsumedAsDelimiter() {
        // [^0-9]+ 贪婪匹配，"-" 与 "beta" 整段被吃掉
        assertEquals(listOf(1, 2, 3), normalizeVersion("1.2.3-beta"))
        assertEquals(listOf(1, 2, 3, 2), normalizeVersion("1.2.3-rc.2"))
        assertEquals(listOf(1, 2), normalizeVersion("beta1.2"))
    }

    @Test
    fun blankVersionIsEmpty() {
        assertEquals(emptyList(), normalizeVersion("  "))
    }

    @Test
    fun whitespaceIsTrimmed() {
        assertEquals(listOf(2, 0), normalizeVersion(" 2.0 "))
    }

    // ---------- isNewerVersion ----------

    @Test
    fun newerPatch() {
        assertTrue(isNewerVersion("1.0.1", "1.0.0"))
    }

    @Test
    fun newerMinorBeatsLowerPatch() {
        assertTrue(isNewerVersion("1.1.0", "1.0.9"))
    }

    @Test
    fun sameVersionIsNotNewer() {
        assertFalse(isNewerVersion("1.2.3", "1.2.3"))
    }

    @Test
    fun olderVersionIsNotNewer() {
        assertFalse(isNewerVersion("0.9.9", "1.0.0"))
    }

    @Test
    fun missingSegmentTreatedAsZero() {
        assertFalse(isNewerVersion("1.2", "1.2.0"))
        assertTrue(isNewerVersion("1.3", "1.2.9"))
    }

    @Test
    fun vPrefixComparable() {
        assertTrue(isNewerVersion("v2.0.0", "V1.9.9"))
    }

    @Test
    fun numericCompareNotLexicographic() {
        assertTrue(isNewerVersion("10.0.0", "9.0.0"))
    }

    // ---------- buildVersionWorkerUrl ----------

    @Test
    fun baseUrlWithoutUid() {
        assertEquals("$VERSION_WORKER_URL/version.json", buildVersionWorkerUrl(null))
    }

    @Test
    fun blankUidOmitted() {
        assertEquals("$VERSION_WORKER_URL/version.json", buildVersionWorkerUrl("   "))
    }

    @Test
    fun uidAppendedAsQuery() {
        assertEquals(
            "$VERSION_WORKER_URL/version.json?uid=2024081409",
            buildVersionWorkerUrl("2024081409"),
        )
    }

    @Test
    fun uidIsUrlEncoded() {
        val url = buildVersionWorkerUrl("a b&c")
        assertTrue(url.contains("uid=a%20b%26c"))
    }

    // ---------- VersionJsonResponse ----------

    @Test
    fun versionJsonDefaults() {
        val resp = VersionJsonResponse()
        assertEquals("", resp.version)
        assertEquals(DEFAULT_QUARK_SHARE_URL, resp.url)
        assertEquals("", resp.changelog)
    }

    // ---------- QQ 群链接 ----------

    @Test
    fun defaultKeyIncludesAuthKey() {
        val url = buildQqGroupWebUrl()
        assertTrue(url.startsWith("https://qm.qq.com/cgi-bin/qm/qr?k=$DEFAULT_QQ_GROUP_KEY"))
        assertTrue("authKey=" in url)
        assertTrue("jump_from=webapi" in url)
    }

    @Test
    fun customKeyHasNoAuthKey() {
        val url = buildQqGroupWebUrl("custom-key")
        assertTrue(url.endsWith("k=custom-key&jump_from=webapi"))
        assertFalse("authKey" in url)
    }

    @Test
    fun iosUrlUsesMqqapiScheme() {
        val url = buildQqGroupIosUrl()
        assertTrue(url.startsWith("mqqapi://card/show_pslcard"))
        assertTrue("uin=1084761691" in url)
        assertTrue("authSig=" in url)
    }
}

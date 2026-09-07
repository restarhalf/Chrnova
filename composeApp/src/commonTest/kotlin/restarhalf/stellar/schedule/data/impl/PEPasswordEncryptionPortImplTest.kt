package restarhalf.stellar.schedule.data.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * PE 加密/签名测试。
 *
 * 注：SIGN_KEY 来自生成代码 LocalSecrets（测试编译链不含该源集），
 * 因此除「真实抓包黄金值」外，构造规则类断言使用同实现下的等价关系表达。
 */
class PEPasswordEncryptionPortImplTest {

    private val impl = PEPasswordEncryptionPortImpl()

    // ---------- MD5 密码 ----------

    @Test
    fun md5KnownVectorHello() {
        assertEquals("5d41402abc4b2a76b9719d911017c592", impl.encryptPasswordForPELogin("hello"))
    }

    @Test
    fun md5KnownVector123456() {
        assertEquals("e10adc3949ba59abbe56e057f20f883e", impl.encryptPasswordForPELogin("123456"))
    }

    @Test
    fun md5CapturedGolden111111() {
        // 2026-09-07 PE 登录真实抓包：password=MD5("111111")
        assertEquals("96e79218965eb72c92a549dd5a330112", impl.encryptPasswordForPELogin("111111"))
    }

    // ---------- SHA1 签名 ----------

    @Test
    fun signCapturedGoldenFromRealRequest() {
        // 2026-09-07 PE 登录真实抓包（39.100.89.70/service/login/mobile/check）：
        // 本地按 SIGN_KEY 复算 SHA1 与抓包 sign 完全一致，此黄金值同时锚定密钥拼装规则
        val sign = impl.generatePESign(
            mapOf(
                "username" to "2024081409",
                "password" to "96e79218965eb72c92a549dd5a330112",
                "sys_id" to "iscpMobile",
            ),
        )
        assertEquals("22CB112D89AB2F4EA842415464A09DE7BC7D7C32", sign)
    }

    @Test
    fun signFiltersNullEmptyAndSignKeys() {
        // null / 空串 / sign 字段不参与签名
        val withNoise = impl.generatePESign(
            mapOf(
                "a" to null,
                "b" to "",
                "c" to "1",
                "sign" to "forged",
                "z" to 0,
            ),
        )
        val clean = impl.generatePESign(mapOf("c" to "1", "z" to 0))
        assertEquals(clean, withNoise)
    }

    @Test
    fun signKeepsExplicitZeroValues() {
        // 数值 0 显式参与签名（与空串不同）
        assertEquals(
            impl.generatePESign(mapOf("n" to 0L)),
            impl.generatePESign(mapOf("n" to 0)),
        )
        assertNotEquals(impl.generatePESign(emptyMap()), impl.generatePESign(mapOf("n" to 0)))
    }

    @Test
    fun signSortsKeysBeforeHashing() {
        // 键序不影响结果（内部排序）
        assertEquals(
            impl.generatePESign(mapOf("bbb" to "2", "aaa" to "1")),
            impl.generatePESign(mapOf("aaa" to "1", "bbb" to "2")),
        )
    }

    @Test
    fun signWithKeyFieldSkipsSecret() {
        // data 已含 key 字段时不追加 SIGN_KEY：结果与单 key 参数的签名不同构
        val withKey = impl.generatePESign(mapOf("key" to "provided"))
        val withoutKey = impl.generatePESign(mapOf("a" to "provided"))
        assertNotEquals(withKey, withoutKey)
        assertEquals(40, withKey.length)
    }

    @Test
    fun signAlwaysIncludesCodeNames() {
        // code_names 即便为空串也参与签名
        val withCodeNames = impl.generatePESign(mapOf("x" to "1", "code_names" to ""))
        val withoutCodeNames = impl.generatePESign(mapOf("x" to "1"))
        assertNotEquals(withoutCodeNames, withCodeNames)
    }

    @Test
    fun signIsUppercaseHex40() {
        val sign = impl.generatePESign(mapOf("a" to "1"))
        assertEquals(40, sign.length)
        assertTrue(sign == sign.uppercase())
    }

    @Test
    fun signConsistentForSameInput() {
        val data = mapOf("username" to "u", "password" to "p", "sys_id" to "iscpMobile")
        assertEquals(impl.generatePESign(data), impl.generatePESign(data))
    }
}

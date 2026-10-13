package restarhalf.stellar.schedule.data.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PEPasswordEncryptionPortImplTest {

    private val impl = PEPasswordEncryptionPortImpl()



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

        assertEquals("96e79218965eb72c92a549dd5a330112", impl.encryptPasswordForPELogin("111111"))
    }



    @Test
    fun signCapturedGoldenFromRealRequest() {


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

        assertEquals(
            impl.generatePESign(mapOf("n" to 0L)),
            impl.generatePESign(mapOf("n" to 0)),
        )
        assertNotEquals(impl.generatePESign(emptyMap()), impl.generatePESign(mapOf("n" to 0)))
    }

    @Test
    fun signSortsKeysBeforeHashing() {

        assertEquals(
            impl.generatePESign(mapOf("bbb" to "2", "aaa" to "1")),
            impl.generatePESign(mapOf("aaa" to "1", "bbb" to "2")),
        )
    }

    @Test
    fun signWithKeyFieldSkipsSecret() {

        val withKey = impl.generatePESign(mapOf("key" to "provided"))
        val withoutKey = impl.generatePESign(mapOf("a" to "provided"))
        assertNotEquals(withKey, withoutKey)
        assertEquals(40, withKey.length)
    }

    @Test
    fun signAlwaysIncludesCodeNames() {

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



    @Test
    fun encryptPeAttIdMatchesCapturedGolden() {

        assertEquals(
            "vORIFhhGtU+sYdSsSu5G2nXeHzhazbNDOMQ2y6Jwv7ZmzsqVzayosDrZpSZqKYKI",
            impl.encryptPeAttId("af9fcad571a84f56a718cfef46c837ea"),
        )
    }
}

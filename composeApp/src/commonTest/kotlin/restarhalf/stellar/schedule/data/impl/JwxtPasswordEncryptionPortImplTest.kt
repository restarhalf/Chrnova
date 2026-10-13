package restarhalf.stellar.schedule.data.impl

import restarhalf.stellar.schedule.config.LocalSecrets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class JwxtPasswordEncryptionPortImplTest {

    private val impl = JwxtPasswordEncryptionPortImpl()

    @Test
    fun outputIsDoubleBase64() {
        val encrypted = impl.encryptPasswordForLogin("password123")

        val inner = Base64.decode(encrypted).decodeToString()
        val cipherBytes = Base64.decode(inner)

        assertTrue(cipherBytes.size % 16 == 0)
        assertTrue(cipherBytes.size >= 16)
    }

    @Test
    fun deterministicForSameInput() {
        val a = impl.encryptPasswordForLogin("same-password")
        val b = impl.encryptPasswordForLogin("same-password")
        assertEquals(a, b)
    }

    @Test
    fun differentInputDifferentCipher() {
        assertNotEquals(
            impl.encryptPasswordForLogin("password-a"),
            impl.encryptPasswordForLogin("password-b"),
        )
    }

    @Test
    fun jsonQuotingChangesCiphertext() {

        val withQuote = impl.encryptPasswordForLogin("a\"b")
        val plain = impl.encryptPasswordForLogin("ab")
        assertNotEquals(withQuote, plain)
    }

    @Test
    fun encryptsUnicodePassword() {
        val encrypted = impl.encryptPasswordForLogin("密码123")
        val inner = Base64.decode(encrypted).decodeToString()
        assertTrue(Base64.decode(inner).isNotEmpty())
    }
}

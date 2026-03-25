package restarhalf.stellar.schedule.data.impl

import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.config.LocalSecrets
import restarhalf.stellar.schedule.domain.port.PasswordEncryptionPort
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class PasswordEncryptionPortImpl : PasswordEncryptionPort {

    private val json = Json

    @OptIn(ExperimentalEncodingApi::class)
    override fun encryptPasswordForLogin(password: String): String {
        val jsonPassword = json.encodeToString(password)
        val keySpec = SecretKeySpec(LocalSecrets.AES_KEY.toByteArray(Charsets.UTF_8), "AES")
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)

        val cipherBytes = cipher.doFinal(jsonPassword.toByteArray(Charsets.UTF_8))
        val cryptoJsBase64 = Base64.encode(cipherBytes)
        return Base64.encode(cryptoJsBase64.toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val AES_TRANSFORMATION = "AES/ECB/PKCS5Padding"
    }
}

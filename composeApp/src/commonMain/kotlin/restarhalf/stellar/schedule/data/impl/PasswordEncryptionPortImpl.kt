import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.*
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.config.LocalSecrets
import restarhalf.stellar.schedule.domain.port.PasswordEncryptionPort
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(DelicateCryptographyApi::class, ExperimentalEncodingApi::class)
class PasswordEncryptionPortImpl : PasswordEncryptionPort {

    private val json = Json

    private val cipher by lazy {
        val keyBytes = LocalSecrets.AES_KEY.encodeToByteArray()
        require(keyBytes.size == 16) { "AES key must be 16 bytes" }
        CryptographyProvider.Default
            .get(AES.ECB)
            .keyDecoder()
            .decodeFromByteArrayBlocking(AES.Key.Format.RAW, keyBytes)
            .cipher(padding = true) // PKCS7 padding
    }

    override fun encryptPasswordForLogin(password: String): String {
        val jsonPassword = json.encodeToString(password)
        val cipherBytes = cipher.encryptBlocking(jsonPassword.encodeToByteArray())
        val cryptoJsBase64 = Base64.encode(cipherBytes)
        return Base64.encode(cryptoJsBase64.encodeToByteArray())
    }
}
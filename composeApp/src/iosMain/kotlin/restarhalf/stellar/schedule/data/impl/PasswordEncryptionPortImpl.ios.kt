@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package restarhalf.stellar.schedule.data.impl

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.serialization.json.Json
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCBlockSizeAES128
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCOptionECBMode
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.posix.size_tVar
import restarhalf.stellar.schedule.config.LocalSecrets
import restarhalf.stellar.schedule.domain.port.PasswordEncryptionPort
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class PasswordEncryptionPortImpl : PasswordEncryptionPort {

    private val json = Json

    @OptIn(ExperimentalEncodingApi::class)
    override fun encryptPasswordForLogin(password: String): String {
        val jsonPassword = json.encodeToString(password)
        val cipherBytes = encryptAesEcbPkcs7(jsonPassword.encodeToByteArray())
        val cryptoJsBase64 = Base64.encode(cipherBytes)
        return Base64.encode(cryptoJsBase64.encodeToByteArray())
    }

    private fun encryptAesEcbPkcs7(plainBytes: ByteArray): ByteArray {
        val keyBytes = LocalSecrets.AES_KEY.encodeToByteArray()
        require(keyBytes.size == AES_KEY_SIZE)

        val outBuffer = ByteArray(plainBytes.size + kCCBlockSizeAES128.toInt())
        val outSize =
            memScoped {
                val outLength = alloc<size_tVar>()
                val status =
                    keyBytes.usePinned { keyPinned ->
                        plainBytes.usePinned { plainPinned ->
                            outBuffer.usePinned { outPinned ->
                                CCCrypt(
                                    kCCEncrypt,
                                    kCCAlgorithmAES,
                                    (kCCOptionPKCS7Padding or kCCOptionECBMode).convert(),
                                    keyPinned.addressOf(0),
                                    keyBytes.size.convert(),
                                    null,
                                    plainPinned.addressOf(0),
                                    plainBytes.size.convert(),
                                    outPinned.addressOf(0),
                                    outBuffer.size.convert(),
                                    outLength.ptr,
                                )
                            }
                        }
                    }

                if (status != kCCSuccess) {
                    error("iOS password encryption failed: CCCrypt status=$status")
                }

                outLength.value.toInt()
            }

        return outBuffer.copyOf(outSize)
    }

    private companion object {
        const val AES_KEY_SIZE = 16
    }
}

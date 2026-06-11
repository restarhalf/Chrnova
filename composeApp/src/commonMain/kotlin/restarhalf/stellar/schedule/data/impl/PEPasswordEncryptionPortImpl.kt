package restarhalf.stellar.schedule.data.impl

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.MD5
import dev.whyoleg.cryptography.algorithms.SHA1
import restarhalf.stellar.schedule.config.LocalSecrets
import restarhalf.stellar.schedule.domain.port.PEPasswordEncryptionPort

@OptIn(DelicateCryptographyApi::class)
class PEPasswordEncryptionPortImpl : PEPasswordEncryptionPort {

    private val SIGN_KEY = LocalSecrets.SIGN_KEY
    private val hasher = CryptographyProvider.Default.get(SHA1).hasher()

    override fun encryptPasswordForPELogin(password: String): String {
        val passwordMD5 = CryptographyProvider.Default.get(MD5).hasher().hashBlocking(password.encodeToByteArray())
        return passwordMD5.toHexString().lowercase()
    }

    override fun generatePESign(data: Map<String, Any?>): String {
        val signString = buildSignString(data)
        val digest = hasher.hashBlocking(signString.encodeToByteArray())
        return digest.toHexString().uppercase()
    }

    private fun buildSignString(data: Map<String, Any?>): String {
        val keys = mutableListOf<String>()
        for ((key, value) in data) {
            when {
                key == "code_names"                          -> keys.add(key)
                value == 0 || value == 0L || value == 0.0   -> keys.add(key)
                value != null && value != "" && key != "sign" -> keys.add(key)
            }
        }
        keys.sort()

        val sb = StringBuilder()
        for (key in keys) {
            sb.append("&").append(key).append("=").append(data[key])
        }

        return when {
            sb.isEmpty()          -> "key=$SIGN_KEY"
            "&key" in sb          -> sb.substring(1)
            else                  -> sb.substring(1) + "&key=$SIGN_KEY"
        }
    }
}
package restarhalf.stellar.schedule.data.impl

import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.*
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.config.LocalSecrets
import restarhalf.stellar.schedule.domain.port.PasswordEncryptionPort
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 密码加密端口实现类
 * 
 * 实现PasswordEncryptionPort接口，负责教务系统登录前的密码加密处理。
 * 使用AES-ECB加密算法，密钥从LocalSecrets.AES_KEY获取。
 */
@OptIn(DelicateCryptographyApi::class, ExperimentalEncodingApi::class)
class PasswordEncryptionPortImpl : PasswordEncryptionPort {

    private val json = Json

    /** AES加密器，懒加载初始化 */
    private val cipher by lazy {
        val keyBytes = LocalSecrets.AES_KEY.encodeToByteArray()
        require(keyBytes.size == 16) { "AES key must be 16 bytes" }
        CryptographyProvider.Default
            .get(AES.ECB)
            .keyDecoder()
            .decodeFromByteArrayBlocking(AES.Key.Format.RAW, keyBytes)
            .cipher(padding = true) // PKCS7 padding
    }

    /**
     * 加密密码用于登录
     * 
     * 加密流程：密码 -> JSON序列化 -> AES加密 -> Base64编码 -> 再次Base64编码
     * 
     * @param password 原始密码
     * @return 加密后的密码字符串
     */
    override fun encryptPasswordForLogin(password: String): String {
        val jsonPassword = json.encodeToString(password)
        val cipherBytes = cipher.encryptBlocking(jsonPassword.encodeToByteArray())
        val cryptoJsBase64 = Base64.encode(cipherBytes)
        return Base64.encode(cryptoJsBase64.encodeToByteArray())
    }
}
package restarhalf.stellar.schedule.data.impl

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.MD5
import dev.whyoleg.cryptography.algorithms.SHA1
import restarhalf.stellar.schedule.config.LocalSecrets
import restarhalf.stellar.schedule.domain.port.PEPasswordEncryptionPort

/**
 * 体育系统密码加密端口实现类
 * 
 * 实现PEPasswordEncryptionPort接口，负责体育系统相关的加密和签名。
 * - 密码加密使用MD5
 * - 请求签名使用SHA1
 */
@OptIn(DelicateCryptographyApi::class)
class PEPasswordEncryptionPortImpl : PEPasswordEncryptionPort {

    /** 签名密钥 */
    private val SIGN_KEY = LocalSecrets.SIGN_KEY

    /** SHA1哈希器 */
    private val hasher = CryptographyProvider.Default.get(SHA1).hasher()

    /**
     * 加密密码用于体育系统登录
     * 
     * @param password 原始密码
     * @return MD5哈希后的密码（小写十六进制）
     */
    override fun encryptPasswordForPELogin(password: String): String {
        val passwordMD5 = CryptographyProvider.Default.get(MD5).hasher().hashBlocking(password.encodeToByteArray())
        return passwordMD5.toHexString().lowercase()
    }

    /**
     * 生成体育系统请求签名
     * 
     * @param data 请求参数映射
     * @return SHA1签名（大写十六进制）
     */
    override fun generatePESign(data: Map<String, Any?>): String {
        val signString = buildSignString(data)
        val digest = hasher.hashBlocking(signString.encodeToByteArray())
        return digest.toHexString().uppercase()
    }

    /**
     * 构建签名字符串
     * 
     * 签名规则：
     * 1. 过滤掉空值和sign字段
     * 2. 按key排序
     * 3. 拼接成 key1=value1&key2=value2 格式
     * 4. 末尾追加签名密钥
     * 
     * @param data 请求参数
     * @return 签名字符串
     */
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
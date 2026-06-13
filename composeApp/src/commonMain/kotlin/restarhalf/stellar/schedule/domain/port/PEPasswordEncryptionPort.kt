package restarhalf.stellar.schedule.domain.port

/**
 * 体育系统密码加密端口接口
 * 
 * 定义体育成绩系统相关的加密和签名接口。
 */
interface PEPasswordEncryptionPort {
    /**
     * 加密密码用于体育系统登录
     * 
     * @param password 原始密码
     * @return 加密后的密码字符串
     */
    fun encryptPasswordForPELogin(password: String): String
    /**
     * 生成体育系统请求签名
     * 
     * @param data 请求参数映射
     * @return 签名字符串
     */
    fun generatePESign(data: Map<String, Any?>): String
}
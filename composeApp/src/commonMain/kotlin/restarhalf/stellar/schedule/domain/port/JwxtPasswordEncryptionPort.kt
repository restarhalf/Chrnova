package restarhalf.stellar.schedule.domain.port

/**
 * 密码加密端口接口
 * 
 * 定义密码加密的抽象接口，用于教务系统登录前的密码加密处理。
 */
interface JwxtPasswordEncryptionPort {

    /**
     * 加密密码用于登录
     * 
     * @param password 原始密码
     * @return 加密后的密码字符串
     */
    fun encryptPasswordForLogin(password: String): String
}

package restarhalf.stellar.schedule.domain.port

interface PEPasswordEncryptionPort {
    fun encryptPasswordForPELogin(password: String): String
}
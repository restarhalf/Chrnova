package restarhalf.stellar.schedule.domain.port

interface PEPasswordEncryptionPort {
    fun encryptPasswordForPELogin(password: String): String
    fun generatePESign(data: Map<String, Any?>): String
}
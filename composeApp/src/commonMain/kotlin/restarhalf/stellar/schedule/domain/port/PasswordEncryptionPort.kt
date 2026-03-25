package restarhalf.stellar.schedule.domain.port

interface PasswordEncryptionPort {

    fun encryptPasswordForLogin(password: String): String
}

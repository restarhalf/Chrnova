package restarhalf.stellar.schedule.domain.port


interface JwxtPasswordEncryptionPort {

    
    fun encryptPasswordForLogin(password: String): String
}

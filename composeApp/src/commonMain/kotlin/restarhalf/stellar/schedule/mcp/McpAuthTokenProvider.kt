package restarhalf.stellar.schedule.mcp

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import restarhalf.stellar.schedule.config.LocalSecrets
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import kotlin.random.Random

class McpAuthTokenProvider(
    private val authStore: JwxtAuthStore,
    private val settings: Settings,
) {
    fun token(): String {
        val userNo = authStore.getUserNo().orEmpty().ifBlank { "anonymous" }
        val salt = settings.getStringOrNull(KEY_DEVICE_SALT) ?: newSalt().also {
            settings[KEY_DEVICE_SALT] = it
        }
        return "mcp-$userNo-$salt-${hmacSha256("$userNo:$salt", LocalSecrets.MCP_SHARED_SECRET)}"
    }

    private fun newSalt(): String = List(32) { Random.nextInt(16).toString(16) }.joinToString("")

    private fun hmacSha256(message: String, secret: String): String {
        val blockSize = 64
        val normalizedKey = secret.encodeToByteArray().sha256IfLonger(blockSize).copyOf(blockSize)
        val outerKey = normalizedKey.map { (it.toInt() xor 0x5c).toByte() }.toByteArray()
        val innerKey = normalizedKey.map { (it.toInt() xor 0x36).toByte() }.toByteArray()
        return (outerKey + (innerKey + message.encodeToByteArray()).sha256()).sha256().toHex()
    }

    private fun ByteArray.sha256IfLonger(blockSize: Int): ByteArray = if (size > blockSize) sha256() else this

    private fun ByteArray.sha256(): ByteArray {
        val data = this
        val bitLength = data.size.toLong() * 8L
        val paddedLength = (((data.size + 8) / 64) + 1) * 64
        val padded = ByteArray(paddedLength)
        data.copyInto(padded)
        padded[data.size] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[paddedLength - 1 - i] = (bitLength ushr (8 * i)).toByte()
        }
        var h0 = 0x6a09e667
        var h1 = -0x4498517b
        var h2 = 0x3c6ef372
        var h3 = -0x5ab00ac6
        var h4 = 0x510e527f
        var h5 = -0x64fa9774
        var h6 = 0x1f83d9ab
        var h7 = 0x5be0cd19
        val k = intArrayOf(
            0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b, 0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
            -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
            -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039, -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
            -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d, -0x2e6d17e7, -0x2966f9dc, -0xbf1ca7b, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8, -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e,
        )
        for (chunk in padded.indices step 64) {
            val w = IntArray(64)
            for (i in 0 until 16) {
                val j = chunk + i * 4
                w[i] = ((padded[j].toInt() and 0xff) shl 24) or ((padded[j + 1].toInt() and 0xff) shl 16) or ((padded[j + 2].toInt() and 0xff) shl 8) or (padded[j + 3].toInt() and 0xff)
            }
            for (i in 16 until 64) {
                val s0 = w[i - 15].rotateRight( 7) xor w[i - 15].rotateRight(18) xor (w[i - 15] ushr 3)
                val s1 = w[i - 2].rotateRight(17) xor w[i - 2].rotateRight( 19) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }
            var a = h0; var b = h1; var c = h2; var d = h3; var e = h4; var f = h5; var g = h6; var h = h7
            for (i in 0 until 64) {
                val s1 = e.rotateRight(6) xor e.rotateRight( 11) xor e.rotateRight(25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + s1 + ch + k[i] + w[i]
                val s0 = a.rotateRight( 2) xor a.rotateRight(13) xor a.rotateRight( 22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj
                h = g; g = f; f = e; e = d + temp1; d = c; c = b; b = a; a = temp1 + temp2
            }
            h0 += a; h1 += b; h2 += c; h3 += d; h4 += e; h5 += f; h6 += g; h7 += h
        }
        return intArrayOf(h0, h1, h2, h3, h4, h5, h6, h7).flatMap { word ->
            listOf((word ushr 24).toByte(), (word ushr 16).toByte(), (word ushr 8).toByte(), word.toByte())
        }.toByteArray()
    }

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    private companion object {
        const val KEY_DEVICE_SALT = "mcp_device_salt"
    }
}

package restarhalf.stellar.schedule.data.local.mmkv

import com.tencent.mmkv.kmp.MMKV
import com.tencent.mmkv.kmp.initialize
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import restarhalf.stellar.schedule.core.log.AppLogger

object MmkvIosBootstrap {
    private const val TAG = "MmkvMigration"

    fun initializeAndMigrate() {
        MMKV.initialize()
        MmkvStores.ALL.forEach { migrateNSUserDefaults(it) }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun migrateNSUserDefaults(name: String) {
        val kv = MMKV.mmkvWithID(name)
        if (kv.containsKey(MmkvStores.MIGRATION_FLAG)) return

        // Kotlin/Native 里 NSUserDefaults(suiteName:) 返回非空
        val defaults = NSUserDefaults(suiteName = name)
        val dict = defaults.dictionaryRepresentation()
        var copied = 0
        for ((rawKey, value) in dict) {
            val key = rawKey as? String ?: continue
            when (value) {
                is NSString -> {
                    kv.encodeString(key, value.toString())
                    copied++
                }
                is NSNumber -> {
                    // objCType 是 CPointer<ByteVar>，不是 String
                    val type = value.objCType?.toKString() ?: ""
                    when {
                        type == "B" || (type.contains("c") && type.length <= 2) ->
                            kv.encodeBool(key, value.boolValue)
                        type.contains("q") || type.contains("l") || type.contains("Q") || type.contains("L") ->
                            kv.encodeLong(key, value.longLongValue)
                        type.contains("i") || type.contains("s") || type.contains("I") || type.contains("S") ->
                            kv.encodeInt(key, value.integerValue.toInt())
                        type.contains("f") ->
                            kv.encodeFloat(key, value.floatValue)
                        else ->
                            kv.encodeDouble(key, value.doubleValue)
                    }
                    copied++
                }
                is String -> {
                    kv.encodeString(key, value)
                    copied++
                }
                else -> Unit
            }
        }
        kv.encodeBool(MmkvStores.MIGRATION_FLAG, true)
        AppLogger.log(TAG, "migrated store=$name keys=$copied from NSUserDefaults")
    }
}

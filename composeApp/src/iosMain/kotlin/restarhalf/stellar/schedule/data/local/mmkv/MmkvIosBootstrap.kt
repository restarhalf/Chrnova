package restarhalf.stellar.schedule.data.local.mmkv

import com.tencent.mmkv.kmp.MMKV
import com.tencent.mmkv.kmp.initialize
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.boolValue
import platform.Foundation.doubleValue
import platform.Foundation.floatValue
import platform.Foundation.integerValue
import platform.Foundation.longLongValue
import platform.Foundation.stringValue
import restarhalf.stellar.schedule.core.log.AppLogger

object MmkvIosBootstrap {
    private const val TAG = "MmkvMigration"

    fun initializeAndMigrate() {
        MMKV.initialize()
        MmkvStores.ALL.forEach { migrateNSUserDefaults(it) }
    }

    private fun migrateNSUserDefaults(name: String) {
        val kv = MMKV.mmkvWithID(name)
        if (kv.containsKey(MmkvStores.MIGRATION_FLAG)) return

        val defaults = NSUserDefaults(suiteName = name) ?: NSUserDefaults.standardUserDefaults
        val dict = defaults.dictionaryRepresentation()
        var copied = 0
        for ((rawKey, value) in dict) {
            val key = rawKey as? String ?: continue
            when (value) {
                is NSString -> kv.encodeString(key, value.toString())
                is NSNumber -> {
                    // NSNumber 承载 Bool/Int/Long/Float/Double，按 objCType 粗分即可覆盖本项目设置项
                    val type = value.objCType ?: ""
                    when {
                        type.contains("c", ignoreCase = true) && type.length <= 2 ->
                            kv.encodeBool(key, value.boolValue)
                        type.contains("q") || type.contains("l") ->
                            kv.encodeLong(key, value.longLongValue)
                        type.contains("i") || type.contains("s") ->
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
